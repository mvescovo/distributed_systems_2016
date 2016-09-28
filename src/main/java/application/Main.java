package application;

import frontEndServer.ServerFrontEndImpl;
import replicationManagerServer.ReplicationManagerImpl;

/**
 * Main class to start the replicationManagerServer.
 *
 * @author michael
 */
public class Main {

    public static void main(String[] args) {

        new ReplicationManagerImpl(9318, "rm1");
        new ReplicationManagerImpl(9319, "rm2");
        new ReplicationManagerImpl(9320, "rm3");
        ServerFrontEndImpl.getInstance();
//      TrackingServiceImpl.getInstance();
    }
}
