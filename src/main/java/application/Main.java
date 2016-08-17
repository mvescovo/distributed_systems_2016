package application;

import server.TrackingService;
import server.TrackingServiceImpl;

/**
 * Main class to run the application.
 *
 * @author michael
 */
public class Main {

    public static void main(String[] args) {

        TrackingService trackingService = TrackingServiceImpl.getInstance();
    }
}
