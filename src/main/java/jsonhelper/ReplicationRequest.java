package jsonhelper;

public class ReplicationRequest {
    public String path;
    public String existed_ip;
    public int existed_client_port;

    public String copy_ip;
    public int copy_client_port;
    public int copy_command_port;


    public ReplicationRequest(String path, String ext_ip, int ext_client_port, String copy_ip, int copy_client_port, int copy_command_port) {
        this.path = path;
        this.existed_ip = ext_ip;
        this.existed_client_port = ext_client_port;

        this.copy_ip = copy_ip;
        this.copy_client_port = copy_client_port;
        this.copy_command_port = copy_command_port;
    }
}
