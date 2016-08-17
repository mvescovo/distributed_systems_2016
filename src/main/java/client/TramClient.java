package client;

import data.Message;
import data.RPCMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.TrackingService;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import static data.RPCMessage.MessageType.REQUEST;

/**
 * Client representing a tram, to connect and interact with the tracking service.
 *
 * @author michael
 */
public class TramClient {

    private static final Logger logger = LogManager.getLogger(TramClient.class);
    private int mRoute;
    private int mPreviousStop;
    private int mCurrentStop;
    private int mNextStop;
    private TrackingService mTrackingService;
    private int mTramId;

    private TramClient() {
        /*
        * Randomly select a tram route and current stop.
        * */
        mRoute = 1;
        mPreviousStop = 2;
        mCurrentStop = 1;
        mNextStop = 2;
        mTramId = 20;
    }

    public static Logger getLogger() {
        return logger;
    }

    public int getRoute() {
        return mRoute;
    }

    public void setRoute(int mRoute) {
        this.mRoute = mRoute;
    }

    public int getPreviousStop() {
        return mPreviousStop;
    }

    public void setPreviousStop(int mPreviousStop) {
        this.mPreviousStop = mPreviousStop;
    }

    public int getCurrentStop() {
        return mCurrentStop;
    }

    public void setCurrentStop(int mCurrentStop) {
        this.mCurrentStop = mCurrentStop;
    }

    public int getNextStop() {
        return mNextStop;
    }

    public void setNextStop(int mNextStop) {
        this.mNextStop = mNextStop;
    }

    private void connectTramWithServer() {
        logger.info("Connecting tram with server...");
        int mPort = 9317;
        String mHost = "localhost";
        String mUrl = "rmi://" + mHost + "/trackingServer/";

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", mPort);
            mTrackingService = (TrackingService) registry.lookup(mUrl);
        } catch (NotBoundException | IOException e) {
            e.printStackTrace();
        }
    }

    private void startTram() {
        while (true) {
            logger.info("Starting tram...");
            /*
            * Retrieve next stop.
            * */
            logger.info("Retreiving next stop...");
            Message requestMessage = new Message();
            Message responseMessage = new Message();

            RPCMessage requestRPCMessage = new RPCMessage();
            requestRPCMessage.setMessageType(REQUEST);
            requestRPCMessage.setTransactionId(1);
            requestRPCMessage.setRPCId(2);
            requestRPCMessage.setRequestId(3);
            requestRPCMessage.setProcedureId((short) 4);
            requestRPCMessage.setCsv_data(getRoute() + "," + getCurrentStop() + "," + getPreviousStop());
            requestRPCMessage.setStatus((short)5);
            requestMessage.marshal(requestRPCMessage);

            try {
                responseMessage = mTrackingService.retrieveNextStop(requestMessage);

                RPCMessage rpcMessageReceived = responseMessage.unMarshal();
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

            } catch (RemoteException e) {
                e.printStackTrace();
            }

            /*
            * Update tram location.
            * */
            logger.info("Updating tram location...");
            Message updateLocationMessage = new Message();
            RPCMessage rpcMessageUpdateLocation = new RPCMessage();
            rpcMessageUpdateLocation.setMessageType(REQUEST);
            rpcMessageUpdateLocation.setTransactionId(1);
            rpcMessageUpdateLocation.setRPCId(2);
            rpcMessageUpdateLocation.setRequestId(3);
            rpcMessageUpdateLocation.setProcedureId((short) 4);
            rpcMessageUpdateLocation.setCsv_data(getRoute() + "," + mTramId + "," + getCurrentStop());
            rpcMessageUpdateLocation.setStatus((short)5);
            updateLocationMessage.marshal(rpcMessageUpdateLocation);

            try {
                mTrackingService.updateTramLocation(updateLocationMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            /*
            * Sleep the tram for a random interval.
            * */
            try {
                logger.info("Sleeping for 4 seconds");
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        logger.trace("Entering application.");
        TramClient tramClient = new TramClient();
        tramClient.connectTramWithServer();
        tramClient.startTram();
    }
}
