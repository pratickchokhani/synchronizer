package com.socialcops.directory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;

import com.socialcops.enums.FileOperation;
import com.socialcops.models.FileDifference;
import com.socialcops.models.FileTree;

/**
 * @author PratickChokhani Contains methods related to directories
 *
 */
public class DirectoryUtils {

	/**
	 * Path to folder to be synchronized
	 */
	private static Path syncFolderPath = null;

	/**
	 * Calculate difference between source file tree and old file tree to figure
	 * out what need to be updated. Any file whose modified data is greater in
	 * source file tree or is not present in old file tree need to be updated.
	 * Any file not present in source file tree needs to be deleted
	 * 
	 * @param sourceFileTree
	 * @param oldFileTree
	 * @param startTime
	 *            last update time. So, any change after that can be taken into
	 *            account
	 * @return File differences containing file path, operation and last
	 *         modified data
	 */
	public static List<FileDifference> calculateDifference(FileTree sourceFileTree, FileTree oldFileTree,
			DateTime startTime) {

		List<FileDifference> fileDifferences = new ArrayList<>();
		List<FileTree> sourceChilds = sourceFileTree.getChilds();
		List<FileTree> oldChilds;
		if (oldFileTree != null) {
			oldChilds = oldFileTree.getChilds();
		} else {
			oldChilds = new ArrayList<>(0);
		}

		int indexSource = 0, indexOld = 0, compare = 0;
		while (indexSource < sourceChilds.size() && indexOld < oldChilds.size()) {
			sourceFileTree = sourceChilds.get(indexSource);
			oldFileTree = oldChilds.get(indexOld);
			compare = sourceFileTree.getName().compareTo(oldFileTree.getName());
			if (compare < 0) {
				createFileDifference(FileOperation.UPDATE, sourceFileTree, fileDifferences);
				calculateDifference(sourceFileTree, null, startTime);
				indexSource++;
			} else if (compare > 0) {
				createFileDifference(FileOperation.DELETE, oldFileTree, fileDifferences);
				indexOld++;
			} else {
				if (oldFileTree.isDirectory() != sourceFileTree.isDirectory()) {
					createFileDifference(FileOperation.DELETE, oldFileTree, fileDifferences);
					createFileDifference(FileOperation.UPDATE, sourceFileTree, fileDifferences);
					oldFileTree = null;
				} else if (oldFileTree.getLastModified().isBefore(sourceFileTree.getLastModified())
						|| startTime.isBefore(sourceFileTree.getLastModified())) {
					createFileDifference(FileOperation.UPDATE, sourceFileTree, fileDifferences);
				}
				fileDifferences.addAll(calculateDifference(sourceFileTree, oldFileTree, startTime));
				indexOld++;
				indexSource++;
			}
		}
		while (indexOld < oldChilds.size()) {
			oldFileTree = oldChilds.get(indexOld);
			createFileDifference(FileOperation.DELETE, oldFileTree, fileDifferences);
			indexOld++;
		}

		while (indexSource < sourceChilds.size()) {
			sourceFileTree = sourceChilds.get(indexSource);
			createFileDifference(FileOperation.UPDATE, sourceFileTree, fileDifferences);
			fileDifferences.addAll(calculateDifference(sourceFileTree, null, startTime));
			indexSource++;
		}

		return fileDifferences;
	}

	/**
	 * Creates file difference for specified operation and file. Does not create
	 * if file operation is update and the file is a directory. The created file
	 * difference is added to fileDifferences list
	 * 
	 * @param fileOperation
	 * @param fileTree
	 * @param fileDifferences
	 */
	public static void createFileDifference(FileOperation fileOperation, FileTree fileTree,
			List<FileDifference> fileDifferences) {
		String path = fileTree.getRelativePath();
		if (!(fileTree.isDirectory() && fileOperation == FileOperation.UPDATE)) {
			fileDifferences.add(new FileDifference(fileOperation, path, fileTree.getLastModified()));
		}
	}

	/**
	 * Creates file tree form source file and returns it
	 * 
	 * @param sourceFile
	 * @return
	 */
	public static FileTree createFileTree(File sourceFile) {
		FileTree fileTree = new FileTree(sourceFile.isDirectory(), sourceFile.getName(), sourceFile.getAbsolutePath(),
				syncFolderPath.relativize(Paths.get(sourceFile.getAbsolutePath())).toString(),
				new DateTime(sourceFile.lastModified()));

		if (sourceFile.isDirectory()) {
			File[] files = sourceFile.listFiles();
			List<FileTree> fileTreeList = fileTree.getChilds();
			for (File file : files) {
				fileTreeList.add(createFileTree(file));
			}
			Collections.sort(fileTreeList, new FileTree.SortComparator());
		}

		return fileTree;
	}

	public static long getLastModifiedTime(Path path) throws IOException {
		if (Files.exists(path)) {
			return Files.getLastModifiedTime(path).toMillis();
		}
		return 0L;
	}

	public static void setSyncFolder(String syncFolder) {
		syncFolderPath = Paths.get(syncFolder);
	}

}
