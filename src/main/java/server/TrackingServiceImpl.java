package server;

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
 * Implementation of the tram tacking service. Clients can connect and interact using available methods.
 * <p>
 * Implemented using the singleton pattern.
 *
 * @author michael
 */
public class TrackingServiceImpl implements TrackingService {

    private static final Logger logger = LogManager.getLogger(TrackingServiceImpl.class);

    public static final short RETRIEVE_NEXT_STOP_PROCEDURE_ID = 1;
    public static final short UPDATE_TRAM_LOCATION_PROCEDURE_ID = 2;
    public static final short SUCCESS_STATUS = 0;
    private static final short FAILURE_STATUS = -1;
    public static final int MIN_SLEEP = 10;
    public static final int MAX_SLEEP = 20;
    private static final int NUM_ROUTES = 5;
    private static final int MAX_TRAMS_PER_ROUTE = 5;

    private volatile static TrackingServiceImpl uniqueInstance;
    private int[] tramRoutes = {1, 96, 101, 109, 112};
    private static Map<Integer, int[]> tramStops;
    private Map<Integer, Boolean> tramIds;
    private volatile long RPCId;
    private Map<Integer, Integer> tramLocation;

    private TrackingServiceImpl() {
        tramStops = new HashMap<>();
        tramStops.put(1, new int[]{1, 2, 3, 4, 5});
        tramStops.put(96, new int[]{23, 24, 2, 34, 22});
        tramStops.put(101, new int[]{123, 11, 22, 34, 5, 4, 7});
        tramStops.put(109, new int[]{88, 87, 85, 80, 9, 7, 2, 1});
        tramStops.put(112, new int[]{110, 123, 11, 22, 34, 33, 29, 4});

        tramIds = new HashMap<>();
        for (int i = 0; i < NUM_ROUTES * MAX_TRAMS_PER_ROUTE; i++) {
            tramIds.put(i, false);
        }

        RPCId = 0;
        setupRMI(this);
        tramLocation = new HashMap<>();
        System.out.println("Created server, accepting connections...");
    }

    public static TrackingServiceImpl getInstance() {
        if (uniqueInstance == null) {
            synchronized (TrackingServiceImpl.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new TrackingServiceImpl();
                }
            }
        }
        return uniqueInstance;
    }

    /*
    * Configure RMI on the server.
    * */
    private void setupRMI(TrackingService trackingService) {
        int port = 9317;
        String host = "localhost";
        String url = "rmi://" + host + "/trackingServer/";

        try {
            TrackingService stub = (TrackingService) UnicastRemoteObject.exportObject(trackingService, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind(url, stub);
            logger.info("Tracking server bound to: " + url);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    /*
    * Respond to a client request for the next stop on it's route.
    * */
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

    /*
    * Respond to a client's request to update their location.
    * */
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

    @Override
    public synchronized int getTramId() throws RemoteException {
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
    public int getRoute(int tramId) {
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
    public synchronized long getRPCId() throws RemoteException {
        RPCId++;
        return RPCId;
    }
}
