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
    private static Map<String, TrackingService> mTrackingServices = new HashMap<>();
    private int mTramId;
    private int mRoute;
    private volatile long RPCId;

    static {
        mReplicationManagers.put("rm1", 9318);
        mReplicationManagers.put("rm2", 9319);
        mReplicationManagers.put("rm3", 9320);
    }

    public ServerFrontEndImpl() {
        RPCId = 0;
        connectServerFrontEndWithReplicationManagers();
        setupRMI(this);
        System.out.println("Created frontend Server, accepting connections...");
    }

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
     * Configure RMI on the replicationManagerServer.
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
     * Configure RMI on the replicationManagerServer front end as a client of replication manager.
     * */
    private void connectServerFrontEndWithReplicationManagers() {
        // Connect required number of RM's as per spec.
        for (String rm : mReplicationManagers.keySet()) {
            logger.info("Connecting frontend server with a replication manager server...");
            int port = mReplicationManagers.get(rm);
            String host = "localhost";
            String url = "rmi://" + host + "/" + rm + "/";

            try {
                Registry registry = LocateRegistry.getRegistry("localhost", port);
                TrackingService trackingService = (TrackingService) registry.lookup(url);
                mTrackingServices.put(rm, trackingService);
            } catch (NotBoundException | IOException e) {
//                e.printStackTrace();
            }
        }
    }

    @Override
    public List<TrackingService> listTramService() {
        List<TrackingService> availableTrackingServices = new ArrayList<>();

        for (String rm : mTrackingServices.keySet()) {
            try {
                if (mTrackingServices.get(rm).alive()) {
                    availableTrackingServices.add(mTrackingServices.get(rm));
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return availableTrackingServices;
    }

    @Override
    public void printTrackingServiceAvailability(List<TrackingService> trackingServices) {
        for (int i = 1; i <= trackingServices.size(); i++) {
            System.out.println("RM" + i + ": on");
        }
    }

    @Override
    public Message retrieveNextStop(Message message) throws RemoteException {
        List<TrackingService> availableTrackingServices = listTramService();
        printTrackingServiceAvailability(availableTrackingServices);

        Message messageReply = new Message();
        for (TrackingService trackingService : availableTrackingServices) {
            messageReply = trackingService.retrieveNextStop(message);
        }
        return messageReply;
    }

    @Override
    public Message updateTramLocation(Message message) throws RemoteException {
        List<TrackingService> availableTrackingServices = listTramService();
        printTrackingServiceAvailability(availableTrackingServices);

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

    /*
    * Main method to run the front end server.
    * */
    public static void main(String args[]) {
        getInstance();
    }
}
