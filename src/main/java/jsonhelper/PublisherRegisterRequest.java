package jsonhelper;

public class PublisherRegisterRequest {
    public String publisherID;

    public PublisherRegisterRequest(String pubID) {
        this.publisherID = pubID;
    }

    @Override
    public String toString() {
        String result = "PublisherRegisterRequest: " + this.publisherID;
        return result;
    }
}
