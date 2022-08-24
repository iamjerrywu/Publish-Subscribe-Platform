package naming;

/**Authors : Sheng-Hao Wu, Kevin Li */

import java.util.HashMap;

/**
 * StorageServerInfo -- Object that stores the storage information, including
 * IP address, client port number and command port number
 */

public class StorageServerInfo {
    /**
     * String of the server IP address
     */
    private String ip;
    /**
     * Integer of the client port number
     */
    private int clientPort;
    /**
     * Integer of the command port number
     */
    private int commandPort;

    /**
     * HashSet of the assigned Publisher
     */
    private HashMap<String, PublisherInfo> assignedPublisherMap;

    /**
     * HashSet of the assigned Subscriber
     */
    private HashMap<String, SubscriberInfo> assignedSubscriberMap;

    /**
     * Constructor for StorageServerInfo
     *
     * @param ipAddr      IP address
     * @param clientPort  client port number
     */
    public StorageServerInfo( String ipAddr, int clientPort) {
        this.ip = ipAddr;
        this.clientPort = clientPort;
        this.assignedPublisherMap = new HashMap<>();
        this.assignedSubscriberMap = new HashMap<>();
    }
    /**
     * Constructor for StorageServerInfo
     *
     * @param ipAddr      IP address
     * @param clientPort  client port number
     * @param commandPort command port number
     */
    public StorageServerInfo( String ipAddr, int clientPort, int commandPort) {
        this.ip = ipAddr;
        this.clientPort = clientPort;
        this.commandPort = commandPort;
        this.assignedPublisherMap = new HashMap<>();
        this.assignedSubscriberMap = new HashMap<>();
    }

    /**
     * Get the IP address of the server
     *
     * @return String of the server IP address
     */
    public String getIP() {return this.ip;}
    /**
     * Get the client port number of the server
     *
     * @return number of the client port
     */
    public int getClientPort() {return this.clientPort;}
    /**
     * Get the command port number of the server
     *
     * @return number of the command port
     */
    public int getCommandPort(){return this.commandPort;}

    /**
     * Get publisher map
     *
     * @return hashmap of the publisher mapping
     */
    public HashMap<String, PublisherInfo> getPublisherMap() {
        return assignedPublisherMap;
    }

    /**
     * Get subscriber map
     *
     * @return hashmap of the subscriber mapping
     */
    public HashMap<String, SubscriberInfo> getSubscriberMap() {
        return assignedSubscriberMap;
    }

    /**
     * Add assigned publisher to publisher map
     */
    public void addAssignedPublisher(String publisherID, PublisherInfo publisherInfo){
        this.assignedPublisherMap.put(publisherID, publisherInfo);
    }

    /**
     * Remove assigned publisher from publisher map
     */
    public void removeAssignedPublisher(String publisherID, PublisherInfo publisherInfo){
        this.assignedPublisherMap.remove(publisherID, publisherInfo);
    }

    /**
     * Add assigned subscriberr to publisher map
     */
    public void addAssignedSubscriber(String subscriberID, SubscriberInfo subscriberInfo){
        this.assignedSubscriberMap.put(subscriberID, subscriberInfo);
    }

    /**
     * Remove assigned publisher from publisher map
     */
    public void removeAssignedSubscriber(String subscriberID, SubscriberInfo subscriberInfo){
        this.assignedSubscriberMap.remove(subscriberID, subscriberInfo);
    }

    /**
     * toString method to present contents
     *
     * @return String representation of contents
     */
    @Override
    public String toString() {
        return "StorageServerInfo: " + "server_ip = <" + ip + "> client_port = <" +
                clientPort + "> command_port = <" + commandPort +">";
    }
}
