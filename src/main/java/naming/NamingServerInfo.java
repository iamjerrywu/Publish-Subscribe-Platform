package naming;

/**Authors : Sheng-Hao Wu, Kevin Li */

/**
 * Naming Server Info -- object that storing the information of the system naming server
 */

public class NamingServerInfo {
    /** 
     * IP address of naming server
     */
    public String ip;
    /**
     * Integer value of service port
     */
    public int servicePort;
    /**
     * Integer value of command port
     */
    public int registrationPort;

    /**
     * Constructor to initialize information about naming server
     *
     * @param ip               ip address of naming server
     * @param servicePort      service port of naming server
     * @param registrationPort registration port of naming server
     */
    public NamingServerInfo(String ip, int servicePort, int registrationPort) {
        this.ip = ip;
        this.servicePort = servicePort;
        this.registrationPort = registrationPort;
    }
}
