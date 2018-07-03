package com.mobiliya.fleet.db;


public class SqlConstants {
    public static final String GET_PARAMS_DATA = "SELECT * FROM ParameterTable LIMIT 10";

    public static final String GET_CURRENT_TRIP = "SELECT * FROM TripTable WHERE Status = 1 OR Status = 2";

    public static final String GET_UNSYNCED_TRIP = "SELECT * FROM TripTable WHERE IsSynced = 0 ";

}
