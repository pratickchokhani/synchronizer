package com.socialcops.sockets;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.socialcops.directory.Synchronizer;

/**
 * @author PratickChokhani Scheduler to check periodically for data in input
 *         stream and also to request file difference to sync data
 *
 */
public class SocketScheduler implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(SocketScheduler.class);

	private long scheduleDelayInMillis = 2000;
	private long dataDeliveryDelay = 120000;
	private final SocketManager socketManager;
	private final Synchronizer synchronizer;
	private final ScheduledExecutorService executorService;
	private final boolean master;
	private final SocketListener socketListener;
	private long lastListenerRun = 0L;
	private long lastProcessorRun = 0L;

	public SocketScheduler(long scheduleDelayInMillis, long dataDeliveryDelay, SocketManager socketManager,
			Synchronizer synchronizer, boolean master, SocketListener socketListener) {
		super();
		this.scheduleDelayInMillis = scheduleDelayInMillis;
		this.dataDeliveryDelay = dataDeliveryDelay;
		this.socketManager = socketManager;
		this.synchronizer = synchronizer;
		this.executorService = Executors.newSingleThreadScheduledExecutor();
		this.master = master;
		this.socketListener = socketListener;
	}

	public void scheduleNextSync() {
		executorService.schedule(this, scheduleDelayInMillis, TimeUnit.MILLISECONDS);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run() Runs periodically to check data for data in
	 * input stream and request for file difference from second party to sync
	 * folder
	 */
	@Override
	public void run() {

		try {
			logger.info("Running scheduler.");
			boolean connectionStatus = socketManager.testConnection();
			if (!connectionStatus) {
				logger.error("Cannot established the connection.");
				throw new IOException("Invalid connection");
			}
			socketListener.listenToSocket();
			if (!synchronizer.isInitialSyncComplete()) {
				if (master) {
					logger.info("Initializing sync.");
					synchronizer.requestFileTree();
				}
			} else {
				long curMillis = DateTime.now().getMillis();
				logger.info("Processing file difference.");
				if (!synchronizer.processFileDifference()) {
					if (curMillis - lastProcessorRun >= dataDeliveryDelay) {
						logger.info("Requesting file difference.");
						if (synchronizer.requestFileDifference()) {
							lastProcessorRun = curMillis;
						}
					}
				}
			}
			scheduleNextSync();
		} catch (Exception e) {
			logger.error("Unexpected exception. Exitting", e);
			try {
				socketManager.close();
			} catch (IOException e1) {
			}
			System.exit(1);
		}
	}
}
