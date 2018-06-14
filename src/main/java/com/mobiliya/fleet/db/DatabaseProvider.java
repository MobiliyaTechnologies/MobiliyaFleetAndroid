package com.mobiliya.fleet.db;

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mobiliya.fleet.activity.TripActivity;
import com.mobiliya.fleet.db.tables.AppInfoTable;
import com.mobiliya.fleet.db.tables.DB_BASIC;
import com.mobiliya.fleet.db.tables.FaultTable;
import com.mobiliya.fleet.db.tables.LatLongTable;
import com.mobiliya.fleet.db.tables.ParameterTable;
import com.mobiliya.fleet.db.tables.TripTable;
import com.mobiliya.fleet.db.tables.UserTable;
import com.mobiliya.fleet.models.FaultModel;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.Parameter;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.DateUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@SuppressWarnings({"ALL", "unused"})
public class DatabaseProvider {
    private static DatabaseProvider instance = null;
    private DBHandler dbHandler;
    private Long minimum_size;

    private DatabaseProvider(Context context) {
        if (dbHandler == null) {
            //Initialize DB handler
            dbHandler = new DBHandler(context);
            //Adding table based on class ParameterTable reflection
            dbHandler.CreateTable(new UserTable());
            dbHandler.CreateTable(new ParameterTable());
            dbHandler.CreateTable(new TripTable());
            dbHandler.CreateTable(new FaultTable());
            dbHandler.CreateTable(new LatLongTable());
            // dbHandler.CreateTable(new FaultCodeTable())
        }
    }

    public static DatabaseProvider getInstance(Context context) {
        if (instance == null)
            instance = new DatabaseProvider(context);
        return instance;
    }

    public DBHandler getDB() {
        return dbHandler;
    }

    public long getDefaultMaximumSize() {
        return dbHandler.getReadableDatabase().getMaximumSize();
    }

    public long getDefaultMinumumSize() {
        return minimum_size;
    }

    public long setDefaultMaximumSize(long bytes) {
        return dbHandler.getReadableDatabase().setMaximumSize(bytes);
    }

    /**
     * Add Logged in user
     *
     * @param user
     * @return
     */
    public long addUser(User user) {
        int affectedRows = dbHandler.DeleteAllTableData(new UserTable());

        UserTable table = new UserTable();

        table.UserId = user.getId();
        table.Email = user.getEmail();
        table.FirstName = user.getFirstName();
        table.LastName = user.getLastName();
        table.MobileNumber = user.getMobileNumber();
        table.Password = user.getPassword();
        table.RoleId = user.getRoleId();
        table.Status = user.getStatus();

        long id = dbHandler.AddNewObject(table);
        return id;
    }

    public long updateApplicationStatus(boolean status) {
        try {
            int affectedRows = dbHandler.DeleteAllTableData(new AppInfoTable());
        } catch (Exception ex) {

        }

        AppInfoTable table = new AppInfoTable();
        table.isAppLive = status;
        long id = dbHandler.AddNewObject(table);
        return id;
    }

    public AppInfoTable getApplicationStatus() {
        try {
            List<DB_BASIC> list = dbHandler.GetAllTableData(AppInfoTable.class);

            if (list != null && !list.isEmpty() && list.size() > 0) {
                AppInfoTable row = (AppInfoTable) list.get(0);
                AppInfoTable info = new AppInfoTable();

                info.isAppLive = row.isAppLive;

                return info;
            }

        } catch (Exception ex) {
        }
        return null;

    }

    public User getUser() {
        List<DB_BASIC> list = dbHandler.GetAllTableData(UserTable.class);

        if (list != null && !list.isEmpty() && list.size() > 0) {
            UserTable row = (UserTable) list.get(0);
            User user = new User();
            user.setId(row.UserId);
            user.setEmail(row.Email);
            user.setFirstName(row.FirstName);
            user.setLastName(row.LastName);
            user.setMobileNumber(row.MobileNumber);
            user.setStatus(row.Status);
            user.setRoleId(row.RoleId);
            user.setTenantId(row.TenantId);
            user.setPassword(row.Password);
            return user;
        }

        return null;
    }

    /**
     * Add Parameter object
     *
     * @param param
     * @return
     */
    public long addParameter(Parameter param) {
        long id = dbHandler.AddNewObject(convertToParameterTable(param));
        return id;
    }

    public int deleteAllTableData(DB_BASIC object){
        return  dbHandler.DeleteAllTableData(object);
    }

    /**
     * Insert Trip data to TripTable
     *
     * @param trip model object
     * @return returns inserted id
     */
    public String addTrip(Trip trip) {
        TripTable table = new TripTable();
        table.TripId = trip.commonId;
        table.TripName = trip.tripName;
        table.StartLocation = trip.startLocation;
        table.EndLocation = trip.endLocation;
        table.Description = trip.description;
        table.StartTime = trip.startTime;
        table.EndTime = trip.endTime;
        table.VehicleID = trip.vehicleId;
        table.Status = trip.status;

        long id = dbHandler.AddNewObject(table);
        if (id > 0) {
            return trip.commonId;
        } else {
            return null;
        }
    }

    /**
     * Update provided trip
     *
     * @param trip
     */
    public int updateTrip(Trip trip) {
        try {
            ContentValues cv = new ContentValues();
            cv.put("TripId", trip.commonId);
            cv.put("TripName", trip.tripName);
            cv.put("StartLocation", trip.startLocation);
            cv.put("EndLocation", trip.endLocation);
            cv.put("Description", trip.description);
            cv.put("StartTime", trip.startTime);
            cv.put("EndTime", trip.endTime);
            cv.put("VehicleID", trip.vehicleId);
            cv.put("Status", trip.status);
            cv.put("Stops", trip.stops);
            cv.put("IsSynced", trip.IsSynced ? 1 : 0);
            cv.put("ServerId", trip._id);
            cv.put("milesDriven", trip.milesDriven);

            return dbHandler.UpdateRow(TripTable.class, cv, "TripId='" + trip.commonId + "'");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return -1;
    }

    /**
     * Update provided tripTable
     *
     * @param tripTable
     */
    public int updateTripTable(TripTable tripTable) {
        return dbHandler.UpdateRow(tripTable);
    }


    /**
     * Delete trip
     *
     * @param tripId
     */
    public long deleteTrip(String tripId) {
        String[] params = {tripId};
        return dbHandler.DeleteTableRow(TripTable.class, "TripId=?", params);
    }

    /**
     * Get Current Running/Paused Trip
     *
     * @return If success return Trip object otherwise NULL
     */
    public Trip getCurrentTrip() {

        List<DB_BASIC> currentTripList = dbHandler.GetTableDataByQuery(TripTable.class, SqlConstants.GET_CURRENT_TRIP, null);

        if (currentTripList != null && !currentTripList.isEmpty() && currentTripList.size() > 0) {
            DB_BASIC row = currentTripList.get(currentTripList.size() - 1);
            TripTable table = (TripTable) row;

            Trip trip = new Trip();

            trip.commonId = table.TripId;
            trip.tripName = table.TripName;
            trip.startLocation = table.StartLocation;
            trip.endLocation = table.EndLocation;
            trip.description = table.Description;
            trip.startTime = table.StartTime;
            trip.endTime = table.EndTime;
            trip.vehicleId = table.VehicleID;
            trip.status = table.Status;
            trip.IsSynced = table.IsSynced;
            trip._id = table.ServerId;

            return trip;
        } else {
            return null;
        }
    }

    public Trip getTripById(String tripId) {
        String[] ids = {tripId};
        List<DB_BASIC> currentTripList = dbHandler.GetTableRow(TripTable.class, "TripId=?", ids);

        if (currentTripList != null && !currentTripList.isEmpty() && currentTripList.size() > 0) {
            DB_BASIC row = currentTripList.get(currentTripList.size() - 1);
            TripTable table = (TripTable) row;

            Trip trip = new Trip();
            trip.commonId = table.TripId;
            trip.tripName = table.TripName;
            trip.startLocation = table.StartLocation;
            trip.endLocation = table.EndLocation;
            trip.description = table.Description;
            trip.startTime = table.StartTime;
            trip.endTime = table.EndTime;
            trip.vehicleId = table.VehicleID;
            trip.status = table.Status;
            trip.IsSynced = table.IsSynced;
            trip._id = table.ServerId;

            return trip;
        } else {
            return null;
        }
    }

    public List<Trip> getLastUnSyncedTrip() {

        List<DB_BASIC> currentTripList = dbHandler.GetTableDataByQuery(TripTable.class, SqlConstants.GET_UNSYNCED_TRIP, null);
        List<Trip> tripslist = null;
        if (currentTripList != null && !currentTripList.isEmpty() && currentTripList.size() > 0) {
            tripslist = new ArrayList<>();
            for (int i = 0; i < currentTripList.size(); i++) {
                DB_BASIC row = currentTripList.get(i);
                TripTable table = (TripTable) row;
                Trip trip = new Trip();
                trip.commonId = table.TripId;
                trip.tripName = table.TripName;
                trip.startLocation = table.StartLocation;
                trip.endLocation = table.EndLocation;
                trip.description = table.Description;
                trip.startTime = table.StartTime;
                trip.endTime = table.EndTime;
                trip.vehicleId = table.VehicleID;
                trip.stops = table.Stops;
                trip.status = table.Status;
                trip.stops = table.Stops;
                trip.milesDriven = table.milesDriven;
                trip._id = table.ServerId;
                tripslist.add(trip);
            }
        } else {
            return null;
        }

        return tripslist;
    }

    /**
     * Get Top 10 records of ParameterTable.
     *
     * @return Returns List of Parameter object
     */
    public Parameter[] getParameterData() {
        List<DB_BASIC> pTable = dbHandler.GetTableDataByQuery(ParameterTable.class, SqlConstants.GET_PARAMS_DATA, null);
        if (pTable.size() > 0) {
            Parameter[] parameters = new Parameter[pTable.size()];
            for (int i = 0; i < pTable.size(); i++) {
                ParameterTable row = (ParameterTable) pTable.get(i);
                parameters[i] = convertToParameterObject(row);
            }
            return parameters;
        }
        return null;
    }

    public long deleteParameter(long id) {
        try {
            String[] params = {String.valueOf(id)};
            long result = dbHandler.DeleteTableRow(ParameterTable.class, "ID=?", params);
            return result;
        } catch (Exception e) {
            return -1;
        }
    }

    private ParameterTable convertToParameterTable(Parameter param) {
        ParameterTable p = new ParameterTable();

        p.ID = param.ID;
        p.TripId = param.TripId;
        p.TenantId = param.TenantId;
        p.UserId = param.UserId;
        p.VehicleId = param.VehicleId;
        p.VIN = param.VIN;
        p.RPM = param.RPM;
        p.Speed = param.Speed;
        p.MaxSpeed = param.MaxSpeed;
        p.HiResMaxSpeed = param.HiResMaxSpeed;
        p.Distance = param.Distance;
        p.HiResDistance = param.HiResDistance;
        p.LoResDistance = param.LoResDistance;
        p.Odometer = param.Odometer;
        p.HiResOdometer = param.HiResOdometer;
        p.LoResOdometer = param.LoResOdometer;
        p.TotalHours = param.TotalHours;
        p.IdleHours = param.IdleHours;
        p.PctLoad = param.PctLoad;
        p.PctTorque = param.PctTorque;
        p.DrvPctTorque = param.DrvPctTorque;
        p.TorqueMode = param.TorqueMode;
        p.FuelUsed = param.FuelUsed;
        p.HiResFuelUsed = param.HiResFuelUsed;
        p.IdleFuelUsed = param.IdleFuelUsed;
        p.FuelRate = param.FuelRate;
        p.AvgFuelEcon = param.AvgFuelEcon;
        p.InstFuelEcon = param.InstFuelEcon;
        p.PrimaryFuelLevel = param.PrimaryFuelLevel;
        p.SecondaryFuelLevel = param.SecondaryFuelLevel;
        p.OilTemp = param.OilTemp;
        p.OilPressure = param.OilPressure;
        p.TransTemp = param.TransTemp;
        p.IntakeTemp = param.IntakeTemp;
        p.IntakePressure = param.IntakePressure;
        p.CoolantTemp = param.CoolantTemp;
        p.CoolantLevel = param.CoolantLevel;
        p.CoolantPressure = param.CoolantPressure;
        p.BrakeAppPressure = param.BrakeAppPressure;
        p.Brake1AirPressure = param.Brake1AirPressure;
        p.Brake2AirPressure = param.Brake2AirPressure;
        p.AccelPedal = param.AccelPedal;
        p.ThrottlePos = param.ThrottlePos;
        p.BatteryPotential = param.BatteryPotential;
        p.SelectedGear = param.SelectedGear;
        p.CurrentGear = param.CurrentGear;
        p.Make = param.Make;
        p.Model = param.Model;
        p.SerialNo = param.SerialNo;
        p.UnitNo = param.UnitNo;
        p.EngineVIN = param.EngineVIN;
        p.EngineMake = param.EngineMake;
        p.EngineModel = param.EngineModel;
        p.EngineSerialNo = param.EngineSerialNo;
        p.EngineUnitNo = param.EngineUnitNo;
        p.ClutchSwitch = param.ClutchSwitch;
        p.BrakeSwitch = param.BrakeSwitch;
        p.ParkBrakeSwitch = param.ParkBrakeSwitch;
        p.CruiseSetSpeed = param.CruiseSetSpeed;
        p.CruiseOnOff = param.CruiseOnOff;
        p.CruiseSet = param.CruiseSet;
        p.CruiseCoast = param.CruiseCoast;
        p.CruiseResume = param.CruiseResume;
        p.CruiseAccel = param.CruiseAccel;
        p.CruiseActive = param.CruiseActive;
        p.CruiseState = param.CruiseState;
        p.FaultSource = param.FaultSource;
        p.FaultSPN = param.FaultSPN;
        p.FaultFMI = param.FaultFMI;
        p.FaultOccurrence = param.FaultOccurrence;
        p.FaultConversion = param.FaultConversion;
        p.Latitude = param.Latitude;
        p.Longitude = param.Longitude;
        p.ParameterDateTime = param.ParameterDateTime;
        p.AdapterId = param.AdapterId;
        p.FirmwareVersion = param.FirmwareVersion;
        p.HardwareVersion = param.HardwareVersion;
        p.AdapterSerialNo = param.AdapterSerialNo;
        p.HardwareType = param.HardwareType;
        p.IsKeyOn = param.IsKeyOn;
        p.SleepMode = param.SleepMode;
        p.LedBrightness = param.LedBrightness;
        p.Message = param.Message;
        p.Status = param.Status;


        p.AmbientTemp = param.AmbientTemp;
        p.DPFOutletTemp = param.DPFOutletTemp;
        p.EngineIntakeManifoldTemp = param.EngineIntakeManifoldTemp;
        p.DPFInletTemp = param.DPFInletTemp;
        p.EngineIntakeManifoldPressure = param.EngineIntakeManifoldPressure;
        p.DPFPressureDifferential = param.DPFPressureDifferential;
        p.EngineCrankcasePressure = param.EngineCrankcasePressure;
        p.EngineTurbochargerSpeed = param.EngineTurbochargerSpeed;
        p.FuelTemp = param.FuelTemp;
        p.SCRInletNox = param.SCRInletNox;
        p.SCROutletNox = param.SCROutletNox;
        p.TotalNoOfPassiveRegenerations = param.TotalNoOfPassiveRegenerations;
        p.DPFAshLoad = param.DPFAshLoad;
        p.TotalNoOfActiveRegenerations = param.TotalNoOfActiveRegenerations;
        p.DPFSootLoad = param.DPFSootLoad;
        p.BarometricPressure = param.BarometricPressure;
        p.FanState = param.FanState;

        p.PGN = param.PGN;
        p.PGNActualValue = param.PGNActualValue;
        p.PGNRawValue = param.PGNRawValue;
        p.AccelPedal = param.AccelPedal;
        p.VehicleSpeed = param.VehicleSpeed;
        p.FaultDescription = param.FaultDescription;
        p.isConnected = param.isConnected;

        return p;
    }

    private Parameter convertToParameterObject(ParameterTable param) {
        Parameter p = new Parameter();

        p.ID = param.ID;
        p.TripId = param.TripId;
        p.TenantId = param.TenantId;
        p.UserId = param.UserId;
        p.VehicleId = param.VehicleId;
        p.VIN = param.VIN;
        p.RPM = param.RPM;
        p.Speed = param.Speed;
        p.MaxSpeed = param.MaxSpeed;
        p.HiResMaxSpeed = param.HiResMaxSpeed;
        p.Distance = param.Distance;
        p.HiResDistance = param.HiResDistance;
        p.LoResDistance = param.LoResDistance;
        p.Odometer = param.Odometer;
        p.HiResOdometer = param.HiResOdometer;
        p.LoResOdometer = param.LoResOdometer;
        p.TotalHours = param.TotalHours;
        p.IdleHours = param.IdleHours;
        p.PctLoad = param.PctLoad;
        p.PctTorque = param.PctTorque;
        p.DrvPctTorque = param.DrvPctTorque;
        p.TorqueMode = param.TorqueMode;
        p.FuelUsed = param.FuelUsed;
        p.HiResFuelUsed = param.HiResFuelUsed;
        p.IdleFuelUsed = param.IdleFuelUsed;
        p.FuelRate = param.FuelRate;
        p.AvgFuelEcon = param.AvgFuelEcon;
        p.InstFuelEcon = param.InstFuelEcon;
        p.PrimaryFuelLevel = param.PrimaryFuelLevel;
        p.SecondaryFuelLevel = param.SecondaryFuelLevel;
        p.OilTemp = param.OilTemp;
        p.OilPressure = param.OilPressure;
        p.TransTemp = param.TransTemp;
        p.IntakeTemp = param.IntakeTemp;
        p.IntakePressure = param.IntakePressure;
        p.CoolantTemp = param.CoolantTemp;
        p.CoolantLevel = param.CoolantLevel;
        p.CoolantPressure = param.CoolantPressure;
        p.BrakeAppPressure = param.BrakeAppPressure;
        p.Brake1AirPressure = param.Brake1AirPressure;
        p.Brake2AirPressure = param.Brake2AirPressure;
        p.AccelPedal = param.AccelPedal;
        p.ThrottlePos = param.ThrottlePos;
        p.BatteryPotential = param.BatteryPotential;
        p.SelectedGear = param.SelectedGear;
        p.CurrentGear = param.CurrentGear;
        p.Make = param.Make;
        p.Model = param.Model;
        p.SerialNo = param.SerialNo;
        p.UnitNo = param.UnitNo;
        p.EngineVIN = param.EngineVIN;
        p.EngineMake = param.EngineMake;
        p.EngineModel = param.EngineModel;
        p.EngineSerialNo = param.EngineSerialNo;
        p.EngineUnitNo = param.EngineUnitNo;
        p.ClutchSwitch = param.ClutchSwitch;
        p.BrakeSwitch = param.BrakeSwitch;
        p.ParkBrakeSwitch = param.ParkBrakeSwitch;
        p.CruiseSetSpeed = param.CruiseSetSpeed;
        p.CruiseOnOff = param.CruiseOnOff;
        p.CruiseSet = param.CruiseSet;
        p.CruiseCoast = param.CruiseCoast;
        p.CruiseResume = param.CruiseResume;
        p.CruiseAccel = param.CruiseAccel;
        p.CruiseActive = param.CruiseActive;
        p.CruiseState = param.CruiseState;
        p.FaultSource = param.FaultSource;
        p.FaultSPN = param.FaultSPN;
        p.FaultFMI = param.FaultFMI;
        p.FaultOccurrence = param.FaultOccurrence;
        p.FaultConversion = param.FaultConversion;
        p.Latitude = param.Latitude;
        p.Longitude = param.Longitude;
        p.ParameterDateTime = param.ParameterDateTime;
        p.AdapterId = param.AdapterId;
        p.FirmwareVersion = param.FirmwareVersion;
        p.HardwareVersion = param.HardwareVersion;
        p.AdapterSerialNo = param.AdapterSerialNo;
        p.HardwareType = param.HardwareType;
        p.IsKeyOn = param.IsKeyOn;
        p.SleepMode = param.SleepMode;
        p.LedBrightness = param.LedBrightness;
        p.Message = param.Message;
        p.Status = param.Status;


        p.AmbientTemp = param.AmbientTemp;
        p.DPFOutletTemp = param.DPFOutletTemp;
        p.EngineIntakeManifoldTemp = param.EngineIntakeManifoldTemp;
        p.DPFInletTemp = param.DPFInletTemp;
        p.EngineIntakeManifoldPressure = param.EngineIntakeManifoldPressure;
        p.DPFPressureDifferential = param.DPFPressureDifferential;
        p.EngineCrankcasePressure = param.EngineCrankcasePressure;
        p.EngineTurbochargerSpeed = param.EngineTurbochargerSpeed;
        p.FuelTemp = param.FuelTemp;
        p.SCRInletNox = param.SCRInletNox;
        p.SCROutletNox = param.SCROutletNox;
        p.TotalNoOfPassiveRegenerations = param.TotalNoOfPassiveRegenerations;
        p.DPFAshLoad = param.DPFAshLoad;
        p.TotalNoOfActiveRegenerations = param.TotalNoOfActiveRegenerations;
        p.DPFSootLoad = param.DPFSootLoad;
        p.BarometricPressure = param.BarometricPressure;
        p.FanState = param.FanState;

        p.PGN = param.PGN;
        p.PGNActualValue = param.PGNActualValue;
        p.PGNRawValue = param.PGNRawValue;
        p.VehicleSpeed = param.VehicleSpeed;
        p.FaultDescription = param.FaultDescription;
        p.isConnected = param.isConnected;
        return p;
    }


    /**
     * Add Fault Code
     *
     * @param fault
     * @return
     */
    public long addFault(FaultModel fault) {
        String currentDate = DateUtils.getLocalTimeString();

        FaultTable table = new FaultTable();

        table.fmi = fault.fmi;
        table.spn = fault.spn;
        table.unit = fault.unit;
        table.description = fault.description;
        table.currentDateTime = DateUtils.getLocalTimeString();

        List<DB_BASIC> list = dbHandler.GetAllTableData(FaultTable.class);

        if (list != null && !list.isEmpty() && list.size() > 0) {

            for (int i = 0; i < list.size(); i++) {
                FaultTable row = (FaultTable) list.get(i);

                //Update Fault If already exist with current date time.
                if (row.spn.equalsIgnoreCase(fault.spn) && row.fmi.equalsIgnoreCase(fault.fmi)) {
                    ContentValues cv = new ContentValues();
                    cv.put("fmi", row.fmi);
                    cv.put("spn", row.spn);
                    cv.put("unit", row.unit);
                    cv.put("description", row.description);
                    cv.put("currentDateTime", currentDate);
                    dbHandler.UpdateRow(FaultTable.class, cv, "ID=" + row.ID);
                    return row.ID;
                }

                //Remove record if more that 12 Hrs.
                if (CommonUtil.isDateDifferenceMoreThan12Hrs(currentDate, row.currentDateTime)) {
                    String[] params = {row.currentDateTime};
                    dbHandler.DeleteTableRow(FaultTable.class, "currentDateTime=?", params);
                }
            }
        }

        long id = dbHandler.AddNewObject(table);
        return id;
    }

    /**
     * Get Fault Code list
     *
     * @return List of FaultModel
     */
    public List<FaultModel> getFaultList() {

        List<DB_BASIC> faultList = dbHandler.GetAllTableData(FaultTable.class);

        List<FaultModel> result = new ArrayList<>();

        if (faultList != null && !faultList.isEmpty() && faultList.size() > 0) {
            for (int i = 0; i < faultList.size(); i++) {
                FaultTable row = (FaultTable) faultList.get(i);
                result.add(new FaultModel(row.fmi, row.spn, row.unit, row.description, row.currentDateTime));
            }
        }

        if (!result.isEmpty() && result.size() > 0)
            Collections.sort(result, Collections.reverseOrder());

        return result;
    }

    /**
     * Add all latitude and longitude to db
     *
     * @param TripId
     * @param latlong
     * @return
     */
    public boolean addLatLong(String tripId, LatLong latlong) {
        boolean isUpdated = false;
        String[] params = {tripId};
        List<DB_BASIC> currentLatlongList = dbHandler.GetTableRow(LatLongTable.class, "mTripId=?", params);


        Gson gson = new Gson();

        if (currentLatlongList != null && !currentLatlongList.isEmpty() && currentLatlongList.size() > 0) {
            //Updating locations
            Type type = new TypeToken<LatLong>() {
            }.getType();
            String jsonlatlong_update = gson.toJson(latlong, type);
            try {
                JsonParser parser = new JsonParser();
                LatLongTable table = new LatLongTable();
                table.mTripId = tripId;

                LatLongTable row = (LatLongTable) currentLatlongList.get(0);
                String latlongArray = row.latlongArray;
                if (!TextUtils.isEmpty(latlongArray)) {
                    JsonArray jarray = parser.parse(latlongArray).getAsJsonArray();
                    JsonObject objectFromString = parser.parse(jsonlatlong_update).getAsJsonObject();
                    jarray.add(objectFromString);
                    table.latlongArray = jarray.toString();
                }


                ContentValues cv = new ContentValues();
                cv.put("mTripId", tripId);
                cv.put("latlongArray", table.latlongArray);

                int affctedrow = dbHandler.UpdateRow(LatLongTable.class, cv, "mTripId='" + tripId + "'");
                if (affctedrow > 0) {
                    isUpdated = true;
                }
            } catch (Exception ex) {
                ex.getMessage();
            }
        } else {

            //Add latlong

            List<LatLong> temp_list = new ArrayList<LatLong>();
            temp_list.add(latlong);
            Type type = new TypeToken<List<LatLong>>() {
            }.getType();
            String jsonlatlong_add = gson.toJson(temp_list, type);
            //Adding locations
            LatLongTable table = new LatLongTable();
            table.mTripId = tripId;
            table.latlongArray = jsonlatlong_add;

            long id = dbHandler.AddNewObject(table);
            if (id > -1) {
                isUpdated = true;
            }
        }
        return isUpdated;
    }

    /**
     * DElete locations on trip stops
     *
     * @param id
     * @return
     */
    public long deleteLatLong(String tripid) {
        try {
            String[] params = {tripid};
            long result = dbHandler.DeleteTableRow(LatLongTable.class, "mTripId=?", params);
            return result;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Get latitude and longitude
     *
     * @param tripId
     * @return
     */
    public List<LatLong> getLatLongList(String tripId) {

        String[] ids = {tripId};
        List<DB_BASIC> currentLatlongList = dbHandler.GetTableRow(LatLongTable.class, "mTripId=?", ids);
        JsonParser parser = new JsonParser();
        Gson gson = new Gson();
        if (currentLatlongList != null && !currentLatlongList.isEmpty() && currentLatlongList.size() > 0) {
            List<LatLong> result = new ArrayList<>();

            LatLongTable row = (LatLongTable) currentLatlongList.get(0);
            String latlongArray = row.latlongArray;
            if (!TextUtils.isEmpty(latlongArray)) {
                JsonArray jarray = parser.parse(latlongArray).getAsJsonArray();
                if (jarray != null) {
                    for (int i = 0; i < jarray.size(); i++) {
                        LatLong model = gson.fromJson(jarray.get(i).getAsJsonObject(), LatLong.class);
                        result.add(model);
                    }
                    return result;
                }
            }

        }
        return null;

    }

    public void addNumberOfStops(String TripId) {
        String[] params = {TripId};
        List<DB_BASIC> currentLatlongList = dbHandler.GetTableRow(LatLongTable.class, "mTripId=?", params);
        int count = 0;
        try {
            if (currentLatlongList != null && !currentLatlongList.isEmpty() && currentLatlongList.size() > 0) {
                LatLongTable row = (LatLongTable) currentLatlongList.get(0);
                count = row.tripPauseCount;
                ContentValues cv = new ContentValues();
                cv.put("mTripId", TripId);
                cv.put("tripPauseCount", ++count);

                int affected = dbHandler.UpdateRow(LatLongTable.class, cv, "mTripId='" + TripId + "'");
                if (affected > 0) {
                    Log.d(TripActivity.class.getName(), "Stops updated" + affected);
                }
            } else {

                LatLongTable table = new LatLongTable();
                table.mTripId = TripId;
                table.tripPauseCount = ++count;

                long id = dbHandler.AddNewObject(table);
                if (id > -1) {
                    Log.d(TripActivity.class.getName(), "Stops Added at row" + id);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get Count of paused
     *
     * @param tripId
     * @return
     */
    public int getStopsCount(String tripId) {
        String[] ids = {tripId};
        List<DB_BASIC> currentLatlongList = dbHandler.GetTableRow(LatLongTable.class, "mTripId=?", ids);
        JsonParser parser = new JsonParser();
        Gson gson = new Gson();
        if (currentLatlongList != null && !currentLatlongList.isEmpty() && currentLatlongList.size() > 0) {
            List<LatLong> result = new ArrayList<>();

            LatLongTable row = (LatLongTable) currentLatlongList.get(0);
            int latlongArray = row.tripPauseCount;

            return latlongArray;
        } else {
            return 0;
        }
    }


}
