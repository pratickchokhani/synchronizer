package com.socialcops.sockets;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socialcops.directory.Synchronizer;
import com.socialcops.enums.DataIdentifier;

/**
 * @author PratickChokhani Listens to socket and passes the data for processing
 *
 */
public class SocketListener {
	private static final Logger logger = LoggerFactory.getLogger(SocketListener.class);
	private static int IDENTIFIER_BYTES = DataIdentifier.FID.getNameInByte().length;
	private final SocketManager socketManager;
	private final Synchronizer synchronizer;

	public SocketListener(SocketManager socketManager, Synchronizer synchronizer) {
		this.socketManager = socketManager;
		this.synchronizer = synchronizer;
	}

	/**
	 * Listens to socket and if data is available, the passes that data for
	 * processing
	 * 
	 * @throws IOException
	 */
	public void listenToSocket() throws IOException {
		int availableByteSize;
		try {
			logger.info("Listening to socket.");
			availableByteSize = socketManager.checkDataAvailability();
			logger.info("Available byte size: {}", availableByteSize);
			if (availableByteSize >= IDENTIFIER_BYTES) {
				synchronizer.processInput();
			}
		} catch (IOException e) {
			logger.error("Invalid connection error. Exitting", e);
			throw new IOException();
		}
	}
}
