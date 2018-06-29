package com.mobiliya.fleet.models;

import com.mobiliya.fleet.db.tables.DB_BASIC;

import static com.mobiliya.fleet.utils.DateUtils.getLocalTimeString;

/**
 * Created by Darshana Pandit on 26-06-2018.
 */

public class BaseParameters extends DB_BASIC {
    public String TenantId = null;
    public String FaultSPN = "0";
    public String FaultDescription = "0";
    public String ParameterDateTime = getLocalTimeString();
    public String TripId = null;
    public String VehicleId = null;
    public float FuelUsed = 0;
    public float Distance = 0;
    public float Speed = 0;
    public float AvgFuelEcon = 0;
    public String Latitude=null;
    public String Longitude=null;
    public String UserId = null;
    public boolean isConnected = false;
    public int RPM = 0;
    public float AccelPedal = 0;
    public String BrakeSwitch = null;
    public String ClutchSwitch = null;
    public float IntakePressure = 0;
    public String AirIntakeTemperature = null;


    public String VehicleRegNumber = null;
    public String DiagonosticTroubleCodes = null;
    public String EngineSerialNo = null;
    public String EngineVIN = null;
    public float OilTemp = 0;
    public double EngineCrankcasePressure = -1;
    public double EngineIntakeManifoldPressure = 0;
    public double AmbientTemp = 0;
    public double BarometricPressure = -1;
}
