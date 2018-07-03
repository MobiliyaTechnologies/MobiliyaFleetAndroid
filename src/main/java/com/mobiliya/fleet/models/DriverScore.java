package com.mobiliya.fleet.models;

public class DriverScore {
    public String _id;
    public String tripId;
    public String userId;
    public behaviour driverBehaviour;

    public class behaviour {
        public int overSpeeding;
        public int hardBraking;
        public int aggressiveAccelerator;
        public int vehicleStops;
        public int driverScore;
    }
}
