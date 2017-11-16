package com.socialcops.models;

import java.io.Serializable;
import java.util.List;

import org.joda.time.DateTime;

public class FileDifferenceData implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 742078543326970740L;
	private List<FileDifference> fileDifferences;
	private DateTime lastUpdate;
	private DateTime currentDataTime;

	public FileDifferenceData(List<FileDifference> fileDifferences, DateTime lastUpdate, DateTime currentDataTime) {
		this.fileDifferences = fileDifferences;
		this.lastUpdate = lastUpdate;
		this.currentDataTime = currentDataTime;
	}

	public FileDifferenceData() {
	}

	public List<FileDifference> getFileDifferences() {
		return fileDifferences;
	}

	public void setFileDifferences(List<FileDifference> fileDifferences) {
		this.fileDifferences = fileDifferences;
	}

	public DateTime getLastUpdate() {
		return lastUpdate;
	}

	public void setLastUpdate(DateTime lastUpdate) {
		this.lastUpdate = lastUpdate;
	}

	public DateTime getCurrentDataTime() {
		return currentDataTime;
	}

	public void setCurrentDataTime(DateTime currentDataTime) {
		this.currentDataTime = currentDataTime;
	}

	@Override
	public String toString() {
		return "FileDifferenceData [fileDifferences=" + fileDifferences + ", lastUpdate=" + lastUpdate
				+ ", currentDataTime=" + currentDataTime + "]";
	}

}
