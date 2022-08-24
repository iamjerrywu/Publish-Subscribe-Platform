package storage;

import com.google.gson.Gson;
import common.Path;
import jsonhelper.*;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import spark.Service;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * Storage Server -- Storage servers serve as the containers/servers that hold
 * all of the files stored by the client. Each storage server is independent
 * from one another, and therefore, a storage server may hold different files
 * from another storage server. To manage all of the storage server, naming
 * servers are responsible for keeping track of not only all the ip addresses
 * and ports of each storage server, but also the content held on each storage
 * server. This allows clients to easily find differnet files on storage servers
 * but allows the file system to still be distributed.
 *
 * In order to interact with the storage server, we created a RESTful API using
 * the Spark package and allows for clients and naming servers to perform
 * certain actions on the storage server. For example, clients are able to use
 * the client API to read, write and get the size of files stored on a storage
 * server. Likewise, the naming server is able to use the command API to create,
 * delete, and copy files on a storage servers. In the case something is
 * malformed or something went wrong, the storage server will spit out an error.
 * Finally, the storage server returns all API requests with a JSON object with
 * fields that depend on the request being made.
 */
public class StorageServer {
    /**
     * Integer of client port number
     */
    private int clientPrt;
    /**
     * Integer of command port number
     */
    private int commandPrt;
    /**
     * Integer of registration port number
     */
    private int registrationPrt;

    /**
     * Spark service for client interface for this storage server
     */
    private Service clientService;
    /**
     * Spark service for command interface for this storage server
     */
    private Service commandService;
    /**
     * The root directory
     */
    private File root;
    /**
     * Gson instance that will be used to stringify and destringify json objects
     */
    private Gson g;

    /**
     * Constructor for storage server
     *
     * @param clientPrt       client port number
     * @param commandPrt      command port number
     * @param registrationPrt registration port number
     * @param root            a string that represents the path to the root
     *                        directory
     */
    public StorageServer(int clientPrt, int commandPrt, int registrationPrt, String root) {
        this.clientPrt = clientPrt;
        this.commandPrt = commandPrt;
        this.g = new Gson();
        this.registrationPrt = registrationPrt;
        this.root = new File(root);
    }

    /**
     * This function is used to start the RESTFUL API for the client service and
     * is used by clients to read, write, and get the size of a file stored on
     * the storage server.
     */
    public void startClientService() {
        clientService = Service.ignite().port(this.clientPrt).threadPool(20);
        clientService.init();
        storageSizeHandler();
        storageReadHandler();
        storageWriteHandler();
    }

    /**
     * This function is used to start the RESTFUL API for the command service and
     * is used by the naming server to create, delete, and copy files stored on the
     * storage server.
     */
    public void startCommandService() {
        commandService = Service.ignite().port(this.commandPrt).threadPool(20);
        commandService.init();
        storageCreateHandler();
        storageDeleteHandler();
        storageCopyHandler();
    }


    /**
     * Handler function to get the size of a certain file in this storage server
     */
    public void storageSizeHandler() {
        this.clientService.post("/storage_size", (request, response) -> {
            String content = request.body();
            PathRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found.");

            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (req.path == null) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            Integer size = null;
            synchronized (this) {
                File fileName = new File(root + req.path);
                if (fileName != null && fileName.exists() && fileName.isFile()) {
                    FileInputStream fileRead = new FileInputStream(fileName);
                    try {
                        size = fileRead.available();
                    } catch (Exception e) {
                        e.printStackTrace();
                        size = null;
                    } finally {
                        fileRead.close();
                    }
                }
            }

            if (size == null) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            File fileName = new File(root + req.path);
            SizeReturn sizeReturn = new SizeReturn(fileName.length());
            String ret = g.toJson(sizeReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     * Handler function to read the content of a certain file in the storage server.
     */
    public void storageReadHandler() {
        this.clientService.post("/storage_read", (request, response) -> {
            String content = request.body();
            ReadRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found.");
            try {
                req = g.fromJson(content, ReadRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (req.path == null || req.length < 0 || req.offset < 0L) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            byte[] fileContent = null;
            synchronized (this) {
                File fileName = new File(root + req.path);
                if (fileName != null && fileName.exists() && fileName.isFile()) {
                    RandomAccessFile fileRead = new RandomAccessFile(fileName, "r");
                    try {
                        fileContent = new byte[req.length];
                        fileRead.seek(req.offset);
                        fileRead.read(fileContent, 0, req.length);
                    } catch (Exception e) {
                        e.printStackTrace();
                        fileContent = null;
                    } finally {
                        fileRead.close();
                    }
                }

            }
            if (fileContent == null) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            DataReturn dataReturn = new DataReturn(new String(fileContent));
            String ret = g.toJson(dataReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }


    /**
     * Handler function to write specific content (in the form of a byte array) to a
     * specific file in this storage server.
     */
    public void storageWriteHandler() {
        this.clientService.post("/storage_write", (request, response) -> {
            String content = request.body();
            WriteRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("FileNotFoundException", "File/path cannot be found.");
            try {
                req = g.fromJson(content, WriteRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (req.path == null || req.data == null || req.offset < 0L) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean err = false;
            synchronized (this) {
                File fileName = new File(root + req.path);
                if (fileName != null && fileName.exists() && fileName.isFile()) {
                    FileOutputStream fileWrite = null;
                    FileInputStream fileRead = new FileInputStream(fileName);
                    try {
                        int fileSize = fileRead.available();
                        byte[] byteData = req.data.getBytes();
                        if (req.offset < fileSize) {
                            fileWrite = new FileOutputStream(fileName);
                            fileWrite.write(byteData, (int) req.offset, byteData.length);
                        } else {
                            fileWrite = new FileOutputStream(fileName, true);
                            fileWrite.write(byteData);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        err = true;
                    } finally {
                        if (fileWrite != null)
                            fileWrite.close();
                        fileRead.close();
                    }

                }

            }

            if (err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(!err);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }


    /**
     * Handler function to create a file with the given path on this storage server.
     * This function creates any necessary directories in order to successfuly
     * create the file.
     */
    public void storageCreateHandler() {
        this.commandService.post("/storage_create", (request, response) -> {
            String content = request.body();
            PathRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "IllegalArgumentException: path invalid.");
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (req.path == null) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            Path path = new Path(req.path);
            if (path.isRoot()) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean res = false;
            synchronized (this) {
                File fileName = path.toFile(this.root);
                if (fileName != null && !fileName.exists()) {
                    try {
                        fileName.getParentFile().mkdirs();
                        fileName.createNewFile();
                        res = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        res = false;
                    }

                }
            }
            BooleanReturn booleanReturn = new BooleanReturn(res);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     * Handler function to delete a file or directory with the given path from this
     * storage server.
     */
    public void storageDeleteHandler() {
        this.commandService.post("/storage_delete", (request, response) -> {
            String content = request.body();
            PathRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "IllegalArgumentException: path invalid.");
            try {
                req = g.fromJson(content, PathRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            boolean res;
            try {
                res = deleteFile(req.path);
            } catch (IOException e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(res);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     * Helper function that helps recursivley delete a file or all the files and
     * directories in a given directory
     *
     * @param pathString the path to a given file/directory from the root.
     * @return boolean indicating if file was successfully deleted
     * @throws IOException if pathstring is invalid
     */
    public synchronized boolean deleteFile(String pathString) throws IOException {
        if (pathString == null) {
            throw new IOException("Path does not exist");
        }
        Path path = new Path(pathString);
        if (!path.isRoot()) {
            File fileName = path.toFile(this.root);
            if (fileName != null && fileName.exists()) {
                try {
                    if (fileName.isDirectory()) {
                        String[] directoryFiles = fileName.list();
                        for (String directoryFile : directoryFiles) {
                            boolean success = deleteFile(directoryFile);
                            if (!success)
                                return false;
                        }
                        fileName.delete();
                    } else
                        fileName.delete();
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }

            }
        }
        return false;
    }


    /**
     * Handler function to copy a file with the given path from a given storage
     * server onto this storage server. This function is used to replicate files
     * accross multiple storage servers
     */
    public void storageCopyHandler() {
        this.commandService.post("/storage_copy", (request, response) -> {
            String content = request.body();
            CopyRequest req;
            ExceptionReturn excepRet = new ExceptionReturn("IllegalArgumentException", "IllegalArgumentException: path invalid.");
            try {
                req = g.fromJson(content, CopyRequest.class);
            } catch (Exception e) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            if (req.path == null || req.server_ip == null) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }


            FileOutputStream fileWrite = null;
            boolean err = true;
            Path path = new Path(req.path);
            if (path.isRoot()) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }

            synchronized (this) {
                try {
                    File fileName = path.toFile(this.root);
                    if (fileName != null && !fileName.exists()) {
                        fileName.getParentFile().mkdirs();
                        fileName.createNewFile();
                    }

                    PathRequest pathRequest = new PathRequest(req.path);
                    HttpClient clientSize = HttpClient.newHttpClient();
                    HttpRequest sendSizeReq = HttpRequest.newBuilder()
                            .uri(URI.create("http://" + req.server_ip + ":" + req.server_port + "/storage_size"))
                            .POST(HttpRequest.BodyPublishers.ofString(g.toJson(pathRequest)))
                            .build();
                    HttpResponse<String> sizeResponse = clientSize.send(sendSizeReq, HttpResponse.BodyHandlers.ofString());
                    SizeReturn sizeReturn = g.fromJson(sizeResponse.body(), SizeReturn.class);

                    ReadRequest readRequest = new ReadRequest(req.path, 0, (int) sizeReturn.size);
                    HttpClient clientRead = HttpClient.newHttpClient();
                    HttpRequest sendReadReq = HttpRequest.newBuilder()
                            .uri(URI.create("http://" + req.server_ip + ":" + req.server_port + "/storage_read"))
                            .POST(HttpRequest.BodyPublishers.ofString(g.toJson(readRequest)))
                            .build();
                    HttpResponse<String> readResponse = clientRead.send(sendReadReq, HttpResponse.BodyHandlers.ofString());

                    DataReturn dataReturn = g.fromJson(readResponse.body(), DataReturn.class);
                    if (fileName != null && fileName.exists() && fileName.isFile()) {
                        byte[] byteData = dataReturn.data.getBytes();
//                        if (0L < fileSize) {
//                            fileWrite = new FileOutputStream(fileName);
//                            fileWrite.write(byteData, 0, byteData.length);
//                        } else {
                        fileWrite = new FileOutputStream(fileName, true);
                        fileWrite.write(byteData);
//                        }
                        err = false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    err = true;
                } finally {
                    if (fileWrite != null)
                        fileWrite.close();
//                    fileRead.close();
                }
            }
            if (err) {
                String ret = g.toJson(excepRet);
                response.status(404);
                response.type("application/json");
                return ret;
            }
            BooleanReturn booleanReturn = new BooleanReturn(true);
            String ret = g.toJson(booleanReturn);
            response.status(200);
            response.type("application/json");
            return ret;
        });
    }

    /**
     * Stop the command Spark service and client Spark service
     */
    public void stop() {
        clientService.stop();
        commandService.stop();
    }
}
