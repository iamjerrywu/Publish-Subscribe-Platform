package jsonhelper;

public class SubscriberRegisterRequest {
    public String subscriberID;
    public String[] keyWords;

    public SubscriberRegisterRequest(String subID, String[] keyWords) {
        this.subscriberID = subID;
        this.keyWords = keyWords;
    }

    @Override
    public String toString() {
        String result = "SubscriberRegisterRequest: " + this.subscriberID;
        return result;
    }
}
