package naming;

import Debug.Debug;
import com.google.gson.Gson;
import jsonhelper.*;
import spark.Service;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * Naming Server -- Serve as an interface between clients and the storage server which stores the files. The naming
 * has the file system information on what's going on in the world of those storage servers. The inbuild tree
 * structure file syetem can track down all the files existed in storage which is connect to the file system.
 *
 * It also support multiple functions for both the clients and the storage server. For clients, they can only
 * access storage server after gaining access from naming servers, if they want to read/modify/delete files on storage
 * server. Storage server has to register on naming server in order to connect to this file system. More details
 * will be explained in following document.
 *
 * Restful API are implemneted based on Java spark libraries, allowing communications between servers, clients. Two
 * Spark service is init (1) service, that handle clients requests (2) registration, that handle storage regisration
 */

public class NamingServer {

    /**
     * Integer of service port number
     */
    private int servicePort;
    /**
     * Integer of registration port number
     */
    private int registrationPort;
    /**
     * Concurrent map the storage server that connect to this naming server
     */
    private ConcurrentHashMap<String, StorageServerInfo> storageServerMap;
    /**
     * HashSet the publisher that exist in this system
     */
    private HashSet<String> publisherSet;

    /**
     * HashSet the publisher that exist in this system
     */
    private HashMap<String, StorageServerInfo> publisherToServer;
    /**
     * HashSet the subscriber that exist in this system
     */
    private HashSet<String> subscriberSet;

    /**
     * HashSet the subscriber that exist in this system
     */
    private HashMap<String, StorageServerInfo> subscriberToServer;

    /**
     * HashMap the file that are subscribed
     */
    private HashMap<String, HashSet<String>> fileToSubscribers;
    /**
     * File system for this naming server
     */
    private FileSystem fileSystem;

    /**
     * Spark service for service related functians for this naming server
     */
    private Service service;
    /**
     * Spark service for registration related functians for this naming server
     */
    private Service registration;
    /**
     * HashSet to record which thread is interacting with the naming server when they are waiting
     * for certain operations
     */
    public HashSet<Integer> threadIndexes;

    /**
     * Debug object
     */
    public Debug debug = new Debug();

    /**
     * Constructor for naming server
     *
     * @param sp         service port number
     * @param rp         registration port number
     */
    public NamingServer(int sp, int rp) {
        this.servicePort = sp;
        this.registrationPort = rp;
        this.storageServerMap = new ConcurrentHashMap<>();
        this.publisherSet = new HashSet<>();
        this.publisherToServer = new HashMap<>();
        this.subscriberSet = new HashSet<>();
        this.subscriberToServer = new HashMap<>();
        this.fileToSubscribers = new HashMap<>();

        this.fileSystem = new FileSystem();
        threadIndexes = new HashSet<>();
    }

    /**
     * Start service that contains bunch of RestFul API procedures for each client HTTP requests. In the
     * very beginning, need to init the spark server for service.
     *
     * @param g Gson object
     */
    public void startService(Gson g) throws IOException {
        service = Service.ignite().port(this.servicePort).threadPool(20);
        service.init();

        /** handle is_valid_path */
        isValidPathHandler(g);

        /** handle is_directory */
        isDirectoryHandler(g);

        /** handle list */
        listHandler(g);

        /** handle create_directory */
        createDirectoryHandler(g);

        /** handle create_file */
        createFileHandler(g);

        /** handle create_file */
        getStorageHandler(g);

        /** handle lock */
        lockHandler(g);

        /** handle unlock */
        unlockHandler(g);

        /** handle delete */
        deleteHandler(g);

        /** handle publisher register */
        publisherRegisterHandler(g);

        /** handle subscriber register */
        subscriberRegisterHandler(g);

        /** handle replication */
        replicationHandler(g);
    }

    /**
     * Start registration that contains bunch of RestFul API procedures for each storage server HTTP requests. In the
     * very beginning, need to init the spark server for registration.
     *
     * @param g Gson object
     */
    public void startRegistratioin(Gson g) throws IOException {
        registration = Service.ignite().port(this.registrationPort).threadPool(20);
        registration.init();

        /** handle registration */
        registrationHandler(g);
    }


    /**
     *  Handler function to deal with path whether it's invalid or not
     *  @param g Gson object
     */
    public void isValidPathHandler(Gson g) {
        this.service.post("/is_valid_path", (request, response) -> {

//            debug.redirectPrint(isValidPathHandler.txt);

            BooleanReturn booleanReturn;
            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }

            if (fileSystem.isValidPath(req.path)) {
                booleanReturn = new BooleanReturn(true);
            } else {
                booleanReturn = new BooleanReturn(false);
            }
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function to deal with path whether it's directory or not
     *
     *  @param g Gson object
     */
    public void isDirectoryHandler(Gson g) {
        service.post("/is_directory", (request, response) -> {

//            debug.redirectPrint("directoryHandler.txt");

            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.hasDirectory(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }

            BooleanReturn booleanReturn = new BooleanReturn(fileSystem.isDirectory(req.path));
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function to list all the files under current path
     *
     *  @param g Gson object
     */
    public void listHandler(Gson g) {
        service.post("/list", (request, response) -> {

//            debug.redirectPrint(listHandler.txt);

            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }

            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.hasDirectory(req.path) || (!fileSystem.isDirectory(req.path))) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }

            FilesReturn filesReturn = new FilesReturn(fileSystem.listFiles(req.path));
            String ret = g.toJson(filesReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function to create directory
     *
     *  @param g Gson object
     */
    public void createDirectoryHandler(Gson g) {
        service.post("/create_directory", (request, response) -> {
//            debug.redirectPrint("createDirectoryHandler.txt");
//            fileSystem.printFileSystem(fileSystem.rootDirectory, 0);

            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            // handle invalid cases
            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.parentDirectoryExist(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }

            // handle cases
            BooleanReturn booleanReturn;
            if (!fileSystem.hasDirectory(req.path)) {
                booleanReturn = new BooleanReturn(true);
                fileSystem.createDirectory(req.path);
            } else {
                booleanReturn = new BooleanReturn(false);
            }

            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function to create file, need to send http request to storage server in the end
     *
     *  @param g Gson object
     */
    public void createFileHandler(Gson g) {
        service.post("/create_file", (request, response) -> {

//            debug.redirectPrint();

            String content = request.body();

            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }

            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.parentDirectoryExist(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (storageServerMap.size() == 0) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalStateException", "No storage servers are connected");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }

            // handle cases
            BooleanReturn booleanReturn;
            if (!fileSystem.hasDirectory(req.path)) {
                booleanReturn = new BooleanReturn(true);
                StorageServerInfo storageServerInfo;
                if (req.publisherID == null) {
                    storageServerInfo = genRandomStorageServerInfo();

                } else {
                    storageServerInfo = publisherToServer.get(req.publisherID);
                }
                fileSystem.createFile(req.path, storageServerInfo);
                if (fileToSubscribers.containsKey(req.path)) {
                    for (String subscriberID : fileToSubscribers.get(req.path)) {
                        Directory directory = fileSystem.findFile(req.path);
                        directory.addSubscriber(subscriberID);
                    }
                }

                getHttpResponse("http://" + storageServerInfo.getIP() + ":" + storageServerInfo.getCommandPort() + "/storage_create", new PathRequest(req.path), g);
            } else {
                booleanReturn = new BooleanReturn(false);
            }
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function to get storage server based on a path
     *
     *  @param g Gson object
     */
    public void getStorageHandler(Gson g) {
        service.post("/getstorage", (request, response) -> {

//            debug.redirectPrint();
//            System.out.println("getStorageHandler");

            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }

            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.hasDirectory(req.path) || (fileSystem.isDirectory(req.path))) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }


            StorageServerInfo storageServerInfo = fileSystem.findFile(req.path).genRandomStorageServerInfo();
            ServerInfo serverInfo = new ServerInfo(storageServerInfo.getIP(), storageServerInfo.getClientPort());
            String ret = g.toJson(serverInfo);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function for lock. Before access file, client need to do lock beforehand. Details about how lock
     *  is appraoched, please refer to Directory.java. Also, those waiting for locks users will be put in queue
     *  and later served based on FIFO order.
     *
     *  Another feaeture called replicationi in here, is try to do load balancing when particular file is being read
     *  too many times. So every 20 times read a file will be replicated, and once a write (excluse lock) action,
     *  those files will be invalidated and only one updated remains.
     *
     *  @param g Gson object
     */
    public void lockHandler(Gson g) {
        service.post("/lock", (request, response) -> {

//            debug.redirectPrint("lockHandler.txt");
//            System.out.println("lockHandler");

            String content = request.body();
            LockRequest req;
            try {
                req = g.fromJson(content, LockRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.hasDirectory(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }

            int threadIndex = genUniqueIndex(100);

            Directory directory = fileSystem.findFile(req.path);

            // shared lock root
            if (!req.path.equals("/")) {
                fileSystem.rootDirectory.addFileReadCnt();
                fileSystem.rootDirectory.sharedLock(threadIndex);
            }

            // shared lock parent dir
            String curDirectory = "";
            String[] directoryList = fileSystem.format(req.path);
            for (int i = 0 ; i < directoryList.length - 1 ; i++) {
                curDirectory += "/" + directoryList[i];
                fileSystem.findFile(curDirectory).addFileReadCnt();
                fileSystem.findFile(curDirectory).sharedLock(threadIndex);
            }

            // lock directory
            if (req.exclusive) {
                directory.exclusiveLock(threadIndex);
            } else {
                directory.sharedLock(threadIndex);
            }

            if (directory.getSubscriberSet().contains(req.subscriberID)) {
                directory.removeSubscriber(req.subscriberID);
            }

            threadIndexes.remove(threadIndex);
            response.status(200);
            response.type("application/json");
            return "";
        });
    }


    /**
     *  Handler function for unlock. For those client just lock, should do unlock afterward. Details about how unlock
     *  is appraoched, please refer to Directory.java.
     *
     *  @param g Gson object
     */
    public void unlockHandler(Gson g) {
        service.post("/unlock", (request, response) -> {

//            debug.redirectPrint("unlockHandler.txt");
//            System.out.println("unlockHandler");

            String content = request.body();
            LockRequest req;
            try {
                req = g.fromJson(content, LockRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }

            if (!fileSystem.isValidPath(req.path) || !fileSystem.hasDirectory(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }

            Directory directory = fileSystem.findFile(req.path);

            // unlock lock root
            if (!req.path.equals("/")) {
                fileSystem.rootDirectory.sharedUnlock();
            }

            // shared unlock parent dir
            String curDirectory = "";
            String[] directoryList = fileSystem.format(req.path);
            for (int i = 0 ; i < directoryList.length - 1 ; i++) {
                curDirectory += "/" + directoryList[i];
                fileSystem.findFile(curDirectory).sharedUnlock();
            }

            // unlock directory
            if (req.exclusive) {
                directory.exclusiveUnlock();
            } else {
                directory.sharedUnlock();
            }
            if (directory.getSubscriberSet().size() == 0) {
                // delete file
                for (StorageServerInfo storageServerInfo : directory.getStorageServerMap().values()) {
                    getHttpResponse("http://" + storageServerInfo.getIP() + ":" + storageServerInfo.getCommandPort() + "/storage_delete", new PathRequest(req.path), g);
                }
            }

            response.status(200);
            response.type("application/json");
            return "";
        });
    }

    /**
     *  Handler function for delete files. If path is directory, delete all files in current and child directory.
     *  If path is a file, then delete that file. If multiple storage server has the same file, then send
     *  HTTP request to all of them to delete.
     *
     *  @param g Gson object
     */
    public void deleteHandler(Gson g) {
        service.post("/delete", (request, response) -> {

//            debug.redirectPrint("deleteHandler.txt");
//            System.out.println("deleteHandler");

            String content = request.body();
            PathRequest req;
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }

            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Give path is invalid");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.parentDirectoryExist(req.path) || !fileSystem.hasDirectory(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found");
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            // handle cases
            BooleanReturn booleanReturn = new BooleanReturn(true);
            String[] allChildFiles = fileSystem.listAllFiles(req.path);
            for (String filePath : allChildFiles) {
                for (StorageServerInfo storageServerInfo : fileSystem.findFile(filePath).getStorageServerMap().values()) {
                    getHttpResponse("http://" + storageServerInfo.getIP() + ":" + storageServerInfo.getCommandPort() + "/storage_delete", new PathRequest(req.path), g);
                }
            }
            fileSystem.deleteFile(req.path);

            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function for the publisher to register to the system with a assigned storage server
     *
     *  @param g Gson object
     */
    public void publisherRegisterHandler(Gson g) {
        this.service.post("/publisher_register", (request, response) -> {

//            debug.redirectPrint("publisherRegistrationHandler.txt");
//            System.out.println("publisherRegistrationHandler");

            String content = request.body();
            PublisherRegisterRequest req;

            try {
                req = g.fromJson(content, PublisherRegisterRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            if (storageServerMap.size() == 0) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalStateException", "No available storage server to assigned for");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }
            if (publisherSet.contains(req.publisherID)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Publisher is already registered");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }

            PublisherInfo publisherInfo = new PublisherInfo(req.publisherID);

            // check which connected storage server has the least assigned publisher
            StorageServerInfo assignedStorageServer = genRandomStorageServerInfo();
            int storageServerAssignedCnt = Integer.MAX_VALUE;
            for (StorageServerInfo ssi : storageServerMap.values()) {
                if (ssi.getPublisherMap().size() < storageServerAssignedCnt) {
                    assignedStorageServer = ssi;
                    storageServerAssignedCnt = ssi.getPublisherMap().size();
                }
            }
            assignedStorageServer.addAssignedPublisher(publisherInfo.getPublisherID(), publisherInfo);
            publisherSet.add(publisherInfo.getPublisherID());
            publisherToServer.put(publisherInfo.getPublisherID(), assignedStorageServer);

            String ret = g.toJson(assignedStorageServer);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler function for the subscriber to register to the system with a assigned storage server
     *
     *  @param g Gson object
     */
    public void subscriberRegisterHandler(Gson g) {
        this.service.post("/subscriber_register", (request, response) -> {

//            debug.redirectPrint("subscriberRegistrationHandler.txt");
//            System.out.println("subscriberRegistrationHandler");

            String content = request.body();
            SubscriberRegisterRequest req;

            try {
                req = g.fromJson(content, SubscriberRegisterRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            if (storageServerMap.size() == 0) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalStateException", "No available storage server to assigned for");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }
            if (subscriberSet.contains(req.subscriberID)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Subscriber is already registered");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }
            for (String keyWord : req.keyWords) {
                String file = "/" + keyWord;
                if (fileSystem.isValidPath(file)){
                    if (!fileToSubscribers.containsKey(file)) {
                        fileToSubscribers.put(file, new HashSet<>());
                    }
                    fileToSubscribers.get(file).add(req.subscriberID);
                }
            }
            SubscriberInfo subscriberInfo = new SubscriberInfo(req.subscriberID);

            // check which connected storage server has the least assigned publisher
            StorageServerInfo assignedStorageServer = genRandomStorageServerInfo();
            int storageServerAssignedCnt = Integer.MAX_VALUE;
            for (StorageServerInfo ssi : storageServerMap.values()) {
                if (ssi.getSubscriberMap().size() < storageServerAssignedCnt) {
                    assignedStorageServer = ssi;
                    storageServerAssignedCnt = ssi.getSubscriberMap().size();
                }
            }
            assignedStorageServer.addAssignedSubscriber(subscriberInfo.getSubscriberID(), subscriberInfo);
            subscriberSet.add(subscriberInfo.getSubscriberID());
            subscriberToServer.put(subscriberInfo.getSubscriberID(), assignedStorageServer);
            String ret = g.toJson(assignedStorageServer);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     *  Handler replication when files need to be copied between storage server. Basically the algorithm is simple, we
     *  only replicate file when needed, that's when the file is not existed on the storage server that it's assigned
     *  to a subscriber, but meantime file exists on other server. In this case, we need to copy the file from that
     *  server to the assigned server.
     *
     *  @param g Gson object
     */
    public void replicationHandler(Gson g) {
        this.service.post("/replication", (request, response) -> {

//            debug.redirectPrint("replicationHandler.txt");
//            System.out.println("replicationHandler");

            String content = request.body();
            ReplicationRequest req;

            try {
                req = g.fromJson(content, ReplicationRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            if (!fileSystem.isValidPath(req.path)) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "Path are invalid");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }
            if (storageServerMap.size() == 0) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalStateException", "No available storage server to assigned for");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }

            StorageServerInfo storageServerInfo = new StorageServerInfo(req.copy_ip, req.copy_client_port, req.copy_command_port);
            fileSystem.createFile(req.path, storageServerInfo);
            Directory directory = fileSystem.findFile(req.path);
            directory.addStorageServerInfo(storageServerInfo.getClientPort() + "/" + storageServerInfo.getCommandPort(), storageServerInfo);
            // first create file
            getHttpResponse("http://" + storageServerInfo.getIP() + ":" + storageServerInfo.getCommandPort() + "/storage_create", new PathRequest(req.path), g);

            // read content
            HttpResponse<String> res = getHttpResponse("http://" + req.existed_ip + ":" + req.existed_client_port + "/storage_size", new PathRequest(req.path), g);
            int size = (int)g.fromJson(res.body(), SizeReturn.class).size;
            res = getHttpResponse("http://" + req.existed_ip + ":" + req.existed_client_port + "/storage_read",
                    new ReadRequest(req.path, 0, size), g);
            String txt = g.fromJson(res.body(), DataReturn.class).data;
            // write content
            getHttpResponse("http://" + req.copy_ip + ":" + req.copy_client_port + "/storage_write",
                    new WriteRequest(req.path, 0, txt), g);

            String ret = g.toJson(new BooleanReturn(true));
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }


    /**
     *  Handler function for registration for storage server. Need to maintain or update the file system according
     *  to what files the storage server has.
     *
     *  @param g Gson object
     */
    public void registrationHandler(Gson g) {

        this.registration.post("/register", (request, response) -> {

//            debug.redirectPrint("registrationHandler.txt");
//            System.out.println("registrationHandler");

            String content = request.body();
            RegisterRequest req;

            try {
                req = g.fromJson(content, RegisterRequest.class);
            } catch (Exception e) {
                ExceptionReturn excepRet = new ExceptionReturn("Bad Request", "Bad Request");
                String ret = g.toJson(excepRet);
                response.status(400);
                response.type("application/json");
                return ret;
            }
            String mapKey = req.client_port + "/" + req.command_port;
            boolean err = false;
            List<String> deletedFiles = new ArrayList();
            if (!storageServerMap.containsKey(mapKey)) {
                StorageServerInfo storageServerInfo = new StorageServerInfo(req.storage_ip, req.client_port, req.command_port);
                storageServerMap.put(mapKey, storageServerInfo);
                err = false;
                for (String file : req.files) {
//                    System.out.println(file);
                    Directory directory = fileSystem.findFile(file);
                    if (directory != null) {
                        if (directory.getPathName().equals("/")) continue;
                        deletedFiles.add(directory.getPathName());
                    } else {
                        fileSystem.createFile(file, storageServerInfo);
//                        System.out.println("create file: " + file + "/" + storageServerInfo);
                    }
                }
            } else {
                err = true;
            }

            if (err) {
                ExceptionReturn excepRet = new ExceptionReturn("IllegalStateException", "This storage client already registered");
                String ret = g.toJson(excepRet);
                response.status(409);
                response.type("application/json");
                return ret;
            }
            FilesReturn filesReturn = new FilesReturn(new String[0]);
            if (deletedFiles.size() > 0) {
                filesReturn = new FilesReturn(deletedFiles.toArray(new String[deletedFiles.size()]));
            }
            String ret = g.toJson(filesReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /** Some helper functions */

    /**
     * Generate a unique integer index for thead that is interacting with current name server
     *
     * @param upperBnd maximum index numbe for thread
     * @return Integer of the index value
     */
    int genUniqueIndex(int upperBnd) {
        int threadIndex;
        do {
            threadIndex = new Random().nextInt(upperBnd);
        } while (threadIndexes.contains(threadIndex));
        threadIndexes.add(threadIndex);
        return threadIndex;
    }

    /**
     * Generate HTTP request to send to other client/server
     *
     * @param uriStr    the uri string
     * @param reqObj    Request object
     * @param g         Gson
     * @return http response
     */
    HttpResponse<String> getHttpResponse(String uriStr, Object reqObj, Gson g) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest sendReq = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .POST(HttpRequest.BodyPublishers.ofString(g.toJson(reqObj)))
                .build();
        return client.send(sendReq, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Generate a random storage server that's conect to naming server
     *
     * @return StorageServerInfo of taht server
     */
    StorageServerInfo genRandomStorageServerInfo() {
        Object[] keys = storageServerMap.keySet().toArray();
        String key = (String)keys[new Random().nextInt(keys.length)];
        return storageServerMap.get(key);
    }

    public void stop() {
        service.stop();
        registration.stop();
    }
}
