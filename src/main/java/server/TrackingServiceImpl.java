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

/**
 * Implementation of the tram tacking service. Clients can connect and interact using available methods.
 * <p>
 * Implemented using the singleton pattern.
 *
 * @author michael
 */
public class TrackingServiceImpl implements TrackingService {

    private static final Logger logger = LogManager.getLogger(TrackingServiceImpl.class);
    private volatile static TrackingServiceImpl uniqueInstance;
    private static Map<Integer, int[]> tramRoutes;
    private volatile long RPCId;

    private TrackingServiceImpl() {
        tramRoutes = new HashMap<>();
        tramRoutes.put(1, new int[]{1, 2, 3, 4, 5});
        tramRoutes.put(96, new int[]{23, 24, 2, 34, 22});
        tramRoutes.put(101, new int[]{123, 11, 22, 34, 5, 4, 7});
        tramRoutes.put(109, new int[]{88, 87, 85, 80, 9, 7, 2, 1});
        tramRoutes.put(112, new int[]{110, 123, 11, 22, 34, 33, 29, 4});
        RPCId = 0;
        logger.info("Created server");
        setupRMI(this);
    }

    private void setupRMI(TrackingService trackingService) {
        int port = 9317;
        String host = "localhost";
        String url = "rmi://" + host + "/trackingServer/";

        try {
            TrackingService stub = (TrackingService) UnicastRemoteObject.exportObject(trackingService, 0);
            Registry registry = LocateRegistry.createRegistry(port);
            registry.bind(url, stub);
            System.out.println("Tracking server bound to: " + url);
        } catch (RemoteException | AlreadyBoundException e) {
            e.printStackTrace();
        }
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

        String[] values = data.split(",");
        int routeId = Integer.parseInt(values[0].trim());
        logger.info("RouteId: " + routeId);
        int currentStop = Integer.parseInt(values[1]);
        logger.info("CurrentStop: " + currentStop);
        int previousStop = Integer.parseInt(values[2]);
        logger.info("PreviousStop: " + previousStop);

        Message messageResponse = new Message();
        RPCMessage rpcMessageReply = new RPCMessage();
        rpcMessageReply.setMessageType(REPLY);
        rpcMessageReply.setTransactionId(1);
        RPCId++;
        rpcMessageReply.setRPCId(RPCId);
        rpcMessageReply.setRequestId(3);
        rpcMessageReply.setProcedureId((short) 4);
        rpcMessageReply.setCsv_data("2");
        rpcMessageReply.setStatus((short)5);
        messageResponse.marshal(rpcMessageReply);

        return messageResponse;
    }

    @Override
    public void updateTramLocation(Message message) throws RemoteException {
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


        String[] values = data.split(",");
        int routeId = Integer.parseInt(values[0].trim());
        int tramId = Integer.parseInt(values[1]);
        int stopId = Integer.parseInt(values[2]);

        System.out.println("\n\n");
        logger.info("RouteId: " + routeId);
        logger.info("TramId: " + tramId);
        logger.info("StopId: " + stopId);

    }

    public synchronized long getRPCId() {
        RPCId++;
        return RPCId;
    }
}
