package com.socialcops.models;

import java.util.Arrays;

/**
 * @author PratickChokhani Contains data identifier and next data size to
 *         identify next data
 */
public class SocketStreamData {

	private byte[] dataIdentifier;
	private long dataSize;

	public SocketStreamData(byte[] dataIdentifier, long dataSize) {
		this.dataIdentifier = dataIdentifier;
		this.dataSize = dataSize;
	}

	public SocketStreamData() {
	}

	public byte[] getDataIdentifier() {
		return dataIdentifier;
	}

	public void setDataIdentifier(byte[] dataIdentifier) {
		this.dataIdentifier = dataIdentifier;
	}

	public long getDataSize() {
		return dataSize;
	}

	public void setDataSize(long dataSize) {
		this.dataSize = dataSize;
	}

	@Override
	public String toString() {
		return "SocketStreamData [dataIdentifier=" + Arrays.toString(dataIdentifier) + ", dataSize=" + dataSize + "]";
	}

}
