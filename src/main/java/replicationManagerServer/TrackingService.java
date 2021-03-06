package replicationManagerServer;

import data.Message;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface for clients to connect to the tracking service.
 *
 * @author michael
 */
public interface TrackingService extends Remote {

    Message retrieveNextStop(Message message) throws RemoteException;
    Message updateTramLocation(Message message) throws RemoteException;
}
