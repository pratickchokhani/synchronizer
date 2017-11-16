package com.socialcops.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.joda.time.DateTime;

public class FileTree implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8325978093455770240L;
	private boolean directory;
	private String name;
	private String absolutePath;
	private String relativePath;
	private DateTime lastModified;
	private List<FileTree> childs;

	public FileTree() {
	}

	public FileTree(boolean directory, String name, String absolutePath, String relativePath, DateTime lastModified) {
		super();
		this.directory = directory;
		this.name = name;
		this.absolutePath = absolutePath;
		this.relativePath = relativePath;
		this.lastModified = lastModified;
		childs = new ArrayList<FileTree>();
	}

	public boolean isDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public DateTime getLastModified() {
		return lastModified;
	}

	public void setLastModified(DateTime lastModified) {
		this.lastModified = lastModified;
	}

	public List<FileTree> getChilds() {
		return childs;
	}

	public void setChilds(List<FileTree> childs) {
		this.childs = childs;
	}

	@Override
	public String toString() {
		return "FileTree [directory=" + directory + ", name=" + name + ", absolutePath=" + absolutePath
				+ ", relativePath=" + relativePath + ", lastModified=" + lastModified + ", childs=" + childs + "]";
	}

	public static class SortComparator implements Comparator<FileTree> {

		public int compare(FileTree o1, FileTree o2) {
			return o1.getName().compareTo(o2.getName());
		}

	}
}
