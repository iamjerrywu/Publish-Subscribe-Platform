package jsonhelper;

public class LockRequest {
    public String path;
    public boolean exclusive;
    public String subscriberID;

    public LockRequest(String path, boolean exclusive) {
        this.path = path;
        this.exclusive = exclusive;

    }
    public LockRequest(String path, boolean exclusive, String subID) {
        this.path = path;
        this.exclusive = exclusive;
        this.subscriberID = subID;

    }
}
