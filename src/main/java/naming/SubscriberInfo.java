package naming;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * Subscriber Info - object that storing the information of the subscriber
 */

public class SubscriberInfo {

    /**
     * ID of subscriber
     */
    private String subscriberID;

    /**
     * Constructor to initialize information about subscriber for naming server to use
     *
     * @param pubID id of subscriber
     */
    public SubscriberInfo(String pubID) {
        this.subscriberID = pubID;
    }

    /**
     * Function to get the ID of the subscriber
     * @return id of subscriber
     */
    public String getSubscriberID() {
        return this.subscriberID;
    }
}
