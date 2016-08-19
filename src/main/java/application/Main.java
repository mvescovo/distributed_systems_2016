package application;

import server.TrackingService;
import server.TrackingServiceImpl;

/**
 * Main class to start the server.
 *
 * @author michael
 */
public class Main {

    public static void main(String[] args) {

        TrackingService trackingService = TrackingServiceImpl.getInstance();
    }
}
