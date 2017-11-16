package com.socialcops.models;

import java.io.Serializable;

import org.joda.time.DateTime;

public class FileTreeWrapper implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4703885582357543365L;
	private FileTree fileTree;
	private DateTime curDataTime;

	public FileTreeWrapper(FileTree fileTree, DateTime curDataTime) {
		this.fileTree = fileTree;
		this.curDataTime = curDataTime;
	}

	public FileTreeWrapper() {
	}

	public FileTree getFileTree() {
		return fileTree;
	}

	public void setFileTree(FileTree fileTree) {
		this.fileTree = fileTree;
	}

	public DateTime getCurDataTime() {
		return curDataTime;
	}

	public void setCurDataTime(DateTime curDataTime) {
		this.curDataTime = curDataTime;
	}

}
