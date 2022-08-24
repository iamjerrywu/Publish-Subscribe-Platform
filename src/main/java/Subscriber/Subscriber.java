package Subscriber;

import com.google.gson.Gson;
import jsonhelper.*;
import naming.NamingServerInfo;
import naming.StorageServerInfo;
import publisher.Content;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

/**Authors : Sheng-Hao Wu, Kevin Li */
/**
 * /**
 *  * Subscriber -- publisher is a kind of client that will publish content to the assigned storage server.
 *  * It mainly three functions,
 *  * 1. register() : to register to the system and to be assigned a dedicated storage server
 *  * 2. setReady() : to set the system ready for publish, it means that corresponded files and directory should be prepared
 *                    in both storage server and naming server. The replicatioin algorithm will be run here if certain
 *                    file is not existed in the assigned server but on others.
 *  * 3. readContent() : Read the content according the the keyword preference. Here the deletion algorithm will be run
 *                       that if the content has been read by all valid subscriber, then the files will be deleted in the
 *                       ssystem.
 *  */
public class Subscriber {
    /** 
     * id of subscriber 
     */
    private String subscriberID;
    /** 
     * HTTPClient used to make http POST requests 
     */
    private HttpClient client;
    /** 
     * latest HTTPResponse from subscriber's POST requests
     */
    private HttpResponse<String> response;
    /** 
     * keywords connected to subscriber 
     */
    private String[] keyWords;
    /** 
     * info about the storage server assigned to the subscriber  
     */
    private StorageServerInfo assignedStorageServer;
    /** 
     * info about the naming server that the subscriber communicates with
     */
    private NamingServerInfo namingServerInfo;

    /**
     * Constructor to initialize subscriber
     *
     * @param subID id of subscriber
     * @param namingServerInfo info about naming server
     */
    public Subscriber(String subID, NamingServerInfo namingServerInfo) {
        this.subscriberID = subID;
        this.client = HttpClient.newHttpClient();
        this.keyWords = new String[0];
        this.namingServerInfo = namingServerInfo;
    }

    /**
     * Constructor to initialize subscriber for certain keywords
     *
     * @param subID id of subscriber
     * @param keyWords keywords tied to the subscriber
     * @param namingServerInfo info about naming server
     */
    public Subscriber(String subID, String[] keyWords, NamingServerInfo namingServerInfo) {
        this.subscriberID = subID;
        this.client = HttpClient.newHttpClient();
        this.keyWords = keyWords;
        this.namingServerInfo = namingServerInfo;
    }

    /**
     * Function to register a subscriber. Relies upon <code>getHttpResponse</code> to perform HTTP POST request.
     *
     * @param namingServerIP ip address of naming server
     * @param namingServerServicePort  service port number of naming server
     * @return the HTTPResponse of registering a subscriber
     * @throws IOException 
     * @throws InterruptedException
     */
    public HttpResponse<String> register(String namingServerIP, int namingServerServicePort) throws IOException, InterruptedException {
        return getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/subscriber_register",
                                new SubscriberRegisterRequest(this.subscriberID, keyWords));
    }

    /**
     * Function to check whether a subscriber is ready to read content from connected storage server. Relies upon <code>getHttpResponse</code> to perform HTTP POST requests.
     *
     * @param namingServerIP ip address of naming server
     * @param namingServerServicePort  service port number of naming server
     * @return boolean to indicate if subscriber is ready
     * @throws IOException 
     * @throws InterruptedException
     */
    public Boolean setReady(String namingServerIP, int namingServerServicePort) throws IOException, InterruptedException {
        for (String keyWord : this.keyWords) {
            response = getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/getstorage", new PathRequest("/" + keyWord));
            if (new Gson().fromJson(response.body(), ExceptionReturn.class).exceptionType != null) continue;
            ServerInfo existedServer = new Gson().fromJson(response.body(), ServerInfo.class);
            if (assignedStorageServer.getClientPort() != existedServer.server_port) {
                // replication
                response = getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/replication",
                                new ReplicationRequest("/" + keyWord, existedServer.server_ip, existedServer.server_port,
                                        assignedStorageServer.getIP(), assignedStorageServer.getClientPort(), assignedStorageServer.getCommandPort()));
                if (new Gson().fromJson(response.body(), BooleanReturn.class).success != true) return false;
            }
        }
        return true;
    }

    /**
     * Function to read the content from storage server connected to subscriber. Relies upon <code>getHttpResponse</code> to perform HTTP POST requests.
     *
     * @return hashmap that contains the keyword and text sent by publishers
     * @throws IOException 
     * @throws InterruptedException
     */
    public HashMap<String, String> readContent() throws IOException, InterruptedException {
        HashMap<String, String> res = new HashMap<>();
        for (String keyWord : keyWords) {
            // lock
            response = getHttpResponse("http://" + namingServerInfo.ip + ":" + namingServerInfo.servicePort + "/lock",
                    new LockRequest("/" + keyWord,  false, subscriberID));

            // first get the size
            response = getHttpResponse("http://" + assignedStorageServer.getIP() + ":" + assignedStorageServer.getClientPort() + "/storage_size", new PathRequest("/" + keyWord));

            int size = (int)new Gson().fromJson(response.body(), SizeReturn.class).size;
            // read the content
            response = getHttpResponse("http://" + assignedStorageServer.getIP() + ":" + assignedStorageServer.getClientPort() + "/storage_read",
                    new ReadRequest("/" + keyWord, 0, size));
            if(new Gson().fromJson(response.body(), ExceptionReturn.class).exceptionType != null) {
                res.put(keyWord, null);
                continue;
            }
            String readTxt = new Gson().fromJson(response.body(), DataReturn.class).data;
            res.put(keyWord, readTxt);

            // unlock
            response = getHttpResponse("http://" + namingServerInfo.ip + ":" + namingServerInfo.servicePort + "/unlock",
                    new LockRequest("/" + keyWord,  false, subscriberID));
        }
        return res;
    }

    /**
     * Function to update the storage server assigned to the subscriber.
     *
     * @param storageServerInfo info about the new storage server subscriber is connected to
     */
    public void updateAssignedStorageServer(StorageServerInfo storageServerInfo) {
        this.assignedStorageServer = storageServerInfo;
    }

    /**
     * Function to get HTTP Response from POST request with the provided uri and request object.
     *
     * @param uriStr the uri of the POST request
     * @param reqObj the request object being sent by the POST request
     * @return httpresponse from POST request
     * @throws IOException 
     * @throws InterruptedException
     */
    HttpResponse<String> getHttpResponse(String uriStr, Object reqObj) throws IOException, InterruptedException {
        HttpRequest sendReq = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(reqObj)))
                .build();
        return client.send(sendReq, HttpResponse.BodyHandlers.ofString());
    }

}
