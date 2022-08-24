import Debug.PubSubException;
import Subscriber.Subscriber;
import jsonhelper.*;
import naming.NamingServer;
import naming.NamingServerInfo;
import naming.StorageServerInfo;
import publisher.Content;
import publisher.Publisher;
import storage.StorageServer;
import com.google.gson.Gson;

import org.junit.*;
import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ConformanceTest {

    /**
     * port number of naming server service
     */
    private int namingServerServicePort;
    /**
     * port number of naming server registration
     */
    private int namingServerRegistrationPort;
    /**
     * IP of naming server
     */
    private String namingServerIP;
    /**
     * naming server object
     */
    NamingServer namingServer;

    /**
     * port number of storage server 1 command
     */
    private int storageServer1CommandPort;
    /**
     * port number of storage server 1 client
     */
    private int storageServer1ClientPort;
    /**
     * IP of storage server
     */
    private String storageServer1IP;
    /**
     * root directory for storage server1
     */
    private String storageServer1Root;
    /**
     * storage server1 object
     */
    StorageServer storageServer1;

    /**
     * port number of storage server 2 command
     */
    private int storageServer2CommandPort;
    /**
     * port number of storage server 2 client
     */
    private int storageServer2ClientPort;
    /**
     * ip for storage server2
     */
    private String storageServer2IP;
    /**
     * root directory for storage server2
     */
    private String storageServer2Root;
    /**
     * storage server2 object
     */
    StorageServer storageServer2;

    /**
     * ID for publisher1
     */
    private String publisher1ID;
    /**
     * ID for publisher1
     */
    private String publisher2ID;
    /**
     * ID for subscriber1
     */
    private String subscriber1ID;
    /**
     * ID for subscriber2
     */
    private String subscriber2ID;

    /**
     * Gson object
     */
    private Gson g = new Gson();

    HttpResponse<String> response;
    String exceptionType;

    /**
     * Exception helper object
     */
    public PubSubException pubSubException;


    /**
     * initialize setup for unit test. All the naming server, storage servers use fix ip and port number, so it's a
     * one shot initialization that fix during run-time.
     */
    @Before
    public void setUup() throws IOException, InterruptedException {
        namingServerServicePort = 8080;
        namingServerRegistrationPort = 8090;
        namingServerIP = "127.0.0.1";

        storageServer1CommandPort = 7000;
        storageServer1ClientPort = 7001;
        storageServer1IP = "127.0.0.1";
        storageServer1Root = "/tmp/dist-system-1";

        storageServer2CommandPort = 7010;
        storageServer2ClientPort = 7011;
        storageServer2IP = "127.0.0.1";
        storageServer2Root = "/tmp/dist-system-2";

        publisher1ID = "publisher_1";
        publisher2ID = "publisher_2";

        subscriber1ID = "subscriber_1";
        subscriber2ID = "subscriber_2";
    }

    /**
     * This test is to verify that system can correctly assign the storage server to publisher.
     */
    @Test
    public void PubRegisterTest() throws IOException, InterruptedException {

        /*
            init system (1 naming server)
         */
        namingServer = new NamingServer(namingServerServicePort, namingServerRegistrationPort);
        namingServer.startService(g);
        namingServer.startRegistratioin(g);

        // check basic naming server functionality
        response = getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/list", new PathRequest("/"));
        assertArrayEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Naming Server directory not initialized empty!"),
                new String[0], g.fromJson(response.body(), FilesReturn.class).files);

        /*
            [Test] test publisher register before any available storage server exist
         */
        Publisher publisher1 = new Publisher(publisher1ID);
        response = publisher1.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Should throw exception when no available storage server exists for assignment!"),
                pubSubException.valueOf(exceptionType), pubSubException.IllegalStateException);

        /*
            [Test] after add storage server1 and publisher1, test register function
         */
        // add storage server1 and registered
        storageServer1 = new StorageServer(storageServer1ClientPort, storageServer1CommandPort, namingServerRegistrationPort, storageServer1Root);
        storageServer1.startClientService();
        storageServer1.startCommandService();
        response = getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                                    new RegisterRequest(storageServer1IP, storageServer1ClientPort, storageServer1CommandPort, new String[0]));

        // test publisher1 register with existed one storage server
        response = publisher1.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Exception should be null!"), exceptionType);

        StorageServerInfo assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        publisher1.updateAssignedStorageServer(assignedStorage);
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server client port unmatched!"),
                storageServer1ClientPort, assignedStorage.getClientPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server command port unmatched!"),
                storageServer1CommandPort, assignedStorage.getCommandPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned more than one publisher!"),
                assignedStorage.getPublisherMap().size(), 1);
        Object[] keys = assignedStorage.getPublisherMap().keySet().toArray();
        assertEquals(assignedStorage.getPublisherMap().get((String)keys[0]).getPublisherID(), publisher1ID);


        /*
            [Test] add storage server2 and publisher2, test register function
         */
        // add another server2 and registered
        storageServer2 = new StorageServer(storageServer2ClientPort, storageServer2CommandPort, namingServerRegistrationPort, storageServer2Root);
        storageServer2.startClientService();
        storageServer2.startCommandService();
        response = getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                                    new RegisterRequest(storageServer2IP, storageServer2ClientPort, storageServer2CommandPort, new String[0]));

        // test publisher2 register with another storage server (should register with storage server2)
        Publisher publisher2 = new Publisher(publisher2ID);
        response = publisher2.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Exception should be null!"), exceptionType);

        assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        publisher2.updateAssignedStorageServer(assignedStorage);
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server client port unmatched!"),
                storageServer2ClientPort, assignedStorage.getClientPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server command port unmatched!"),
                storageServer2CommandPort, assignedStorage.getCommandPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned more than one publisher!"),
                assignedStorage.getPublisherMap().size(), 1);
        keys = assignedStorage.getPublisherMap().keySet().toArray();
        assertEquals(assignedStorage.getPublisherMap().get((String)keys[0]).getPublisherID(), publisher2ID);

        /*
            [Test] test publihser1 repeatedly register
         */
        response = publisher1.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Publisher should throw IllegalArgumentException when repeatedly register"),
                pubSubException.valueOf(exceptionType), pubSubException.IllegalArgumentException);

        namingServer.stop();
        storageServer1.stop();
        storageServer2.stop();
    }

    /**
     * This test is to let naming server set file ready for publisher before publishing
     */
    @Test
    public void PubSetReadyTest() throws IOException, InterruptedException {
        /*
            init system (1 naming server)
         */
        namingServer = new NamingServer(namingServerServicePort, namingServerRegistrationPort);
        namingServer.startService(g);
        namingServer.startRegistratioin(g);

        /*
            init one storage server
        */
        storageServer1 = new StorageServer(storageServer1ClientPort, storageServer1CommandPort, namingServerRegistrationPort, storageServer1Root);
        storageServer1.startClientService();
        storageServer1.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                        new RegisterRequest(storageServer1IP, storageServer1ClientPort, storageServer1CommandPort, new String[0]));

        /*
            register one publisher, initialize the contents
        */
        Publisher publisher1 = new Publisher(publisher1ID);
        String txt = "Lakers won 2020 NBA Champ!";
        String[] keyWords = new String[] {"NBA", "Lakers", "Champ", "2020"};
        Content content = new Content(txt, keyWords);
        publisher1.addContent(content);
        publisher1.register(namingServerIP, namingServerServicePort);

        /*
            [Test] test create file ready on storage server **/
        publisher1.setReady(namingServerIP, namingServerServicePort);

        response = getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/list", new PathRequest("/"));
        String[] listing = g.fromJson(response.body(), FilesReturn.class).files;
        HashSet<String> keyWordsSet = new HashSet<>(Arrays.asList(keyWords));
        for(String keyWord: listing) {

            // check if all files have in created under naming server directory
            assertTrue(String.format("[Error](line:%s) %s", getCurrentLine(), "File is not created in directory"),
                    keyWordsSet.contains(keyWord));
            keyWordsSet.remove(keyWord);

            // check storage server info
            response = getHttpResponse("http://" + namingServerIP + ":" + namingServerServicePort + "/getstorage", new PathRequest("/" + keyWord));
            ServerInfo assignedStorage = g.fromJson(response.body(), ServerInfo.class);
            assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "File is not assigned to corresponding storage server!"),
                    storageServer1ClientPort, assignedStorage.server_port);

            // check if can read from storage not thorwing exception, whcih exactly verified file has been created there
            response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1ClientPort + "/storage_read",
                                        new ReadRequest("/" + keyWord, 0, 0));
            assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Read valid file should not throw exception!"),
                        g.fromJson(response.body(), ExceptionReturn.class).exceptionType);
        }

        // rest folder in storage server in order to let stress test pass
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/NBA"));
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/2020"));
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/Champ"));
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/Lakers"));

        namingServer.stop();
        storageServer1.stop();
    }

    /**
     * This test is to test if publisher can successfully publish content to storage server
     */
    @Test
    public void PubPublishTest() throws IOException, InterruptedException {
        /*
            init system (1 naming server)
         */
        namingServer = new NamingServer(namingServerServicePort, namingServerRegistrationPort);
        namingServer.startService(g);
        namingServer.startRegistratioin(g);

        /*
            init one storage server
        */
        storageServer1 = new StorageServer(storageServer1ClientPort, storageServer1CommandPort, namingServerRegistrationPort, storageServer1Root);
        storageServer1.startClientService();
        storageServer1.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                        new RegisterRequest(storageServer1IP, storageServer1ClientPort, storageServer1CommandPort, new String[0]));

        /*
            register one publisher, initialize the contents
        */
        Publisher publisher1 = new Publisher(publisher1ID);
        String txt = "I like NBA Lakers!";
        String[] keyWords = new String[] {"NBA", "Lakers"};
        Content content = new Content(txt, keyWords);
        publisher1.addContent(content);

        // if no assigned storage server
        assertFalse(String.format("[Error](line:%s) %s", getCurrentLine(), "Should not publish if no assigned storage server"),
                publisher1.publish());
        response = publisher1.register(namingServerIP, namingServerServicePort);
        StorageServerInfo assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        publisher1.updateAssignedStorageServer(assignedStorage);

        /*
            create file ready on storage server
        */
        publisher1.setReady(namingServerIP, namingServerServicePort);

        /*
            [Test] test publisher publish content to the assigned storage server by reading them back
        */
        long[] prevSize = new long[keyWords.length];
        for (int i = 0; i < keyWords.length; i++) {
            response = getHttpResponse("http://" + assignedStorage.getIP() + ":" + assignedStorage.getClientPort() + "/storage_size", new PathRequest("/" + keyWords[i]));
            prevSize[i] = new Gson().fromJson(response.body(), SizeReturn.class).size;
        }

        // publish with assigned storage server
        assertTrue(String.format("[Error](line:%s) %s", getCurrentLine(), "Should publish if assigned storage server"),
                    publisher1.publish());
        for (int i = 0; i < keyWords.length; i++) {
            response = getHttpResponse("http://" + assignedStorage.getIP() + ":" + assignedStorage.getClientPort() + "/storage_read",
                                        new ReadRequest("/" + keyWords[i], prevSize[i], txt.length()));
            String readTxt = g.fromJson(response.body(), DataReturn.class).data;
            assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Read data is different from the published one!"),
                        txt, readTxt);
        }

        // rest folder in storage server in order to let stress test pass
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/NBA"));
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/Lakers"));

        namingServer.stop();
        storageServer1.stop();
    }

    /**
     * This test is to verify that system can correctly assign the storage server to subscriber
     */
    @Test
    public void SubRegistrationTest() throws IOException, InterruptedException {
        /*
            init system (1 naming server)
         */
        namingServer = new NamingServer(namingServerServicePort, namingServerRegistrationPort);
        namingServer.startService(g);
        namingServer.startRegistratioin(g);

        /*
            [Test] test subscriber register before any available storage server exist
         */
        Subscriber subscriber1 = new Subscriber(subscriber1ID, new NamingServerInfo(namingServerIP, namingServerServicePort, namingServerRegistrationPort));
        response = subscriber1.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Should throw exception when no available storage server exists for assignment!"),
                pubSubException.valueOf(exceptionType), pubSubException.IllegalStateException);

        /*
            [Test] after add storage server1 and subscriber1, test register function
         */
        // add storage server1 and registered
        storageServer1 = new StorageServer(storageServer1ClientPort, storageServer1CommandPort, namingServerRegistrationPort, storageServer1Root);
        storageServer1.startClientService();
        storageServer1.startCommandService();
        response = getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer1IP, storageServer1ClientPort, storageServer1CommandPort, new String[0]));

        // test subscriber1 register with existed one storage server
        response = subscriber1.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Exception should be null!"),exceptionType, null);

        StorageServerInfo assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        subscriber1.updateAssignedStorageServer(assignedStorage);

        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server client port unmatched!"),
                storageServer1ClientPort, assignedStorage.getClientPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server command port unmatched!"),
                storageServer1CommandPort, assignedStorage.getCommandPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned more than one subscriber!"),
                assignedStorage.getSubscriberMap().size(), 1);
        Object[] keys = assignedStorage.getSubscriberMap().keySet().toArray();
        assertEquals(assignedStorage.getSubscriberMap().get((String)keys[0]).getSubscriberID(), subscriber1ID);

        /*
            [Test] add storage server2 and subscriber2, test register function
         */
        // add another server2 and registered
        storageServer2 = new StorageServer(storageServer2ClientPort, storageServer2CommandPort, namingServerRegistrationPort, storageServer2Root);
        storageServer2.startClientService();
        storageServer2.startCommandService();
        response = getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer2IP, storageServer2ClientPort, storageServer2CommandPort, new String[0]));

        // test publisher2 register with another storage server (should register with storage server2)
        Subscriber subscriber2 = new Subscriber(subscriber2ID, new NamingServerInfo(namingServerIP, namingServerServicePort, namingServerRegistrationPort));
        response = subscriber2.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Exception should be null!"), exceptionType);

        assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server client port unmatched!"),
                storageServer2ClientPort, assignedStorage.getClientPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned storage server command port unmatched!"),
                storageServer2CommandPort, assignedStorage.getCommandPort());
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Assigned more than one publisher!"),
                assignedStorage.getSubscriberMap().size(), 1);
        keys = assignedStorage.getSubscriberMap().keySet().toArray();
        assertEquals(assignedStorage.getSubscriberMap().get((String)keys[0]).getSubscriberID(), subscriber2ID);

        /*
            [Test] test subscriber1 repeatedly register
         */
        response = subscriber1.register(namingServerIP, namingServerServicePort);
        exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
        assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Subscriber should throw IllegalArgumentException when repeatedly register"),
                pubSubException.valueOf(exceptionType), pubSubException.IllegalArgumentException);

        namingServer.stop();
        storageServer1.stop();
        storageServer2.stop();
    }

    /**
     * This test is to let naming server set file ready (replicate if it exist in other storage server) for subscriber before reading content
     */
    @Test
    public void SubReplicationTest() throws IOException, InterruptedException {
        /*
            init system (1 naming server)
         */
        namingServer = new NamingServer(namingServerServicePort, namingServerRegistrationPort);
        namingServer.startService(g);
        namingServer.startRegistratioin(g);

        /*
            init 1st storage server
        */
        storageServer1 = new StorageServer(storageServer1ClientPort, storageServer1CommandPort, namingServerRegistrationPort, storageServer1Root);
        storageServer1.startClientService();
        storageServer1.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer1IP, storageServer1ClientPort, storageServer1CommandPort, new String[0]));

        /*
            init publisher1 (dummmy) which assigned to storage server1
        */
        Publisher publisher1 = new Publisher(publisher1ID);
        response = publisher1.register(namingServerIP, namingServerServicePort);

        /*
            init subscriber1 which assigned to storage server1
        */
        Subscriber subscriber1 = new Subscriber(subscriber1ID, new String[]{"NBA", "Lakers"}, new NamingServerInfo(namingServerIP, namingServerServicePort, namingServerRegistrationPort));
        response = subscriber1.register(namingServerIP, namingServerServicePort);
        StorageServerInfo assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        subscriber1.updateAssignedStorageServer(assignedStorage);

        /*
            init 2nd storage server
        */
        storageServer2 = new StorageServer(storageServer2ClientPort, storageServer2CommandPort, namingServerRegistrationPort, storageServer2Root);
        storageServer2.startClientService();
        storageServer2.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer2IP, storageServer2ClientPort, storageServer2CommandPort, new String[0]));

        /*
            init publisher2 (content) which assigned to storage server2
        */
        Publisher publisher2 = new Publisher(publisher2ID);
        String txt = "I like NBA Warriors!";
        String[] keyWords = new String[] {"NBA", "Warriors"};
        Content content = new Content(txt, keyWords);
        publisher2.addContent(content);
        response = publisher2.register(namingServerIP, namingServerServicePort);
        assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        publisher2.updateAssignedStorageServer(assignedStorage);
        publisher2.setReady(namingServerIP, namingServerServicePort);
        publisher2.publish();

        /*
            [Test] test replication when subscriber1 want to read content from storage server1 but only exist on storage server2
        */
        assertTrue(String.format("[Error](line:%s) %s", getCurrentLine(), "Should successfully replicate without exception"),
                subscriber1.setReady(namingServerIP, namingServerServicePort));
        // check if can read from storage not throwing exception, which exactly verified file has been created there
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1ClientPort + "/storage_read",
                new ReadRequest("/NBA", 0, 0));
        assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Read valid file should not throw exception!"),
                    g.fromJson(response.body(), ExceptionReturn.class).exceptionType);

        // rest folder in storage server in order to let stress test pass
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/NBA"));
        response = getHttpResponse("http://" + storageServer2IP + ":" + storageServer2CommandPort + "/storage_delete", new PathRequest("/NBA"));
        response = getHttpResponse("http://" + storageServer2IP + ":" + storageServer2CommandPort + "/storage_delete", new PathRequest("/Warriors"));

        namingServer.stop();
        storageServer1.stop();
        storageServer2.stop();
    }

    /**
        This test is to verify subscriber can read content from assigned server if it has subscribed contents
    */
    @Test
    public void SubReadTest() throws IOException, InterruptedException {
        /*
            init system (1 naming server)
         */
        namingServer = new NamingServer(namingServerServicePort, namingServerRegistrationPort);
        namingServer.startService(g);
        namingServer.startRegistratioin(g);


        /*
            init 1st storage server
        */
        storageServer1 = new StorageServer(storageServer1ClientPort, storageServer1CommandPort, namingServerRegistrationPort, storageServer1Root);
        storageServer1.startClientService();
        storageServer1.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer1IP, storageServer1ClientPort, storageServer1CommandPort, new String[0]));

        /*
            init publisher1 (dummmy) which assigned to storage server1
        */
        Publisher publisher1 = new Publisher(publisher1ID);
        response = publisher1.register(namingServerIP, namingServerServicePort);

        /*
            init subscriber1 which assigned to storage server1
        */
        Subscriber subscriber1 = new Subscriber(subscriber1ID, new String[]{"NBA", "Lakers"}, new NamingServerInfo(namingServerIP, namingServerServicePort, namingServerRegistrationPort));
        response = subscriber1.register(namingServerIP, namingServerServicePort);
        StorageServerInfo assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        subscriber1.updateAssignedStorageServer(assignedStorage);


        /*
            init 2nd storage server
        */
        storageServer2 = new StorageServer(storageServer2ClientPort, storageServer2CommandPort, namingServerRegistrationPort, storageServer2Root);
        storageServer2.startClientService();
        storageServer2.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer2IP, storageServer2ClientPort, storageServer2CommandPort, new String[0]));

        /*
            init publisher2 (content) which assigned to storage server2
        */
        Publisher publisher2 = new Publisher(publisher2ID);
        String txt = "I like NBA Warriors!";
        String[] keyWords = new String[] {"NBA", "Warriors"};
        Content content = new Content(txt, keyWords);
        publisher2.addContent(content);
        response = publisher2.register(namingServerIP, namingServerServicePort);
        assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        publisher2.updateAssignedStorageServer(assignedStorage);
        publisher2.setReady(namingServerIP, namingServerServicePort);
        publisher2.publish();

        /*
            Before read, do set ready (replication if needed)
        */
        subscriber1.setReady(namingServerIP, namingServerServicePort);

        /*
            [Test] test if subscriber can read back the data correctly
        */
        HashMap<String, String> res= subscriber1.readContent();
        for (Map.Entry<String, String> set : res.entrySet()) {
            if (set.getKey().equals("Lakers")) {
                assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Text should be null since it's not existed in system!"), set.getValue());
            }else if (set.getKey().equals("NBA")) {
                assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Text read unmatched with the system possessed!"),
                            set.getValue(), txt);
            }
        }

        // rest folder in storage server in order to let stress test pass
        response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1CommandPort + "/storage_delete", new PathRequest("/NBA"));
        response = getHttpResponse("http://" + storageServer2IP + ":" + storageServer2CommandPort + "/storage_delete", new PathRequest("/NBA"));
        response = getHttpResponse("http://" + storageServer2IP + ":" + storageServer2CommandPort + "/storage_delete", new PathRequest("/Warriors"));

        namingServer.stop();
        storageServer1.stop();
        storageServer2.stop();
    }

    /**
     This test is test system will automatically delete content if it has been read by all subscribers
     */
    @Test
    public void SubDeletionTest() throws IOException, InterruptedException {
       /*
            init system (1 naming server)
         */
        namingServer = new NamingServer(namingServerServicePort, namingServerRegistrationPort);
        namingServer.startService(g);
        namingServer.startRegistratioin(g);


        /*
            init 1st storage server
        */
        storageServer1 = new StorageServer(storageServer1ClientPort, storageServer1CommandPort, namingServerRegistrationPort, storageServer1Root);
        storageServer1.startClientService();
        storageServer1.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer1IP, storageServer1ClientPort, storageServer1CommandPort, new String[0]));

        /*
            init publisher1 (dummmy) which assigned to storage server1
        */
        Publisher publisher1 = new Publisher(publisher1ID);
        response = publisher1.register(namingServerIP, namingServerServicePort);

        /*
            init subscriber1 which assigned to storage server1
        */
        Subscriber subscriber1 = new Subscriber(subscriber1ID, new String[]{"NBA", "Lakers"},
                                                new NamingServerInfo(namingServerIP, namingServerServicePort, namingServerRegistrationPort));
        response = subscriber1.register(namingServerIP, namingServerServicePort);
        StorageServerInfo assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        subscriber1.updateAssignedStorageServer(assignedStorage);

        /*
            init 2nd storage server
        */
        storageServer2 = new StorageServer(storageServer2ClientPort, storageServer2CommandPort, namingServerRegistrationPort, storageServer2Root);
        storageServer2.startClientService();
        storageServer2.startCommandService();
        getHttpResponse("http://" + namingServerIP + ":" + namingServerRegistrationPort + "/register",
                new RegisterRequest(storageServer2IP, storageServer2ClientPort, storageServer2CommandPort, new String[0]));

        /*
            init subscriber2 which assigned to storage server1
        */
        Subscriber subscriber2 = new Subscriber(subscriber2ID, new String[]{"NBA", "Warriors"},
                                                new NamingServerInfo(namingServerIP, namingServerServicePort, namingServerRegistrationPort));
        response = subscriber2.register(namingServerIP, namingServerServicePort);
        assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        subscriber2 .updateAssignedStorageServer(assignedStorage);

        /*
            init publisher2 (content) which assigned to storage server2
        */
        Publisher publisher2 = new Publisher(publisher2ID);
        String txt = "I like NBA Warriors!";
        String[] keyWords = new String[] {"NBA", "Warriors"};
        Content content = new Content(txt, keyWords);
        publisher2.addContent(content);
        response = publisher2.register(namingServerIP, namingServerServicePort);
        assignedStorage = g.fromJson(response.body(), StorageServerInfo.class);
        publisher2.updateAssignedStorageServer(assignedStorage);
        publisher2.setReady(namingServerIP, namingServerServicePort);
        publisher2.publish();

        /*
            Before read, do set ready (replciation if needed)
        */
        subscriber1.setReady(namingServerIP, namingServerServicePort);
        subscriber2.setReady(namingServerIP, namingServerServicePort);

        /*
            [Test] test if all subscriber read the content, will the subscribed content be deleted
        */
        // after subscriber1 read, all files should remains
        subscriber1.readContent();
        for (String keyWord : keyWords) {
            if (keyWord.equals("Warriors")) {
                response = getHttpResponse("http://" + storageServer2IP + ":" + storageServer2ClientPort + "/storage_read",
                        new ReadRequest("/" + keyWord, 0, 0));
                assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Read valid file should not throw exception!"),
                            g.fromJson(response.body(), ExceptionReturn.class).exceptionType);
                continue;
            }
            response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1ClientPort + "/storage_read",
                    new ReadRequest("/" + keyWord, 0, 0));
            assertNull(String.format("[Error](line:%s) %s", getCurrentLine(), "Read valid file should not throw exception!"),
                    g.fromJson(response.body(), ExceptionReturn.class).exceptionType);
        }

        // after subscriber2 read, all files should be deleted
        subscriber2.readContent();
        for (String keyWord : keyWords) {
            response = getHttpResponse("http://" + storageServer2IP + ":" + storageServer2ClientPort + "/storage_read",
                    new ReadRequest("/" + keyWord, 0, 0));
            exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
            assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Read valid file should not throw exception!"),
                    pubSubException.valueOf(exceptionType), PubSubException.FileNotFoundException);

            response = getHttpResponse("http://" + storageServer1IP + ":" + storageServer1ClientPort + "/storage_read",
                    new ReadRequest("/" + keyWord, 0, 0));
            exceptionType = g.fromJson(response.body(), ExceptionReturn.class).exceptionType;
            assertEquals(String.format("[Error](line:%s) %s", getCurrentLine(), "Read valid file should not throw exception!"),
                    pubSubException.valueOf(exceptionType), PubSubException.FileNotFoundException);
        }

        namingServer.stop();
        storageServer1.stop();
        storageServer2.stop();
    }


    /**
     *  Get current line number for debugging
     * @return Integer of line number
     */
    int getCurrentLine() {
        return Thread.currentThread().getStackTrace()[2].getLineNumber();
    }

    /**
     * Generate HTTP request to send to other client/server
     *
     * @param uriStr    the uri string
     * @param reqObj    Request object
     */
    HttpResponse<String> getHttpResponse(String uriStr, Object reqObj) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest sendReq = HttpRequest.newBuilder()
                .uri(URI.create(uriStr))
                .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(reqObj)))
                .build();
        return client.send(sendReq, HttpResponse.BodyHandlers.ofString());
    }
}