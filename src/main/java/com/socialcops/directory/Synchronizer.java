package com.socialcops.directory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialcops.enums.DataIdentifier;
import com.socialcops.enums.FileOperation;
import com.socialcops.models.FileDifference;
import com.socialcops.models.FileDifferenceData;
import com.socialcops.models.FileTree;
import com.socialcops.models.FileTreeWrapper;
import com.socialcops.models.SocketStreamData;
import com.socialcops.sockets.SocketManager;

/**
 * @author PratickChokhani Process all the data received from client or master.
 *         Sends processed data to client or master
 *
 */
public class Synchronizer {
	private static final Logger logger = LoggerFactory.getLogger(Synchronizer.class);

	private final File syncFolder;
	private final Path syncFolderPath;
	private DateTime curDataTime;
	private FileTree systemFileTree;
	private boolean initialSyncComplete = false;
	private boolean master;
	private SocketManager socketManager;
	private ObjectMapper objectMapper;
	private DateTime lastUpdateTime = new DateTime(0L);
	private final Queue<FileDifference> fileDifferenceToProcess = new ArrayDeque<>(100);
	private final AtomicBoolean requestBeingProcessed = new AtomicBoolean(false);
	private FileDifference curProcessingFileDifference = null;
	private final AtomicBoolean initialSyncBeingWorked = new AtomicBoolean(false);

	public Synchronizer(File syncFolder, FileTree systemFileTree, boolean master, DateTime curDataTime,
			SocketManager socketManager, ObjectMapper objectMapper) {
		this.syncFolder = syncFolder;
		this.systemFileTree = systemFileTree;
		this.master = master;
		this.curDataTime = curDataTime;
		this.socketManager = socketManager;
		this.objectMapper = objectMapper;
		this.syncFolderPath = Paths.get(syncFolder.getAbsolutePath());
	}

	/**
	 * Processes the input received from socket according to DataIdentifier
	 * 
	 * @throws IOException
	 */
	public synchronized void processInput() throws IOException {
		logger.info("Getting data identifier.");
		SocketStreamData socketStreamData = socketManager.getDataIdentifier();
		logger.info("Identifier received: {}", socketStreamData);

		DataIdentifier dataIdentifier = decoder(socketStreamData.getDataIdentifier());
		logger.info("Decoded Identifier received: {}", dataIdentifier);
		if (dataIdentifier == DataIdentifier.INVALID) {
			logger.error("Invalid data received. {}", socketStreamData);
			return;
		}
		long size = socketStreamData.getDataSize();
		byte[] bytes = null;
		if (dataIdentifier != DataIdentifier.FIR) {
			bytes = socketManager.getByteFromSocket(size);
			logger.info("data bytes received. byte size: {}", bytes.length);
		}
		switch (dataIdentifier) {
		case RTC: // Requests File tree
			sendFileTree();
			break;
		case FIT: // File tree is received
			processRemoteFileTree(bytes);
			break;
		case RTD: // Requests the list of files that have been changed since
					// last update
			sendFileTreeDifference(bytes);
			break;
		case FID: // List of files that have been changed since last update
			processFileDifference(bytes);
			break;
		case RFI: // Request a copy of file
			sendRequestedFile(bytes);
			break;
		case FIR: // File is being received in the socket
			processedReceiveFile(size);
			break;
		case FNF: // Requested file is not found
			requestBeingProcessed.getAndSet(false);
			processFileDifference();
			break;
		default:
			break;
		}
	}

	/**
	 * Request the file tree of the sync folder from the client
	 * 
	 * @return true if success
	 * @throws IOException
	 */
	public synchronized boolean requestFileTree() throws IOException {

		if (!initialSyncBeingWorked.compareAndSet(false, true)) {
			return false;
		}

		try {
			logger.info("Requesting file tree.");
			socketManager.sendByteToSocket(DataIdentifier.RTC, new byte[2]);
			return true;
		} catch (Exception e) {
			logger.error("Unexpected error. Exitting.", e);

			initialSyncBeingWorked.getAndSet(false);
			throw new IOException();
		}
	}

	/**
	 * Sends the complete file tree of the sync folder to the master
	 * 
	 * @throws IOException
	 */
	private synchronized void sendFileTree() throws IOException {
		byte[] bytes;
		try {
			logger.info("Serializing file tree.");
			FileTreeWrapper fileTreeWrapper = new FileTreeWrapper(systemFileTree, curDataTime);
			logger.info("FileTree Wrapper: {}, ObjectMapper: {}", fileTreeWrapper, objectMapper);
			bytes = objectMapper.writeValueAsBytes(fileTreeWrapper);
		} catch (JsonProcessingException e) {
			logger.error("Unexpected error. Exitting.", e);
			throw new IOException();
		}

		try {
			logger.info("Sending file tree.");
			socketManager.sendByteToSocket(DataIdentifier.FIT, bytes);
		} catch (IOException e) {
			logger.error("Socket error. Cannot send data. Exitting.", e);
			throw new IOException();
		}
	}

	/**
	 * Process the file tree received from client. Compare them with the current
	 * file tree and send the differences that need to be updated in the client
	 * side. Also, update the differences that needs to be done in master side.
	 * It queues the differences to be processed one by one
	 * 
	 * @param data
	 *            file tree in bytes
	 * @throws IOException
	 */
	private synchronized void processRemoteFileTree(byte[] data) throws IOException {
		FileTreeWrapper remoteFileTreeWrapper;
		try {
			logger.info("Deserializing file tree wrapper.");
			remoteFileTreeWrapper = objectMapper.readValue(data, FileTreeWrapper.class);
		} catch (Exception e) {
			initialSyncBeingWorked.set(false);
			logger.error(MessageFormat.format("Cannot parse data time.", data), e);
			throw new IOException();
		}

		try {
			curDataTime = new DateTime();
			systemFileTree = DirectoryUtils.createFileTree(syncFolder);

			logger.info("Calculating file tree difference for the current system.");
			List<FileDifference> fileDifferences = DirectoryUtils
					.calculateDifference(remoteFileTreeWrapper.getFileTree(), systemFileTree, new DateTime(0L));
			List<FileDifference> systemFileDifference = new ArrayList<>(fileDifferences.size());
			for (FileDifference fileDifference : fileDifferences) {
				if (fileDifference.getFileOperation() != FileOperation.DELETE) {
					systemFileDifference.add(fileDifference);
				}
			}

			logger.info("Adding file tree difference for processing.");
			fileDifferenceToProcess.addAll(systemFileDifference);

			logger.info("Calculating file tree difference for remote system.");
			fileDifferences = DirectoryUtils.calculateDifference(systemFileTree, remoteFileTreeWrapper.getFileTree(),
					new DateTime(0L));
			List<FileDifference> remoteFileDifference = new ArrayList<>(fileDifferences.size());
			for (FileDifference fileDifference : fileDifferences) {
				if (fileDifference.getFileOperation() != FileOperation.DELETE) {
					remoteFileDifference.add(fileDifference);
				}
			}
			FileDifferenceData remoteFileDifferenceData = new FileDifferenceData(remoteFileDifference, lastUpdateTime,
					curDataTime);
			logger.info("Sending file tree difference");
			sendFileDifferenceData(remoteFileDifferenceData);

			processFileDifference();

			initialSyncComplete = true;
		} catch (Exception e) {
			logger.error("Unexpected error. Exitting.", e);
			initialSyncBeingWorked.set(false);
		}
	}

	/**
	 * Requests file difference from remote system to update the current folder
	 * and maintain the data in sync
	 * 
	 * @return true is success
	 * @throws IOException
	 */
	public synchronized boolean requestFileDifference() throws IOException {

		if (!requestBeingProcessed.compareAndSet(false, true)) {
			return false;
		}
		try {
			logger.info("Requesting file difference.");
			byte[] bytes = objectMapper.writeValueAsBytes(lastUpdateTime);
			socketManager.sendByteToSocket(DataIdentifier.RTD, bytes);
			return true;
		} catch (Exception e) {
			logger.error("Unexpected error. Exitting.", e);
			requestBeingProcessed.set(false);
			throw new IOException();
		}
	}

	/**
	 * Sends file difference to remote system for maintaining sync
	 * 
	 * @param data
	 *            time when last sync was done
	 * @throws IOException
	 */
	private synchronized void sendFileTreeDifference(byte[] data) throws IOException {

		DateTime remoteLastUpdateTime;
		try {

			remoteLastUpdateTime = objectMapper.readValue(data, DateTime.class);

		} catch (Exception e) {
			logger.error(MessageFormat.format("Cannot parse data time. Exitting", data), e);
			throw new IOException();

		}
		sendFileTreeDifference(remoteLastUpdateTime);
	}

	/**
	 * Processes received file difference and update the files that have changed
	 * in remote system
	 * 
	 * @param data
	 *            file difference in bytes
	 * @throws IOException
	 */
	private synchronized void processFileDifference(byte[] data) throws IOException {
		FileDifferenceData fileDifferenceData;
		try {
			fileDifferenceData = objectMapper.readValue(data, FileDifferenceData.class);
		} catch (Exception e) {
			logger.error(MessageFormat.format("Cannot parse file difference data. Exitting", data), e);
			throw new IOException();
		}

		try {
			DateTime remoteCurrentDataTime = fileDifferenceData.getCurrentDataTime();
			List<FileDifference> remoteFileDifferences = fileDifferenceData.getFileDifferences();
			for (FileDifference fileDifference : remoteFileDifferences) {
				Path path = syncFolderPath.resolve(fileDifference.getPath());
				try {
					if (remoteCurrentDataTime.isAfter(DirectoryUtils.getLastModifiedTime(path))) {

						if (fileDifference.getFileOperation() == FileOperation.DELETE) {
							Files.deleteIfExists(path);
						} else {
							fileDifferenceToProcess.add(fileDifference);
						}
					}
				} catch (Exception e) {
					logger.error(MessageFormat.format("Unexpected exception for file difference", fileDifference), e);
				}
			}

			requestBeingProcessed.set(false);
			processFileDifference();
			initialSyncComplete = true;
		} catch (Exception e) {
			requestBeingProcessed.set(false);
			logger.error("Unexpected Error. Exitting.", e);
			throw new IOException();
		}
	}

	/**
	 * Sends requested file
	 * 
	 * @param data
	 *            path of the file in bytes
	 * @throws IOException
	 */
	private synchronized void sendRequestedFile(byte[] data) throws IOException {

		try {
			String pathStr = new String(data);
			Path path = syncFolderPath.resolve(pathStr);
			if (!Files.exists(path)) {
				logger.info("File not found.");
				socketManager.sendByteToSocket(DataIdentifier.FNF, new byte[1]);
				return;
			}
			File file = new File(path.toString());
			logger.info("Sending requested file");
			socketManager.sendFile(file.length(), new FileInputStream(file));
		} catch (Exception e) {
			logger.error("Unexpected error. Exitting.", e);
			throw new IOException();
		}
	}

	/**
	 * Save file that was requested
	 * 
	 * @param size
	 *            size of the file in bytes
	 * @throws IOException
	 */
	private synchronized void processedReceiveFile(long size) throws IOException {
		try {
			String pathStr = new String(curProcessingFileDifference.getPath());
			Path path = syncFolderPath.resolve(pathStr);
			if (Files.exists(path)) {
				Files.delete(path);
			}

			if (!Files.exists(path.getParent())) {
				Files.createDirectories(path.getParent());
			}
			socketManager.readFile(Files.newOutputStream(path), size);
			Files.setLastModifiedTime(path,
					FileTime.fromMillis(curProcessingFileDifference.getLastModified().getMillis()));
			requestBeingProcessed.getAndSet(false);
			processFileDifference();
		} catch (Exception e) {
			requestBeingProcessed.getAndSet(false);
			logger.error("Unexpected error. Exitting.", e);
			throw new IOException();
		}
	}

	/**
	 * Fetch a file difference from queue and if the same local is out dated,
	 * than that file is requested from remote system.
	 * 
	 * @return true if success
	 * @throws IOException
	 */
	public synchronized boolean processFileDifference() throws IOException {

		if (!requestBeingProcessed.compareAndSet(false, true)) {
			logger.info("Request being processed.");
			return false;
		}
		logger.info("Processing file difference.");
		if (fileDifferenceToProcess.isEmpty()) {
			logger.info("No file to be processed.");
			requestBeingProcessed.set(false);
			return false;
		}

		FileDifference fileDifference;
		Path path;
		do {
			fileDifference = fileDifferenceToProcess.poll();
			path = syncFolderPath.resolve(fileDifference.getPath());
		} while (fileDifferenceToProcess.size() > 0
				&& fileDifference.getLastModified().isBefore(DirectoryUtils.getLastModifiedTime(path)));

		logger.info("File last modified: {}, Path: {}", DirectoryUtils.getLastModifiedTime(path),
				fileDifference.getPath());
		if (fileDifference.getLastModified().isAfter(DirectoryUtils.getLastModifiedTime(path))
				&& !Files.isDirectory(path)) {
			logger.info("Processing file difference: {}", fileDifference);
			curProcessingFileDifference = fileDifference;
			socketManager.sendByteToSocket(DataIdentifier.RFI, fileDifference.getPath().getBytes());
		} else {
			requestBeingProcessed.set(false);
			processFileDifference();
		}

		return true;
	}

	/**
	 * Sends file difference data to remote system containing list of files and
	 * time when the list was updated
	 * 
	 * @param fileDifferenceData
	 * @throws IOException
	 */
	private synchronized void sendFileDifferenceData(FileDifferenceData fileDifferenceData) throws IOException {
		byte[] bytes;
		try {
			bytes = objectMapper.writeValueAsBytes(fileDifferenceData);
		} catch (JsonProcessingException e) {
			initialSyncBeingWorked.getAndSet(false);
			logger.error("Unexpected error. Exitting.", e);
			throw new IOException();
		}

		try {
			logger.info("Sending file difference post file tree processing.");
			socketManager.sendByteToSocket(DataIdentifier.FID, bytes);
			initialSyncBeingWorked.getAndSet(false);
		} catch (IOException e) {
			initialSyncBeingWorked.getAndSet(false);
			logger.error("Socket error. Cannot send data. Exitting.", e);
			throw new IOException();
		}
	}

	/**
	 * Send file tree to remote system
	 * 
	 * @param remoteLastUpdateTime
	 * @throws IOException
	 */
	private synchronized void sendFileTreeDifference(DateTime remoteLastUpdateTime) throws IOException {
		DateTime currentTime = DateTime.now();
		FileTree currentTree = DirectoryUtils.createFileTree(syncFolder);
		List<FileDifference> fileDifferences = DirectoryUtils.calculateDifference(currentTree, systemFileTree,
				remoteLastUpdateTime);
		FileDifferenceData fileDifferenceData = new FileDifferenceData(fileDifferences, lastUpdateTime, currentTime);

		try {
			logger.info("Sending requested file difference.");
			socketManager.sendByteToSocket(DataIdentifier.FID, objectMapper.writeValueAsBytes(fileDifferenceData));
		} catch (IOException e) {
			logger.error("Socket error. Cannot send data. Exitting.", e);
			throw new IOException();
		}
	}

	/**
	 * Decodes bytes to DataIdentifier and identifies what the next data will be
	 * 
	 * @param bytes
	 *            data identifier bytes
	 * @return
	 */
	public DataIdentifier decoder(byte[] bytes) {

		for (DataIdentifier dataIdentifier : DataIdentifier.values()) {
			if (Arrays.equals(bytes, dataIdentifier.getNameInByte())) {
				return dataIdentifier;
			}
		}
		return DataIdentifier.INVALID;
	}

	public boolean isInitialSyncComplete() {
		return initialSyncComplete;
	}

	public void setInitialSyncComplete(boolean initialSyncComplete) {
		this.initialSyncComplete = initialSyncComplete;
	}

}
