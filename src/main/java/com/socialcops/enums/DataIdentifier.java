package com.socialcops.enums;

/**
 * @author PratickChokhani Used to identify the data being sent over network
 */
public enum DataIdentifier {
	/**
	 * Request complete file tree
	 */
	RTC("RTC"),
	/**
	 * Request file tree difference
	 */
	RTD("RTD"),
	/**
	 * Request specific file
	 */
	RFI("RFI"),
	/**
	 * Return specified file
	 */
	FIR("FIR"),
	/**
	 * Return File not found
	 */
	FNF("FNF"),
	/**
	 * Return File Difference
	 */
	FID("FID"),
	/**
	 * Return File Tree
	 */
	FIT("FIT"),
	/**
	 * Invalid input
	 */
	INVALID("INVALID");

	private byte[] nameInByte;

	private DataIdentifier(String name) {
		this.nameInByte = name.getBytes();
	}

	public byte[] getNameInByte() {
		return nameInByte;
	}

}
