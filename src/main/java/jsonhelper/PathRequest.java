package jsonhelper;

public class PathRequest {
    public String path;
    public String publisherID;

    public PathRequest(String path) {
        this.path = path;
        this.publisherID = null;
    }

    public PathRequest(String path, String publisherID) {
        this.path = path;
        this.publisherID = publisherID;
    }
}
