package com.socialcops.properties;

import java.util.Properties;

/**
 * @author PratickChokhani Property received from config.properties is stored
 *         here.
 */
public class SyncProperty {

	private static String MASTER = "master";
	private static String SERVER_IP = "server.ip";
	private static String SERVER_PORT = "server.port";
	private static String SYNC_FOLDER = "sync.folder";
	private static String FILE_TREE_NAME = "file_tree.data";
	private static String SCHEDULE_DELAY_IN_MILLIS = "schedule.delay.in.millis";
	private static String DATA_DELIVERY_DELAY = "data.delivery.delay";

	/**
	 * True is current run is master else false
	 */
	private boolean master;
	/**
	 * IP address of the master
	 */
	private String serverIp;
	/**
	 * Port used by master
	 */
	private int serverPort;
	/**
	 * Folder to be synced
	 */
	private String syncFolder;
	/**
	 * Millis in which scheduler is to be run
	 */
	private long scheduleDelayInMillis;
	/**
	 * Delay after which folder is to be synced again
	 */
	private long dataDeliveryDelay;

	public SyncProperty(Properties properties) throws NumberFormatException {
		this.master = properties.getProperty(MASTER).equals(MASTER) ? true : false;
		this.serverIp = properties.getProperty(SERVER_IP);
		this.serverPort = Integer.parseInt(properties.getProperty(SERVER_PORT));
		this.syncFolder = properties.getProperty(SYNC_FOLDER);
		this.scheduleDelayInMillis = Long.parseLong(properties.getProperty(SCHEDULE_DELAY_IN_MILLIS));
		this.dataDeliveryDelay = Long.parseLong(properties.getProperty(DATA_DELIVERY_DELAY));
	}

	public static String getMASTER() {
		return MASTER;
	}

	public static String getSERVER_IP() {
		return SERVER_IP;
	}

	public static String getSERVER_PORT() {
		return SERVER_PORT;
	}

	public boolean isMaster() {
		return master;
	}

	public String getServerIp() {
		return serverIp;
	}

	public int getServerPort() {
		return serverPort;
	}

	public static String getSYNC_FOLDER() {
		return SYNC_FOLDER;
	}

	public String getSyncFolder() {
		return syncFolder;
	}

	public static String getFILE_TREE_NAME() {
		return FILE_TREE_NAME;
	}

	public static String getSCHEDULE_DELAY_IN_MILLIS() {
		return SCHEDULE_DELAY_IN_MILLIS;
	}

	public static String getDATA_DELIVERY_DELAY() {
		return DATA_DELIVERY_DELAY;
	}

	public long getScheduleDelayInMillis() {
		return scheduleDelayInMillis;
	}

	public long getDataDeliveryDelay() {
		return dataDeliveryDelay;
	}

	@Override
	public String toString() {
		return "SyncProperty [master=" + master + ", serverIp=" + serverIp + ", serverPort=" + serverPort
				+ ", syncFolder=" + syncFolder + ", scheduleDelayInMillis=" + scheduleDelayInMillis
				+ ", dataDeliveryDelay=" + dataDeliveryDelay + "]";
	}

}
