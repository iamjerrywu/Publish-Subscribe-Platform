package naming;

import java.util.*;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * File System -- Basically we implement the entire file system following the Multiple Leaf
 * Tree Sturcture. Each node serve as a directory (details refer to Directory.java File
 * System support create, delete, search, check, list all the directories and filese in this
 * system. Naming server based on this file system properties and know what exactly is going
 * on outward in those storage servers it connects to.
 */

public class FileSystem {

    /**
     * Root directory ('/')
     */
    Directory rootDirectory = new Directory("/", "", null, false, null);

    /**
     * Constructor for FileSystem
     */
    public FileSystem() {
    }

    /**
     * Constructor for FileSystem
     *
     * @param path String of path need to be included for FileSystem
     */
    public FileSystem(String path) {
        this.createFile(path, null);
    }

    /**
     * Create file in file system. Need the path name and which storage server contains current file
     * Traverse all the directory name like prefix strings, and for each level of directory, need to creat
     * that Directory (File) Object
     *
     * @param path String of path need to be included for FileSystem
     * @param storageServerInfo storage server that contains current file
     */
    void createFile(String path, StorageServerInfo storageServerInfo) {
        String[] directoryList = format(path);

        Directory curDirectory = this.rootDirectory;
        String directoryPathName = "";
        // traverse all the directory name like prefix strings
        for (int i = 0; i < directoryList.length; i++) {
            directoryPathName += "/" + directoryList[i];
            if (curDirectory.hasChildDirectory(directoryPathName)) {
                curDirectory = curDirectory.getChildDirectories().get(directoryPathName);
            } else {
                Directory newDirectory;
                // only file that need to consider storage server, since storage server doesn't store directory
                if (i == directoryList.length - 1) {
                    newDirectory = new Directory(directoryPathName, directoryList[i], curDirectory, true, storageServerInfo);
                } else {
                    newDirectory = new Directory(directoryPathName, directoryList[i], curDirectory, false, null);
                }
                curDirectory.addChildDirectory(newDirectory);
                curDirectory = newDirectory;
            }
        }
    }

    /**
     * Create directory in file system. Need the path name. Traverse all the directory name like prefix strings,
     * and for each level of directory, need to creat that Directory (File) Object
     *
     * @param path String of path need to be included for FileSystem
     */
    void createDirectory(String path) {
        if (!parentDirectoryExist(path)) {
            System.out.println("parent directory doesn't exist");
            return;
        }

        String[] directoryList = format(path);
        Directory curDirectory = this.rootDirectory;
        String directoryPathName = "";
        for (int i = 0; i < directoryList.length; i++) {
            directoryPathName += "/" + directoryList[i];
            if (curDirectory.hasChildDirectory(directoryPathName)) {
                curDirectory = curDirectory.getChildDirectories().get(directoryPathName);
            } else {
                Directory newDirectory = new Directory(directoryPathName, directoryList[i], curDirectory, false, null);
                curDirectory.addChildDirectory(newDirectory);
                curDirectory = newDirectory;
            }
        }
    }

    /**
     * Delete file, check if file exists then delete it. After delete the file, check if those parent directory
     * is empty (not including any files), then they should also be deleted. To achieve this, recursively trace up
     * to the parent directory and check if it's child directories is empty, then delete it recursively trace up
     * until reach the root directory
     *
     *
     * @param path String of path need to be included for FileSystem
     */
    void deleteFile(String path) {
        if (path.equals("/")) return;
        path = path.substring(1);
        Directory curDirectory = findFile(path);
        if (curDirectory == null) {
            return;
        }
        do {
            if (curDirectory.getParentDirectory() == rootDirectory) {
                rootDirectory.getChildDirectories().remove(curDirectory.getPathName());
                break;
            }
            Directory parent = curDirectory.getParentDirectory();
            parent.getChildDirectories().remove(curDirectory.getPathName());
            curDirectory = parent;
        } while(curDirectory.getChildDirectories().size() == 0);
    }

    /**
     * Check is current path is valid ot not
     *
     * @param path String of directory need to be checked
     * @return boolean if valid path then true otherwise false
     */
    boolean isValidPath(String path) {
        if (path.equals("") || path.contains(":") || path.charAt(0) != '/')
            return false;
        return true;
    }

    /**
     * Check is file system has this directory
     *
     * @param path String of directory need to be checked
     * @return boolean if directory exist (true) or not(false)
     */
    boolean hasDirectory(String path) {
        return findFile(path) != null;
    }

    /**
     * Check is existed directory is a file or not
     *
     * @param path String of directory need to be checked
     * @return boolean if directory existed as a file (true) or not(false)
     */
    boolean isDirectory(String path) {
        Directory directory = findFile(path);
        if (directory == null) {
            return false;
        }
        return directory.isFile() != true;
    }

    /**
     * List all the files that under current level of path.
     *
     * @param path String of path need to be checked
     * @return array of string files name that existed under that level of directory
     */
    String[] listFiles(String path) {
        Directory directory = findFile(path);
        List<String> returnedFiles = new ArrayList<String>();

        for (Directory childDirectory : directory.getChildDirectories().values()) {
            returnedFiles.add(childDirectory.getName());
        }
        return returnedFiles.toArray(new String[returnedFiles.size()]);
    }

    /**
     * List all the files that under current level of path as well as the child directories. To achieve this,
     * recusively traverse down the child directories until all directory has been visited
     *
     * @param path String of path need to be checked
     * @return array of string files name that existed under that level of directory as well as child directories.
     *
     */
    String[] listAllFiles(String path) {
        Directory directory = findFile(path);
        List<String> returnedFiles = new ArrayList<String>();
        if (directory != null && directory.isFile()) {
            returnedFiles.add(directory.getPathName());
        } else {
            for (Directory childDirectory : directory.getChildDirectories().values()) {
                search(childDirectory, returnedFiles);
            }
        }
        return returnedFiles.toArray(new String[returnedFiles.size()]);

    }

    /**
     * Recursioni search function help to traverse down all the child directory based on current directory
     *
     * @param directory Current directory that waiting to be explored
     * @param returnedFiles List of string that need to be returned
     *
     */
    void search(Directory directory, List<String> returnedFiles) {
        if (directory.isFile()) {
            returnedFiles.add(directory.getPathName());
            return;
        }
        for (Directory childDirectory : directory.getChildDirectories().values()) {
            search(childDirectory, returnedFiles);
        }
    }

    /**
     * Check if parent directory exist in the file system for specific path
     *
     * @param path path that need to be checked
     * @return boolean that if parent directory exist for current path
     */
    boolean parentDirectoryExist(String path) {
        if (path.equals("/") || path.equals("//")) return true;
        String[] directoryList = format(path);
        Directory curDirectory = this.rootDirectory;
        String directoryPathName = "";
        for (int i = 0; i < directoryList.length; i++) {
            directoryPathName = '/' + directoryList[i];
            if (i < directoryList.length - 1 && !curDirectory.hasChildDirectory(directoryPathName)) {
                return false;
            }
        }
        return true;
    }


    /**
     * Find the directory object based on specific path
     *
     * @param path path that need to search for that directory
     * @return Directory that found based on path
     */
    Directory findFile(String path) {
        if (!isValidPath(path)) return null;
        if (path.equals("/") || path.equals("//")) return rootDirectory;

        String[] directoryList = format(path);
        String directoryPathName = "";
        Directory curDirectory = rootDirectory;
        for (int i = 0; i < directoryList.length; i++) {
            directoryPathName += "/" + directoryList[i];

            if (curDirectory.hasChildDirectory(directoryPathName)) {
                curDirectory = curDirectory.getChildDirectories().get(directoryPathName);
            } else {
                return null;
            }
        }
        return curDirectory;
    }

    /**
     * Format the path string to standardized format, i.e: dir1/dir2/file, then split it based
     * on diameter "/"
     *
     * @param path path that need to be formatted and splitted
     * @return Array of string for each level of directory name
     */
    String[] format(String path) {
        if(path.length() > 1 && path.charAt(1) == '/') return path.substring(2).split("/");
        if(path.charAt(0) == '/') return path.substring(1).split("/");
        return path.split("/");
    }

    /**
     * Helper functon that recursively print the file system from a specific directory and level. It will
     * recursively print all the child directory information
     *
     * @param directory the directory that want to start traverse down
     * @param level the level value that wnat to start traverse down
     */
    void printFileSystem(Directory directory, int level) {
        System.out.println("level = :" + level);
        System.out.println("cur_directory path name = :" + directory.getPathName());
        if (directory.isFile()) return;
        for (String name : directory.getChildDirectories().keySet()) {
            printFileSystem(directory.getChildDirectories().get(name), level + 1);
        }
    }
}