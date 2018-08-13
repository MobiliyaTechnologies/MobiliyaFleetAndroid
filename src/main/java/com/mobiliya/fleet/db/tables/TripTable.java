package com.mobiliya.fleet.db.tables;

@SuppressWarnings({"ALL", "unused"})
public class TripTable extends DB_BASIC {
    public String TripId;
    public String ServerId;
    public String TenantId;
    public String TripName;
    public String Description;
    public String StartTime;
    public String EndTime;
    public String VehicleID;
    public String StartLocation;
    public String EndLocation;
    public String milesDriven;
    public int Status;
    public int Stops;
    public boolean IsSynced;
    public String speedings;
}
