package client;

import data.Message;
import data.RPCMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import server.TrackingService;
import server.TrackingServiceImpl;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static data.RPCMessage.MessageType.REPLY;
import static data.RPCMessage.MessageType.REQUEST;
import static server.TrackingServiceImpl.MAX_SLEEP;
import static server.TrackingServiceImpl.MIN_SLEEP;

/**
 * Client representing a tram, to connect and interact with the tracking service.
 *
 * @author michael
 */
public class TramClient {

    private static final Logger logger = LogManager.getLogger(TramClient.class);

    private TrackingService mTrackingService;
    private int mTramId;
    private int mRoute;
    private int mPreviousStop;
    private int mCurrentStop;
    private int mNextStop;
    private int mTransactionId;
    private int mRequestId;
    private boolean mServerResponding = true;

    public TramClient() {
        mTransactionId = 0;
        mRequestId = 0;
    }

    /*
    * Confire RMI on the client and get starting paramaters.
    * */
    public void connectTramWithServer() {
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

        try {
            mTramId = mTrackingService.getTramId();
            if (mTramId == -1) {
                logger.warn("No tramID's available. Quitting...");
                System.exit(-1);
            }
            mRoute = mTrackingService.getRoute(mTramId);
            mCurrentStop = mTrackingService.getFirstStop(mRoute);
            mPreviousStop = mTrackingService.getSecondStop(mRoute);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    /*
    * Start a tram going up and down it's route.
    * */
    public void startTram() {
        logger.info("Starting tram...");
        System.out.println("Tram " + (mTramId + 1) + " on route " + mRoute + " starting at stop " + mCurrentStop + "" +
                "." + "\n");

        while (mServerResponding) {
            /*
            * Retrieve next stop.
            * */
            boolean receivedNextStop = false;
            mTransactionId++;
            mRequestId++;
            long RPCId = -1;
            try {
                RPCId = mTrackingService.getRPCId();
            } catch (RemoteException e) {
                logger.warn("Remote exception. Quitting...");
                mServerResponding = false;
            }

            while (!receivedNextStop && mServerResponding) {
                logger.info("Retreiving next stop...");
                Message requestMessage = new Message();
                Message responseMessage;
                RPCMessage requestRPCMessage = new RPCMessage();
                requestRPCMessage.setMessageType(REQUEST);
                requestRPCMessage.setTransactionId(mTransactionId);
                requestRPCMessage.setRPCId(RPCId);
                requestRPCMessage.setRequestId(mRequestId);
                requestRPCMessage.setProcedureId(TrackingServiceImpl.RETRIEVE_NEXT_STOP_PROCEDURE_ID);
                requestRPCMessage.setCsv_data(getRoute() + "," + getCurrentStop() + "," + getPreviousStop());
                requestRPCMessage.setStatus(TrackingServiceImpl.SUCCESS_STATUS);
                requestMessage.marshal(requestRPCMessage);

                try {
                    responseMessage = mTrackingService.retrieveNextStop(requestMessage);
                    RPCMessage rpcMessageReceived = responseMessage.unMarshal();
                    RPCMessage.MessageType type = rpcMessageReceived.getMessageType();
                    logger.info("type: " + type);
                    long transactionId = rpcMessageReceived.getTransactionId();
                    logger.info("transactionId: " + transactionId);
                    long RPCIdReceived = rpcMessageReceived.getRPCId();
                    logger.info("RPCId: " + RPCIdReceived);
                    long requestId = rpcMessageReceived.getRequestId();
                    logger.info("requestId: " + requestId);
                    short procedureId = rpcMessageReceived.getProcedureId();
                    logger.info("procedureId: " + procedureId);
                    String data = rpcMessageReceived.getCsv_data();
                    logger.info("NEXT STOP: " + data);
                    short status = rpcMessageReceived.getStatus();
                    logger.info("status: " + status);

                    if (type == REPLY &&
                            transactionId == mTransactionId &&
                            RPCIdReceived == RPCId &&
                            requestId == mRequestId &&
                            procedureId == TrackingServiceImpl.RETRIEVE_NEXT_STOP_PROCEDURE_ID &&
                            status == TrackingServiceImpl.SUCCESS_STATUS) {

                        if (data != null) {
                            mNextStop = Integer.parseInt(data);
                            if (mNextStop != -1) {
                                receivedNextStop = true;

                                // Print out the next stop and current time to the console
                                String time = new SimpleDateFormat("h:mm a").format(new Date());
                                System.out.println("The next stop is " + mNextStop + ", the current time is: " + time);
                            } else {
                                System.out.println("Invalid next stop received from server. Retrying...");
                            }
                        } else {
                            logger.warn("Next stop unavailable.");
                        }
                    } else {
                        System.out.println("Status from server is FAIL. Retrying...");
                    }
                } catch (RemoteException e) {
                    logger.warn("Remote exception. Quitting...");
                    mServerResponding = false;
                }
            }

            /*
            * Sleep the tram for a random interval between 10 and 20 seconds while it travels to the next stop.
            * */
            if (mServerResponding) {
                Random rand =  new Random();
                int randomNum = rand.nextInt((MAX_SLEEP - MIN_SLEEP) + 1) + MIN_SLEEP;

                try {
                    System.out.println("ETA for stop " + mNextStop + " is " + randomNum + " " + "seconds...");
                    Thread.sleep(randomNum * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            /*
            * Update tram location.
            * */
            boolean updateedTramLocation = false;
            if (mServerResponding) {
                mPreviousStop = mCurrentStop;
                mCurrentStop = mNextStop;
                System.out.println("Arrived at stop " + mCurrentStop + ".\n");

                RPCId = -1;
                try {
                    RPCId = mTrackingService.getRPCId();
                } catch (RemoteException e) {
                    logger.warn("Remote exception. Quitting...");
                    mServerResponding = false;
                }
                mRequestId++;
            }

            while (!updateedTramLocation && mServerResponding) {
                logger.info("Updating tram location...");
                Message updateLocationMessage = new Message();
                Message updateLocationresponseMessage;
                RPCMessage rpcMessageUpdateLocation = new RPCMessage();
                rpcMessageUpdateLocation.setMessageType(REQUEST);
                rpcMessageUpdateLocation.setTransactionId(mTransactionId);
                rpcMessageUpdateLocation.setRPCId(RPCId);
                rpcMessageUpdateLocation.setRequestId(mRequestId);
                rpcMessageUpdateLocation.setProcedureId(TrackingServiceImpl.UPDATE_TRAM_LOCATION_PROCEDURE_ID);
                rpcMessageUpdateLocation.setCsv_data(getRoute() + "," + mTramId + "," + getCurrentStop());
                rpcMessageUpdateLocation.setStatus(TrackingServiceImpl.SUCCESS_STATUS);
                updateLocationMessage.marshal(rpcMessageUpdateLocation);

                try {
                    updateLocationresponseMessage = mTrackingService.updateTramLocation(updateLocationMessage);
                    RPCMessage rpcMessageReceived = updateLocationresponseMessage.unMarshal();
                    RPCMessage.MessageType type = rpcMessageReceived.getMessageType();
                    logger.info("type: " + type);
                    long transactionId = rpcMessageReceived.getTransactionId();
                    logger.info("transactionId: " + transactionId);
                    long RPCIdReceived = rpcMessageReceived.getRPCId();
                    logger.info("RPCId: " + RPCIdReceived);
                    long requestId = rpcMessageReceived.getRequestId();
                    logger.info("requestId: " + requestId);
                    short procedureId = rpcMessageReceived.getProcedureId();
                    logger.info("procedureId: " + procedureId);
                    String data = rpcMessageReceived.getCsv_data();
                    logger.info("NEXT STOP: " + data);
                    short status = rpcMessageReceived.getStatus();
                    logger.info("status: " + status);

                    if (type == REPLY &&
                            transactionId == mTransactionId &&
                            RPCIdReceived == RPCId &&
                            requestId == mRequestId &&
                            procedureId == TrackingServiceImpl.UPDATE_TRAM_LOCATION_PROCEDURE_ID &&
                            status == TrackingServiceImpl.SUCCESS_STATUS) {
                        updateedTramLocation = true;
                    } else {
                        System.out.println("Failed to update tram location on server. Retrying...");
                    }
                } catch (RemoteException e) {
                    logger.warn("Remove exception. Quitting...");
                    mServerResponding = false;
                    break;
                }
            }
        }
    }

    /*
    * Main method to run a tram client.
    * */
    public static void main(String args[]) {
        TramClient tramClient = new TramClient();
        tramClient.connectTramWithServer();
        tramClient.startTram();
    }

    /*
    * Setters and getters.
    * */
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
}
