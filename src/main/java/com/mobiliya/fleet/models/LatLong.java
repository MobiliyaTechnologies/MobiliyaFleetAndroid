package com.mobiliya.fleet.models;


@SuppressWarnings({"ALL", "unused"})
public class LatLong {

    public String latitude;
    public String longitude;
    public String time;
    public Long tripId;

    public LatLong() {

    }

    public LatLong(String latitude, String longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
