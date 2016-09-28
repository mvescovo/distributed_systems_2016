package replicationManagerServer;

import data.Message;
import data.RPCMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

import static data.RPCMessage.MessageType.REPLY;
import static data.RPCMessage.MessageType.REQUEST;

/**
 * @author michael.
 */
public class ReplicationManagerImpl implements ReplicationManager, TrackingService {

    private static final Logger logger = LogManager.getLogger(ReplicationManagerImpl.class);

    public static final short RETRIEVE_NEXT_STOP_PROCEDURE_ID = 1;
    public static final short UPDATE_TRAM_LOCATION_PROCEDURE_ID = 2;
    public static final short SUCCESS_STATUS = 0;
    public static final short FAILURE_STATUS = -1;
    public static final int MIN_SLEEP = 10;
    public static final int MAX_SLEEP = 20;
    private static final int NUM_ROUTES = 5;
    public static final int MAX_TRAMS_PER_ROUTE = 5;

    public static int[] tramRoutes = {1, 96, 101, 109, 112};
    public static Map<Integer, int[]> tramStops = new HashMap<>();
    public static Map<Integer, Boolean> tramIds = new HashMap<>();
    private Map<Integer, Integer> tramLocation;
    private int mPort;
    private String mName;

    static {
        tramStops.put(1, new int[]{1, 2, 3, 4, 5});
        tramStops.put(96, new int[]{23, 24, 2, 34, 22});
        tramStops.put(101, new int[]{123, 11, 22, 34, 5, 4, 7});
        tramStops.put(109, new int[]{88, 87, 85, 80, 9, 7, 2, 1});
        tramStops.put(112, new int[]{110, 123, 11, 22, 34, 33, 29, 4});
    }

    public ReplicationManagerImpl(int port, String name) {
        mPort = port;
        mName = name;

        tramIds = new HashMap<>();
        for (int i = 0; i < NUM_ROUTES * MAX_TRAMS_PER_ROUTE; i++) {
            tramIds.put(i, false);
        }

        System.out.println("Created replication manager server " + mName + " on port " + mPort + ", accepting connections...");
        tramLocation = new HashMap<>();

        setupRMI(this);
    }

    /*
     * Configure RMI on the replicationManagerServer.
     * */
    private void setupRMI(TrackingService trackingService) {
        String host = "localhost";
        String url = "rmi://" + host + "/" + mName + "/";

        try {
            TrackingService stub = (TrackingService) UnicastRemoteObject.exportObject(trackingService, 0);
            Registry registry = LocateRegistry.createRegistry(mPort);
            registry.bind(url, stub);
            logger.info(mName + " bound to: " + url);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    public boolean alive() throws RemoteException {
        return true;
    }

    @Override
    public Message retrieveNextStop(Message message) throws RemoteException {
        RPCMessage rpcMessageReceived = message.unMarshal();
        RPCMessage.MessageType type = rpcMessageReceived.getMessageType();
        logger.info("type: " + type);
        long transactionId = rpcMessageReceived.getTransactionId();
        logger.info("transactionId: " + transactionId);
        long RPCId = rpcMessageReceived.getRPCId();
        logger.info("RPCId: " + RPCId);
        long requestId = rpcMessageReceived.getRequestId();
        logger.info("requestId: " + requestId);
        short procedureId = rpcMessageReceived.getProcedureId();
        logger.info("procedureId: " + procedureId);
        String data = rpcMessageReceived.getCsv_data();
        logger.info("data: " + data);
        short status = rpcMessageReceived.getStatus();
        logger.info("status: " + status);

        Message messageResponse = new Message();

        if (type == REQUEST && procedureId == RETRIEVE_NEXT_STOP_PROCEDURE_ID) {
            RPCMessage rpcMessageReply = new RPCMessage();
            rpcMessageReply.setMessageType(REPLY);
            rpcMessageReply.setTransactionId(transactionId);
            rpcMessageReply.setRPCId(RPCId);
            rpcMessageReply.setRequestId(requestId);
            rpcMessageReply.setProcedureId(procedureId);

            String[] values = data.split(",");
            int routeId = Integer.parseInt(values[0].trim());
            logger.info("RouteId: " + routeId);
            int currentStop = Integer.parseInt(values[1]);
            logger.info("CurrentStop: " + currentStop);
            int previousStop = Integer.parseInt(values[2]);
            logger.info("PreviousStop: " + previousStop);

            int[] stops = tramStops.get(routeId);
            int currentStopIndex;
            int nextStop = -1;

            // Find the next stop
            for (int i = 0; i < stops.length; i++) {
                if (stops[i] == currentStop) {
                    currentStopIndex = i;
                    if (currentStopIndex == 0 ||
                            currentStopIndex != stops.length - 1 && previousStop == stops[currentStopIndex - 1]) {
                        // The tram is going right
                        nextStop = stops[currentStopIndex + 1];
                    } else {
                        // The tram is going left
                        nextStop = stops[currentStopIndex - 1];
                    }
                    break;
                }
            }
            rpcMessageReply.setCsv_data(Integer.toString(nextStop));

            if (nextStop == -1) {
                rpcMessageReply.setStatus(FAILURE_STATUS);
            } else {
                rpcMessageReply.setStatus(SUCCESS_STATUS);
            }

            messageResponse.marshal(rpcMessageReply);
            return messageResponse;
        } else {
            // Ignore the message
            return null;
        }
    }

    @Override
    public Message updateTramLocation(Message message) throws RemoteException {
        RPCMessage rpcMessageReceived = message.unMarshal();
        RPCMessage.MessageType type = rpcMessageReceived.getMessageType();
        logger.info("type: " + type);
        long transactionId = rpcMessageReceived.getTransactionId();
        logger.info("transactionId: " + transactionId);
        long RPCId = rpcMessageReceived.getRPCId();
        logger.info("RPCId: " + RPCId);
        long requestId = rpcMessageReceived.getRequestId();
        logger.info("requestId: " + requestId);
        short procedureId = rpcMessageReceived.getProcedureId();
        logger.info("procedureId: " + procedureId);
        String data = rpcMessageReceived.getCsv_data();
        logger.info("data: " + data);
        short status = rpcMessageReceived.getStatus();
        logger.info("status: " + status);

        Message messageResponse = new Message();

        if (type == REQUEST && procedureId == UPDATE_TRAM_LOCATION_PROCEDURE_ID) {
            RPCMessage rpcMessageReply = new RPCMessage();
            rpcMessageReply.setMessageType(REPLY);
            rpcMessageReply.setTransactionId(transactionId);
            rpcMessageReply.setRPCId(RPCId);
            rpcMessageReply.setRequestId(requestId);
            rpcMessageReply.setProcedureId(procedureId);

            String[] values = data.split(",");
            int routeId = Integer.parseInt(values[0].trim());
            logger.info("RouteId: " + routeId);
            int tramId = Integer.parseInt(values[1]);
            logger.info("TramId: " + tramId);
            int stopId = Integer.parseInt(values[2]);
            logger.info("StopId: " + stopId);

            tramLocation.put(tramId, stopId);

            rpcMessageReply.setCsv_data("");
            rpcMessageReply.setStatus(SUCCESS_STATUS);
            messageResponse.marshal(rpcMessageReply);
            return messageResponse;
        }

        System.out.println("\n\n");
        return messageResponse;
    }

    /*
    * Main method to run a replica manager.
    * */
    public static void main(String args[]) {
        if (args.length == 2) {
            logger.info("Port: " + args[0]);
            logger.info("Name: " + args[1]);
            ReplicationManager replicationManager = new ReplicationManagerImpl(Integer.parseInt(args[0]), args[1]);
        } else {
            logger.debug("Invalid arugment length. Need port and name. E.g. \"9318 rm1\"");
        }
    }
}
