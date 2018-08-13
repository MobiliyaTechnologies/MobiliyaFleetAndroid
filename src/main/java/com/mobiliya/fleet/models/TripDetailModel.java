package com.mobiliya.fleet.models;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TripDetailModel {
    public String _id;
    public String tripName;
    public String startTime;
    public String endTime;
    public String startLocation;
    public String endLocation;
    public String vehicleId;
    public String commonId;
    @SerializedName("locationDetails")
    public List<LatLong> locationDetails;
    public int stops;
    public String speedings;
    public String mileage;
    public String topSpeed;
    public String avgSpeed;
    public String fuelUsed;
    public String milesDriven;
    public String tripDuration;
    public String description;

    public String hardBraking;
    public String aggressiveAccelerator;
    public String faultCount;
}


