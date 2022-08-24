package naming;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * Directory -- each directory act like the tree node in the entire filesystem structure. It contains information like
 *  1. relationship info to parent directory and children directory
 *  2. properties identification (directory or file)
 *  3. lock / unlock required variables
 *  4. serving queue for load balancing
 */

public class Directory {
    /**
     * String of path name (from root)
     */
    private String pathName;
    /**
     * String of level name
     */
    private String name;
    /**
     * Parent directory of current directory
     */
    private Directory parentDirectory;
    /**
     * Map of current directory to all child directories
     */
    private HashMap<String, Directory> childDirectories = new HashMap<>();
    /**
     * Boolean of whether current directory is a file or not
     */
    private boolean isFile;
    /**
     * Concurrent map the storage server that has current directory as files
     */
    private ConcurrentHashMap<String, StorageServerInfo> storageServerMap = new ConcurrentHashMap<>();

    /**
     * HashSet the subscriber that who subscribe this file
     */
    private HashSet<String> subscriberSet = new HashSet<>();

    /**
     * Interger of shared lock count, served as semaphore for shared locking mechanism, need synchronized protection
     *
     */
    private int sharedLockCnt;
    /**
     * Boolean of exclusive write, served as mutex for exclusive locking mechanism, need synchronized protection
     */
    private boolean exclusiveWrite;
    /**
     * Serving queue for load balancing, need synchronized protection
     */
    private Queue<Integer> queue;
    /**
     * Integer for miliseconds delay during thread sleep
     */
    private final int delayMiliSec = 1;
    /**
     * Integer value of how many read times that file need to be replicated
     */
    private final int replicateScale = 20;
    /**
     * integer value of counting how many file reads
     */
    private int fileReadCnt;

    /**
     * Constructor for Directory
     *
     * @param pathName          path name
     * @param name              current directory name
     * @param parentDirectory   the parent directory
     * @param isFile            whether current directory is file or not
     * @param storageServerInfo storage server that contains current directory
     */
    public Directory(String pathName, String name, Directory parentDirectory, boolean isFile, StorageServerInfo storageServerInfo) {
        this.pathName = pathName;
        this.name = name;
        this.isFile = isFile;
        this.parentDirectory = parentDirectory;
        this.sharedLockCnt = 0;
        this.exclusiveWrite = false;
        this.queue = new LinkedList<>();
        this.fileReadCnt = 0;
        if (storageServerInfo != null) {
            this.storageServerMap.put(storageServerInfo.getClientPort() + "/" + storageServerInfo.getCommandPort(), storageServerInfo);
        }
    }

    /**
     * Get directory path name
     *
     * @return String of path name
     */
    public String getPathName() {
        return this.pathName;
    }
    /**
     * Get directory current level name
     *
     * @return String of current level name
     */
    public String getName() {
        return this.name;
    }
    /**
     * Get directory parent directory
     *
     * @return Directory of parent directory
     */
    public Directory getParentDirectory() {
        return this.parentDirectory;
    }

    /**
     * Get child directories of current director
     *
     * @return hash map of directories of child directory
     */
    public HashMap<String, Directory> getChildDirectories() {
        return this.childDirectories;
    }

    /**
     * Identify whether directory is file or not
     *
     * @return boolean whether file or not
     */
    public boolean isFile() {
        return isFile;
    }
    /**
     * Get shared lock count
     *
     * @return integer of shared lock count
     */
    public int getsharedLockCnt() {
        return sharedLockCnt;
    }

    /**
     * Get storage mapping table
     *
     * @return hash mapping table of storage server that contains current directory
     */
    public ConcurrentHashMap<String, StorageServerInfo> getStorageServerMap() {
        return this.storageServerMap;
    }

    /**
     * Get subscriber mapping table
     *
     * @return hash mapping table of storage server that contains current directory
     */
    public HashSet<String> getSubscriberSet() {
        return this.subscriberSet;
    }

    /**
     * Add subscriber mapping table
     */
    public void addSubscriber(String subscriberID) {
        this.subscriberSet.add(subscriberID);
    }

    /**
     * Remove subscriber mapping table
     */
    public void removeSubscriber(String subscriberID) {
        this.subscriberSet.remove(subscriberID);
    }

    /**
     * Add directory to child directories
     *
     * @param directory directory to be added
     */
    public void addChildDirectory(Directory directory) {
        if (!hasChildDirectory(directory.pathName)) {
            this.childDirectories.put(directory.pathName, directory);
        }
    }

    /**
     * Justify if has child directories
     *
     * @param directoryPathName directory path name
     * @return boolean of whether has child directory or not
     */
    public boolean hasChildDirectory(String directoryPathName) {
        return this.childDirectories.containsKey(directoryPathName);
    }

    /**
     * Get one random storage server that contain current directory
     *
     * @return storage server info of the server contain current directory
     */
    StorageServerInfo genRandomStorageServerInfo() {
        Object[] keys = storageServerMap.keySet().toArray();
        String key = (String)keys[new Random().nextInt(keys.length)];
        return storageServerMap.get(key);
    }

    /**
     * Add storage server that contains current directory
     *
     * @param serverKey           key (client port + "/" + command port)
     * @param storageServerInfo   storage server information
     */
    void addStorageServerInfo(String serverKey, StorageServerInfo storageServerInfo) {
        this.storageServerMap.put(serverKey, storageServerInfo);
    }

    /**
     * Remove storage server that contains current directory
     *
     * @param serverKey           key (client port + "/" + command port)
     */
    void removeStorageServerInfo(String serverKey) {
        this.storageServerMap.remove(serverKey);
    }


    /**
     * Execute exclusive lock procedure, if either follwing three condition is true, thread has to wait
     *  1. Current thread is in queue
     *  2. Directory already exclusive lock by others
     *  3. Shared lock by others
     *  After that, thread can modified exclusive write mutex to true, preventing others access current directory
     *
     * @param threadIndex      unique integer index for thread
     */
    void exclusiveLock(int threadIndex) throws InterruptedException {
        synchronized (this) {
            this.queue.add(threadIndex);
        }
        while (queue.peek() != threadIndex || this.exclusiveWrite || this.sharedLockCnt > 0) {
            Thread.sleep(this.delayMiliSec);
        }
        synchronized (this) {
            this.exclusiveWrite = true;
            queue.remove();
        }
    }

    /**
     * Check if exclusive lock invalid, if true then it's invalid
     *
     * @return boolean true if invalid otherwise false
     */
    boolean isExclusiveLockInvalid() {
        return this.exclusiveWrite == true;
    }

    /**
     * Execute shared lock procedure, if either follwing three condition is true, thread has to wait
     *  1. Current thread is in queue
     *  2. Directory already exclusive lock by others
     *  After that, thread can add shared lock count, and for those want to shared lock, they are allowed as well
     *  However, exclusive lock users have to wait
     *
     * @param threadIndex      unique integer index for thread
     */
    void sharedLock(int threadIndex) throws InterruptedException {
        synchronized (this) {
            this.queue.add(threadIndex);
        }
        while (queue.peek() != threadIndex || this.exclusiveWrite) {
            Thread.sleep(this.delayMiliSec);
        }
        synchronized (this) {
            queue.remove();
            this.sharedLockCnt+=1;
        }
    }

    /**
     * Check if shared lock invalid, if it's negative then it's invalid
     *
     * @return boolean true if invalid otherwise false
     */
    boolean isSharedLockInvalid() {
        return this.sharedLockCnt < 0;
    }

    /**
     * Execute exclusive unlock procedure, just modify exclusive write to false
     *
     */
    void exclusiveUnlock() {
        synchronized (this) {
            this.exclusiveWrite = false;
        }
    }

    /**
     * Check if exclusive write value is invalid before unlock, if it's false then it's invalid
     *
     * @return boolean true if invalid otherwise false
     */
    boolean isExclusiveUnlockInvalid() {
        return this.exclusiveWrite == false;
    }

    /**
     * Execute shared unlock procedure, just minus one on shared lock count
     *
     */
    void sharedUnlock() {
        synchronized (this) {
            this.sharedLockCnt-=1;
        }
    }

    /**
     * Check if shared lock count value is invalid before unlock, if it's not greater than 0 then it's invalid
     *
     * @return boolean true if invalid otherwise false
     */
    boolean isSharedUnlockInvalid() {
        return this.sharedLockCnt <= 0;
    }

    /**
     * Check if read count is invalid, if it's negative then it's invalid
     *
     * @return boolean true if invalid otherwise false
     */
    boolean isReadCountInvalid() {
        return this.sharedLockCnt < 0;
    }

    /**
     * Check if current directory need to be replicated, need to replicate every 20 times read actions
     * That is, one storage server server 20 times read, if more, than add one extra storage server
     *
     * @return boolean true if need to replicate
     */
    public boolean needReplicate() {
        return this.isFile && this.fileReadCnt / this.replicateScale != this.storageServerMap.size() - 1;
    }

    /**
     * Check if invalidation action is required. When write action and more than one server containing current
     * directory, then save one, and delete others
     *
     * @return boolean true if need to invalidation
     */
    public boolean needInvalidation() {
        return this.isFile && this.storageServerMap.size() > 1;
    }

    void addFileReadCnt() {
        synchronized (this) {
            if (this.isFile)
                this.fileReadCnt+=1;
        }
    }
    /**
     * Reset file read count number to zero
     */
    void resetFileReadCnt() {
        synchronized (this) {
            this.fileReadCnt = 0;
        }
    }

    /**
     * toString method to present contents
     *
     * @return String representation of contents
     */
    @Override
    public String toString() {
        return "directory_path_name: " + this.pathName;
    }
}