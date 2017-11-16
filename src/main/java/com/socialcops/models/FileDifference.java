package com.socialcops.models;

import java.io.Serializable;

import org.joda.time.DateTime;

import com.socialcops.enums.FileOperation;

/**
 * @author PratickChokhani File difference containing the operation, path and
 *         last modified data of the file
 */
public class FileDifference implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5409708986586576568L;
	private FileOperation fileOperation;
	private String path;
	private DateTime lastModified;

	public FileDifference(FileOperation fileOperation, String path, DateTime lastModified) {
		super();
		this.fileOperation = fileOperation;
		this.path = path;
		this.lastModified = lastModified;
	}

	public FileDifference() {
	}

	public FileOperation getFileOperation() {
		return fileOperation;
	}

	public void setFileOperation(FileOperation fileOperation) {
		this.fileOperation = fileOperation;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public DateTime getLastModified() {
		return lastModified;
	}

	public void setLastModified(DateTime lastModified) {
		this.lastModified = lastModified;
	}

	@Override
	public String toString() {
		return "FileDifference [fileOperation=" + fileOperation + ", path=" + path + ", lastModified=" + lastModified
				+ "]";
	}

}
