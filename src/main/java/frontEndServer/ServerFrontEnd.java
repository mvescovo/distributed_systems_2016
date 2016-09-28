package frontEndServer;

import data.Message;
import replicationManagerServer.TrackingService;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * @author michael.
 */
public interface ServerFrontEnd extends Remote {

    List<TrackingService> listTramService() throws RemoteException;
    void printTrackingServiceAvailability(List<TrackingService> trackingServices) throws RemoteException;

    int getTramId() throws RemoteException;
    int getRoute(int tramId) throws RemoteException;
    int getFirstStop(int route) throws RemoteException;
    int getSecondStop(int route) throws RemoteException;
    long getRPCId() throws RemoteException;

    Message retrieveNextStop(Message message) throws RemoteException;
    Message updateTramLocation(Message message) throws RemoteException;
}
