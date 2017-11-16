package com.socialcops.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.socialcops.directory.DirectoryUtils;
import com.socialcops.directory.Synchronizer;
import com.socialcops.models.FileTree;
import com.socialcops.properties.SyncProperty;
import com.socialcops.sockets.SocketListener;
import com.socialcops.sockets.SocketManager;
import com.socialcops.sockets.SocketScheduler;

public class Main {
	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	private static ObjectMapper objectMapper;

	private final static String PROPERTIES = "./config.properties";
	private final static String DEFAULT_PROPERTIES = "config-default.properties";

	public static void main(String[] str) {

		new Main().initialize();
	}

	/**
	 * Initialize the configuration, establish connection and hands control to
	 * Scheduler
	 */
	public void initialize() {
		logger.info(System.getProperty("java.class.path"));
		Main.objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JodaModule());

		SyncProperty syncProperty = config();
		if (syncProperty == null) {
			return;
		}

		// Verifying if sync folder exists and write permission is assigned
		File syncFolder = new File(syncProperty.getSyncFolder());
		try {
			if (!syncFolder.isDirectory() || !syncFolder.canWrite()) {
				logger.error(MessageFormat.format("Invalid sync folder: {0}", syncProperty.getSyncFolder()));
				return;
			}
		} catch (Exception e) {
			logger.error(MessageFormat.format("Invalid sync folder : {0}", syncProperty.getSyncFolder()), e);
			return;
		}

		logger.info("Config Properties: {}", syncProperty);

		DirectoryUtils.setSyncFolder(syncProperty.getSyncFolder());

		// Initialize Socket manager
		SocketManager socketManager = new SocketManager(syncProperty.getServerPort(), syncProperty.getServerIp(),
				syncProperty.isMaster());
		boolean success;
		// Initiate connections
		if (syncProperty.isMaster()) {
			success = socketManager.initiateMaster();
		} else {
			success = socketManager.initiateClient();
		}

		// Exits if connection cannot be established
		if (!success) {
			logger.error("Cannot create connection. Exitting.");
			return;
		}

		DateTime curDataTime = DateTime.now();
		// Creates file tree of the sync fodler
		FileTree fileTree = DirectoryUtils.createFileTree(syncFolder);
		logger.info("File tree created.");
		Synchronizer synchronizer = new Synchronizer(syncFolder, fileTree, syncProperty.isMaster(), curDataTime,
				socketManager, objectMapper);

		SocketListener socketListener = new SocketListener(socketManager, synchronizer);

		SocketScheduler socketScheduler = new SocketScheduler(syncProperty.getScheduleDelayInMillis(),
				syncProperty.getDataDeliveryDelay(), socketManager, synchronizer, syncProperty.isMaster(),
				socketListener);
		// start the scheduler
		socketScheduler.run();

	}

	/**
	 * Parse config.properties file and return SyncProperty with all the
	 * configuration
	 * 
	 * @return SyncPro
	 */
	public SyncProperty config() {

		Properties property;
		SyncProperty syncProperty;
		try {
			File file = new File(PROPERTIES);
			InputStream in;
			if (!file.exists()) {
				in = ClassLoader.getSystemResource(DEFAULT_PROPERTIES).openStream();
				logger.info("Cannot find user defined properties: {}. Using default: {}", PROPERTIES,
						DEFAULT_PROPERTIES);
			} else {
				in = new FileInputStream(file);
			}
			property = new Properties();
			property.load(in);
		} catch (IOException e) {
			logger.error(MessageFormat.format("Cannot find {0}. Exiting.", DEFAULT_PROPERTIES), e);
			return null;
		}

		try {
			syncProperty = new SyncProperty(property);
		} catch (NumberFormatException e) {
			logger.error(MessageFormat.format("Invalid port: {0}", property.getProperty(SyncProperty.getSERVER_PORT())),
					e);
			return null;
		}
		return syncProperty;

	}

	public static ObjectMapper getObjectMapper() {
		return objectMapper;
	}
}
