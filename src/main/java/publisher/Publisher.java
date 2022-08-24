package publisher;

import com.google.gson.Gson;
import jsonhelper.*;
import naming.StorageServerInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedList;
import java.util.Queue;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * Publisher -- publisher is a kind of client that will publish content to the assigned storage server.
 * It mainly three functions,
 * 1. register() : to register to the system and to be assigned a dedicated storage server
 * 2. setReady() : to set the system ready for publish, it means that corresponded files and directory should be prepared in both storage server and naming server
 * 3. publish() : write text to the file that has been created on the storage server
 */

public class Publisher {
    /** 
     * id of publisher 
     */
    private String publisherID;
    /** 
     * queue of content
     */
    private Queue<Content> contentQueue;
    /** 
     * HTTPClient to make HTTP POST requests
     */
    private HttpClient client;
    /** 
     * latest HTTPResponse from HTTP POST request
     */
    private HttpResponse<String> response;
    /** 
     * info about storage server associated with publisher
     */
    private StorageServerInfo assignedStorageServer;

    /** 
     * constructor to initialize publisher
     * @param pubID id of publisher
     */
    public Publisher(String pubID) {
        publisherID = pubID;
        this.contentQueue = new LinkedList<>();
        client = HttpClient.newHttpClient();
    }

    /** 
     * constructor to initialize publisher
     * @param pubID id of publisher
     * @param cq initial queue of content
     */
    public Publisher(String pubID, Queue<Content> cq) {
        publisherID = pubID;
        this.contentQueue = cq;
        client = HttpClient.newHttpClient();
    }

    /**
     * Function to register a publisher. Relies upon <code>getHttpResponse</code> to perform HTTP POST request.
     *
     * @param namingServerIP ip address of naming server
     * @param namingServerServicePort  service port number of naming server
     * @return the HTTPResponse of registering a publisher
     * @throws IOException 
     * @throws InterruptedException
     */
    public HttpResponse<String> register(String namingServerIP, int namingServerServicePort) throws IOException, InterruptedException {
        return getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/publisher_register",
                                new PublisherRegisterRequest(this.publisherID));
    }

    /**
     * Function to make sure the publsiher is ready to sent content to connected storage server, which has the file prepared.
     * Relies upon <code>getHttpResponse</code> to perform HTTP POST requests.
     *
     * @param namingServerIP ip address of naming server
     * @param namingServerServicePort  service port number of naming server
     * @throws IOException 
     * @throws InterruptedException
     */
    public void setReady(String namingServerIP, int namingServerServicePort) throws IOException, InterruptedException {
        Object[] objArray = this.contentQueue.toArray();

        for (Object obj : objArray) {
            Content content= (Content)obj;
            for (String keyWord : content.getKeyWords()) {
                getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/create_file",
                        new PathRequest("/" + keyWord, getPublisherID()));
            }
        }
    }

    /**
     * Function to publish a content file to connected storage server. Relies upon <code>getHttpResponse</code> to perform HTTP POST requests.
     *
     * @return boolean to indicate whether the publisher was successful in sending content file to assigned storage server.
     * @throws IOException 
     * @throws InterruptedException
     */
    public Boolean publish() throws IOException, InterruptedException {
        if (assignedStorageServer == null) return false;
        while (!contentQueue.isEmpty()) {
            Content content = getContent();
            for (String keyWord : content.getKeyWords()) {
                // get the size first
                response = getHttpResponse("http://" + assignedStorageServer.getIP() + ":" + assignedStorageServer.getClientPort() + "/storage_size",
                        new PathRequest("/" + keyWord));
                long size = new Gson().fromJson(response.body(), SizeReturn.class).size;
                // write the content
                getHttpResponse("http://" + assignedStorageServer.getIP() + ":" + assignedStorageServer.getClientPort() + "/storage_write",
                        new WriteRequest("/" + keyWord, size, content.getText()));
            }
        }
        return true;
    }

    /**
     * Function to get the id of the publisher
     * 
     * @return id of publisher
     */
    public String getPublisherID() {
        return publisherID;
    }

    /**
     * Function to add content file to queue for publisher to send to storaged server connected to publisher
     * 
     * @param c content file to be added
     */
    public void addContent(Content c) {
        contentQueue.add(c);
    }

    /**
     * Function to get first contetn file from content queue
     * 
     * @return first content file in content queue
     */
    public Content getContent() {
        if (this.contentQueue.isEmpty())
            return null;
        Content content = this.contentQueue.peek();
        this.contentQueue.remove();
        return content;
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
