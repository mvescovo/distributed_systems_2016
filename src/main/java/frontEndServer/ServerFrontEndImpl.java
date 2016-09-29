package frontEndServer;

import data.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import replicationManagerServer.TrackingService;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static replicationManagerServer.ReplicationManagerImpl.*;

/**
 * @author michael.
 */
public class ServerFrontEndImpl implements ServerFrontEnd {

    private static final Logger logger = LogManager.getLogger(ServerFrontEndImpl.class);
    private volatile static ServerFrontEndImpl uniqueInstance;
    private static Map<String, Integer> mReplicationManagers = new HashMap<>();
    private static Map<String, Boolean> mConnected = new HashMap<>();
    private static Map<String, TrackingService> mTrackingServices = new HashMap<>();
    private boolean rmiSetup = false;

    static {
        mReplicationManagers.put("rm1", 9318);
        mReplicationManagers.put("rm2", 9319);
        mReplicationManagers.put("rm3", 9320);
        mConnected.put("rm1", false);
        mConnected.put("rm2", false);
        mConnected.put("rm3", false);
    }

    private int mTramId;
    private int mRoute;
    private volatile long RPCId;

    private ServerFrontEndImpl() {
        RPCId = 0;
        connectServerFrontEndWithReplicationManagers();
    }

    /*
    * Front end is a singleton. Use this to get an instance.
    * */
    public static ServerFrontEndImpl getInstance() {
        if (uniqueInstance == null) {
            synchronized (ServerFrontEndImpl.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new ServerFrontEndImpl();
                }
            }
        }
        return uniqueInstance;
    }

    /*
    * Main method to run the front end server.
    * */
    public static void main(String args[]) {
        getInstance();
    }

    /*
     * Configure RMI on the Frontend to accept clients.
     * */
    private void setupRMI(ServerFrontEnd serverFrontEnd) {
        int frontEndPort = 9317;
        String host = "localhost";
        String url = "rmi://" + host + "/serverFrontEnd/";

        try {
            ServerFrontEnd frontEnd = (ServerFrontEnd) UnicastRemoteObject.exportObject(serverFrontEnd, 0);
            Registry registry = LocateRegistry.createRegistry(frontEndPort);
            registry.bind(url, frontEnd);
            logger.info("Server front end bound to: " + url);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    /*
     * Connect to replication manager servers.
     * */
    private void connectServerFrontEndWithReplicationManagers() {
        for (String rm : mReplicationManagers.keySet()) {
            connectToRm(rm, connected -> {
                if (!rmiSetup) {
                    setupRMI(this);
                    rmiSetup = true;
                    System.out.println("Created frontend Server, accepting connections...");
                }
            });
        }
    }

    /*
    * Provide a list of available tracking services.
    * */
    @Override
    public List<TrackingService> listTramService() {
        List<TrackingService> connectedRMs = new ArrayList<>();
        for (String rm : mTrackingServices.keySet()) {
            try {
                Message message = mTrackingServices.get(rm).retrieveNextStop(null);
                if (message != null) {
                    // RM is connected.
                    connectedRMs.add(mTrackingServices.get(rm));
                }
            } catch (RemoteException e) {
                // RM is not connected.
                mConnected.put(rm, false);
                // Reconnect RM
                connectToRm(rm, connected -> {
                    // Reconnected.
                });
            }
        }
        return connectedRMs;
    }

    /*
    * Connect to the specified replication manager.
    * */
    private void connectToRm(String rm, ConnectedToRmCallback callback) {
        new Thread(() -> {
            while (!mConnected.get(rm)) {
                int port = mReplicationManagers.get(rm);
                String host = "localhost";
                String url = "rmi://" + host + "/" + rm + "/";

                try {
                    Registry registry = LocateRegistry.getRegistry("localhost", port);
                    TrackingService trackingService = (TrackingService) registry.lookup(url);
                    mTrackingServices.put(rm, trackingService);
                    mConnected.put(rm, true);
                } catch (NotBoundException | IOException e) {
                    // Could not connect to RM.
                }
            }
            callback.onConnected(true);
        }).start();
    }

    /*
    * Print the status of all replication manager servers.
    * */
    @Override
    public void printTrackingServiceAvailability() {
        for (String rm : mConnected.keySet()) {
            String status;
            if (mConnected.get(rm)) {
                status = "on";
            } else {
                status = "off";
            }
            System.out.println(rm.toUpperCase() + " is " + status);
        }
    }

    /*
    * Retrieve the next stop from available RM's and pass back to the client.
    * */
    @Override
    public Message retrieveNextStop(Message message) throws RemoteException {
        List<TrackingService> availableTrackingServices = listTramService();
        printTrackingServiceAvailability();

        Message messageReply = new Message();
        for (TrackingService trackingService : availableTrackingServices) {
            messageReply = trackingService.retrieveNextStop(message);
        }
        return messageReply;
    }

    /*
    * Update tram location on all available RM's.
    * */
    @Override
    public Message updateTramLocation(Message message) throws RemoteException {
        List<TrackingService> availableTrackingServices = listTramService();
        printTrackingServiceAvailability();

        Message messageReply = new Message();
        for (TrackingService trackingService : availableTrackingServices) {
            messageReply = trackingService.updateTramLocation(message);
        }
        return messageReply;
    }

    @Override
    public int getTramId() throws RemoteException {
        for (int i = 0; i < tramIds.size(); i++) {
            if (!tramIds.get(i)) {
                tramIds.put(i, true);
                return i;
            }
        }
        // no id's available.
        return -1;
    }

    @Override
    public int getRoute(int tramId) throws RemoteException {
        int route = 0;

        for (int i = 0, j = 0; i < tramIds.size(); i++, j++) {
            if (j == MAX_TRAMS_PER_ROUTE) {
                j = 0;
                route++;
            }
            if (i == tramId) {
                return tramRoutes[route];
            }
        }
        // Route not found.
        return -1;
    }

    @Override
    public int getFirstStop(int route) throws RemoteException {
        return tramStops.get(route)[0];
    }

    @Override
    public int getSecondStop(int route) throws RemoteException {
        return tramStops.get(route)[1];
    }

    @Override
    public long getRPCId() throws RemoteException {
        RPCId++;
        return RPCId;
    }
}

interface ConnectedToRmCallback {
    void onConnected(boolean connected);
}
