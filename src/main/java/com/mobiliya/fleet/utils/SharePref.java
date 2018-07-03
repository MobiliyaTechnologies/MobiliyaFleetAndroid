package com.mobiliya.fleet.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.mobiliya.fleet.models.DriverScore;
import com.mobiliya.fleet.models.LastTrip;
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.models.Vehicle;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Set;

/**
 * This is utility class to provide access to Shared Preferences. It provides
 * methods to add, edit and delete the preference values.
 *
 * @author Kunal
 */
public class SharePref {

    private static final String TAG = SharePref.class.getSimpleName();
    /**
     * Shared preference instance.
     */
    @SuppressLint("StaticFieldLeak")
    private static SharePref mPreferences;
    /**
     * Shared preferences.
     */
    private static SharedPreferences mSharedPrefs;

    @SuppressLint("StaticFieldLeak")
    private static Context mCtx;
    private String vehicleID;

    /**
     * Default constructor.
     *
     * @param context application context.
     */
    private SharePref(Context context) {
        mSharedPrefs = context.getSharedPreferences(
                SHARED_PREF_NAME/*Constants.PREF_NAME*/, Context.MODE_PRIVATE);
        mCtx = context;
    }

    /**
     * Get singleton instance of preferences.
     * <p/>
     * .
     *
     * @return SharePointPreferences.
     */
    public static SharePref getInstance(Context context) {
        if (mPreferences == null) {
            mPreferences = new SharePref(context);
        }
        return mPreferences;
    }


    /**
     * Add item to Shared Preferences.
     *
     * @param name  item name.
     * @param value item value.
     */
    public void addItem(String name, String value) {
        LogUtil.d(TAG, "act_changes addItem name " + name
                + " value{" + value + "}");
        Editor mSharedPrefsEditor = mSharedPrefs.edit();
        mSharedPrefsEditor.putString(name, value);
        mSharedPrefsEditor.apply();
    }

    /**
     * Add item to Shared Preferences.
     *
     * @param name  item name.
     * @param value item value.
     */
    public void addItem(String name, Boolean value) {
        LogUtil.d(TAG, "act_changes addItem name " + name
                + " value{" + value + "}");
        Editor mSharedPrefsEditor = mSharedPrefs.edit();
        mSharedPrefsEditor.putBoolean(name, value);
        mSharedPrefsEditor.apply();
    }

    /**
     * Add item to Shared Preferences.
     *
     * @param name  item name.
     * @param value item value.
     */
    public void addItem(String name, int value) {
        LogUtil.d(TAG, "act_changes addItem name " + name + " value{" + value + "}");
        Editor mSharedPrefsEditor = mSharedPrefs.edit();
        mSharedPrefsEditor.putInt(name, value);
        mSharedPrefsEditor.apply();
    }

    /**
     * Add item to Shared Preferences.
     *
     * @param name  item name.
     * @param value item value.
     */
    @SuppressWarnings("SameParameterValue")
    public void addItem(String name, float value) {
        LogUtil.d(TAG, "act_changes addItem name " + name
                + " value{" + value + "}");
        Editor mSharedPrefsEditor = mSharedPrefs.edit();
        mSharedPrefsEditor.putFloat(name, value);
        mSharedPrefsEditor.apply();
    }

    /**
     * Get value for given item from Shared Preferences.
     *
     * @param name item name.
     * @return item value.
     */
    public String getItem(String name) {
        return mSharedPrefs.getString(name, null);
    }

    /**
     * Get value for given item from Shared Preferences.
     *
     * @param name item name.
     * @return item value.
     */
    public String getItem(String name, @SuppressWarnings("SameParameterValue") String defaultValue) {
        return mSharedPrefs.getString(name, defaultValue);
    }

    /**
     * Get value for given item from Shared Preferences.
     *
     * @param name item name.
     * @return item value.
     */
    public int getItem(String name, int defaultValue) {
        return mSharedPrefs.getInt(name, defaultValue);
    }

    /**
     * Get value for given item from Shared Preferences.
     *
     * @param name item name.
     * @return item value.
     */
    public Float getItem(@SuppressWarnings("SameParameterValue") String name, @SuppressWarnings("SameParameterValue") Float defaultValue) {
        return mSharedPrefs.getFloat(name, defaultValue);
    }

    /**
     * Get value for given item from Shared Preferences.
     *
     * @param name item name.
     * @return item value.
     */
    public Boolean getBooleanItem(String name) {
        return mSharedPrefs.getBoolean(name, false);
    }

    public Boolean getBooleanItem(String name, boolean defaultValue) {
        return mSharedPrefs.getBoolean(name, defaultValue);
    }

    public void addListItem(String name, Set<String> set) {

        Editor mSharedPrefsEditor = mSharedPrefs.edit();
        mSharedPrefsEditor.putStringSet(name, set);
        mSharedPrefsEditor.apply();

    }

    public Set<String> getListItem(String name) {

        return mSharedPrefs.getStringSet(name, null);


    }

    /**
     * Removes item from Shared Preferences.
     *
     * @param name item name.
     */
    public void removeItem(String name) {
        LogUtil.d(TAG, "act_changes removeItem name " + name);
        Editor mSharedPrefsEditor = mSharedPrefs.edit();
        mSharedPrefsEditor.remove(name);
        mSharedPrefsEditor.apply();
    }

    /**
     * Update item value in Shared Preferences.
     *
     * @param name  item name.
     * @param value item value.
     */
    public void updateItem(String name, String value) {
        Editor mSharedPrefsEditor = mSharedPrefs.edit();
        mSharedPrefsEditor.putString(name, value);
        mSharedPrefsEditor.apply();
    }

    /**
     * Clears the whole preferences. This will be called when user logs out of
     * application.
     */
    public void clearPreferences() {
        Editor edit = mSharedPrefs.edit();
        edit.clear().apply();
    }

    //this method will give the logged in user details
    public User getUser() {
        return new User(
                mPreferences.getItem(Constants.KEY_ID, ""),
                mPreferences.getItem(Constants.PREF_EMAIL, ""),
                mPreferences.getItem(Constants.KEY_FIRSTNAME, ""),
                mPreferences.getItem(Constants.KEY_LASTNAME, ""),
                mPreferences.getItem(Constants.KEY_MOBILENO, ""),
                mPreferences.getItem(Constants.KEY_STATUS, -1),
                mPreferences.getItem(Constants.KEY_ROLEID, ""),
                mPreferences.getItem(Constants.KEY_FLEETID, ""),
                mPreferences.getItem(Constants.TENANT_ID, "")
                //mPreferences.getItem(Constants.PREF_PASSWORD, "")
        );
    }

    public User convertUserResponse(JSONObject responseData) {
        User user = null;
        try {
            user = new User(
                    responseData.getString("id"),
                    responseData.getString("email"),
                    responseData.getString("firstName"),
                    responseData.getString("lastName"),
                    responseData.getString("mobileNumber"),
                    responseData.getInt("status"),
                    responseData.getString("roleId"),
                    responseData.getString("fleetId"),
                    responseData.getString("tenantId")
                    //responseData.getString("password")
            );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return user;
    }

    public Vehicle convertVehicleResponse(JSONObject responseData) {
        Vehicle vehicle = new Vehicle();
        try {
            vehicle.setId(responseData.getString("id"));
            vehicle.setBrandName(responseData.getString("brandName"));
            vehicle.setModel(responseData.getString("model"));
            vehicle.setFuleType(responseData.getString("fuelType"));
            vehicle.setRegistrationNo(responseData.getString("registrationNumber"));
            vehicle.setYearOfManufacture(responseData.getString("yearOfManufacture"));
            try {
                vehicle.setDeviceId(responseData.getString("deviceId"));
            }catch (Exception e){
                e.printStackTrace();
            }
            vehicle.setIsDeleted(responseData.getInt("isDeleted"));
            vehicle.setStatus(responseData.getString("status"));
            vehicle.setCreatedAt(responseData.getString("createdAt"));
            vehicle.setUpdatedAt(responseData.getString("updatedAt"));
            vehicle.setUserId(responseData.getString("userId"));
            vehicle.setVehicleColor(responseData.getString("color"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return vehicle;
    }

    public void setVehicleData(Vehicle vehicle) {
        Editor mPreferences = mSharedPrefs.edit();
        mPreferences.putString(Constants.KEY_VEHICLE_ID, vehicle.getId());
        mPreferences.putString(Constants.KEY_VEHICLE_REGISTRATION_NO, vehicle.getRegistrationNo());
        mPreferences.putString(Constants.KEY_VEHICLE_BRAND, vehicle.getBrandName());
        mPreferences.putString(Constants.KEY_VEHICLE_MODEL, vehicle.getModel());
        mPreferences.putString(Constants.KEY_VEHICLE_FUEL_TYPE, vehicle.getFuleType());
        mPreferences.putString(Constants.KEY_VEHICLE_YEAR_MANUFACTURE, vehicle.getYearOfManufacture());
        mPreferences.putString(Constants.KEY_VEHICLE_COLOR, vehicle.getVehicleColor());
        mPreferences.apply();
    }

    public Vehicle getVehicleData() {
        Vehicle vehicle = new Vehicle();
        try {
            vehicle.setId(getItem(Constants.KEY_VEHICLE_ID));
            vehicle.setBrandName(getItem(Constants.KEY_VEHICLE_BRAND));
            vehicle.setModel(getItem(Constants.KEY_VEHICLE_MODEL));
            vehicle.setFuleType(getItem(Constants.KEY_VEHICLE_FUEL_TYPE));
            vehicle.setRegistrationNo(getItem(Constants.KEY_VEHICLE_REGISTRATION_NO));
            vehicle.setYearOfManufacture(getItem(Constants.KEY_VEHICLE_YEAR_MANUFACTURE));
            vehicle.setVehicleColor(getItem(Constants.KEY_VEHICLE_COLOR));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return vehicle;
    }

    public void setUserData(User user) {
        Editor mPreferences = mSharedPrefs.edit();
        mPreferences.putString(Constants.KEY_ID, user.getId());
        mPreferences.putString(Constants.PREF_EMAIL, user.getEmail());
        mPreferences.putString(Constants.KEY_FIRSTNAME, user.getFirstName());
        mPreferences.putString(Constants.KEY_LASTNAME, user.getLastName());
        mPreferences.putString(Constants.KEY_MOBILENO, user.getMobileNumber());
        mPreferences.putInt(Constants.KEY_STATUS, user.getStatus());
        mPreferences.putString(Constants.KEY_ROLEID, user.getRoleId());
        mPreferences.putString(Constants.KEY_FLEETID, user.getFleetId());
        //mPreferences.putString(Constants.PREF_PASSWORD, CommonUtil.decodeString(user.getPassword()));
        mPreferences.putString(Constants.TENANT_ID, user.getTenantId());
        mPreferences.apply();
    }



   /* public void setRunningTrip(Trip trip) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putLong(TRIP_KEY_ID, trip.ID);
        //editor.putLong(TRIP_KEY_SELF_TRIP_ID, trip.SelfTripId);
        editor.putLong(TRIP_KEY_TRIP_ID, trip._id);
        editor.putString(TRIP_KEY_NAME, trip.tripName);
        editor.putString(TRIP_KEY_DESCRIPTION, trip.description);
        editor.putLong(TRIP_KEY_STARTTIME, trip.startTime);
        editor.putLong(TRIP_KEY_ENDTIME, trip.endTime);
        editor.putString(TRIP_KEY_START_LOCATION, trip.startLocation);
        editor.putString(TRIP_KEY_END_LOCATION, trip.endLocation);
        editor.putInt(TRIP_KEY_STATUS, trip.status);
        editor.apply();
    }

    public void deleteTrip() {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putLong(TRIP_KEY_ID, -1);
        editor.putLong(TRIP_KEY_SELF_TRIP_ID, -1);
        editor.putLong(TRIP_KEY_TRIP_ID, -1);
        editor.putString(TRIP_KEY_NAME, "");
        editor.putString(TRIP_KEY_DESCRIPTION, "");
        editor.putLong(TRIP_KEY_STARTTIME, -1);
        editor.putLong(TRIP_KEY_ENDTIME, -1);
        editor.putString(TRIP_KEY_START_LOCATION, "");
        editor.putString(TRIP_KEY_END_LOCATION, "");
        editor.putInt(TRIP_KEY_STATUS, -1);
        editor.apply();
    }*/


    //the constants
    private static final String SHARED_PREF_NAME = "simplifiedcodingsharedpref";
    private static final String REMEMBER_PREF_USER = "remember_user_pref";
    private static final String KEY_FIRSTNAME = "keyfirstname";
    private static final String KEY_LASTNAME = "keylastname";
    private static final String KEY_EMAIL = "keyemail";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_MOBILENO = "keymobile";
    private static final String KEY_STATUS = "keystatus";
    private static final String KEY_ROLEID = "keyroleid";
    private static final String KEY_TENANTID = "keytenantid";
    private static final String KEY_TOKEN = "keytoken";
    private static final String KEY_ID = "keyid";
    private static final String KEY_VEHICLE_NO = "vehicleno";
    private static final String KEY_VEHICLE_ID = "vehicleid";
    private static final String KEY_TRIP_ID = "tripid";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";

    private static final String TRIP_KEY_ID = "trip_local_db_id";
    private static final String TRIP_KEY_SELF_TRIP_ID = "trip_self_trip_id";
    private static final String TRIP_KEY_TRIP_ID = "trip_server_db_id";
    private static final String TRIP_KEY_NAME = "tripname";
    private static final String TRIP_KEY_DESCRIPTION = "tripdesc";
    private static final String TRIP_KEY_STARTTIME = "tripstarttime";
    private static final String TRIP_KEY_ENDTIME = "tripendtime";
    private static final String TRIP_KEY_START_LOCATION = "trip_start_location";
    private static final String TRIP_KEY_END_LOCATION = "trip_end_location";
    private static final String TRIP_KEY_STATUS = "trip_status";

    public static final String CAB_INFO = "Cab Information";
    public static final String ENG_INFO = "Engine Information";
    public static final String ENG_INFO2 = "Engine Information2";
    public static final String TRANS_INFO = "Transmission Information";
    // public static final String  BRAKE_INFO="Brake Information";
    // public static final String CRUS_INFO="Cruise Control Information";
    public static final String VIN = "Engine Vin";
    public static final String ENG_TORQ = "Engine Reference Torq";


    public static final String CRUS_SET = "Cruise set";
    public static final String CRUS_SETSPEED = "Cruise set speed";
    public static final String CRUS_ONOFF = "Cruise on off";
    public static final String CRUS_COAST = "Cruise Coast";
    public static final String CRUS_RESUME = "Cruise Resume";
    public static final String CRUS_ACCEL = "Cruise Accel";
    public static final String CRUS_ACTIVE = "Cruise Active";
    public static final String CRUS_STATE = "Cruise State";

    public static final String CON_STATUS = "Connection status";
    public static final String ADAP_VER = "Adapter Version";
    public static final String ADAP_SLEEP = "Adapter sleep mode";
    public static final String ADAP_LED_BRI = "Adapter Led Brightness";
    public static final String ADAP_NAME = "Adapter name";
    public static final String ADAP_PASS = "Adapter Password";
    public static final String ADAP_ERR = "Adapter error message";

    public static final String ENG_RPM = "Engine RPM";
    public static final String SPEED = "Vehicle speed";
    public static final String PLOAD = "Percent Load";
    public static final String PTORQ = "Percent Torqu";
    public static final String DPTORQ = "Driver Percent Torqu";
    public static final String THROTTLEPOSI = "Throttle position";
    public static final String BATTERYCHARGING = "Battery Charging";


    public static final String ODOMETER = "Truck odometer";
    public static final String TOTALFUELUSED = "Total fuel used";
    public static final String TOTALIDLEFUELUSED = "Total idel fuel used";
    public static final String AVG_FUEL_ECONOMY = "Average fuel economy";
    public static final String INST_FUEL_ECONOMY = "Instant fuel economy";
    public static final String FUEL_RATE = "fuel rate";
    public static final String TOTAL_ENG_HRS = "Total Engine Hours";
    public static final String TOTAL_ENG_IDLE_HRS = "Total Engine idle Hours";
    public static final String FUEL_LEVEL = "Fuel Level";
    public static final String COOLANT_LEVEL = "Cooleant level";
    public static final String FAN_STATE = "Fan state";
    public static final String FAN_STATE_HEX = "Fan_state_Hex";
    public static final String ACC_PEDEL_POSI = "Accelerator Pedel Positoion";
    public static final String PARK_SETT = "Parking setting switch";
    public static final String BRAKE_SETT = "Brake setting switch";
    public static final String CLUTH_SETT = "Cluth setting switch";

    public static final String ENG_DIST = "Engine Distance";
    public static final String GPS_LOCATION = "Gps Location";
    public static final String BRAKE_AIR_PRESSURE = "Brake Air Pressure";

    public static final String OIL_TEMP = "Oil Temperature";
    public static final String COOLANT_TEMP = "Coolant Temperature";
    public static final String INTAKE_AIR_TEMP = "Intake air Temperature";
    public static final String TANS_TEMP = "Tranmission air Temperature";
    public static final String OIL_PRESSURE = "Oil Pressure";
    public static final String INTAKE_AIR_PRE = "Intake air pressure";

    public static final String FAULT_SOURCE = "Fault Source";
    public static final String FAULT_SPN = "Fault SPN";
    public static final String FAULT_FMI = "Fault FMI";
    public static final String FAULT_OCCUR = "Fault Occurrence";
    public static final String FAULT_CONVER = "Fault Conversion";

    public static final String FAULT_ALERT_COUNT = "FaultCount";

    public static final String DEVICE_CONNECTION = "Device connection";

    public static final String DEVICE_TYPE_J1939 = "J1939";
    public static final String DEVICE_TYPE_J1708 = "J1708";
    private static final String KEY_VIN = "Vin";
    private static final String KEY_VEHICLE_NUM = "Vehicle number";
    private static final String KEY_EMAIL_R = "remember_email";
    private static final String KEY_PASS_R = "remember_password";
    private static final String KEY_SAVE_LOGIN = "save_login";

    public static final String BAROMETRIC_PRE = "Barometric";
    public static final String BAROMETRIC_PRE_HEX = "Barometric_Hex";
    public static final String FUEL_TMP = "Fuel temperature";
    public static final String FUEL_TMP_HEX = "Fuel_temperature_Hex";
    public static final String ENG_INTAKEMANIFOLD_PRESS = "engine_intake_manifold pressure";
    public static final String ENG_INTAKEMANIFOLD_PRESS_HEX = "engine_intake_manifold_pressure_Hex";
    public static final String ENG_INTAKEMANIFOLD_TEMP = "engine intake manifold temperature";
    public static final String ENG_INTAKEMANIFOLD_TEMP_HEX = "engine_intake_manifold_temperature_Hex";
    public static final String AMBIENT_TEMP = "Ambient temperature";
    public static final String AMBIENT_TEMP_HEX = "Ambient_temperature_Hex";
    public static final String ENG_TURBO_SPEED = "Engine turbo speed";
    public static final String ENG_TURBO_SPEED_HEX = "Engine_turbo_speed_Hex";
    public static final String ENG_CRANK_PRESSURE = "Engine crank case pressure";
    public static final String ENG_CRANK_PRESSURE_HEX = "Engine_crank_case_pressure_Hex";
    public static final String TOT_ACT_REGEN = "Total num of active regeneration";
    public static final String TOT_ACT_REGEN_HEX = "Total_num_of_active_regeneration_Hex";
    public static final String TOT_PASIV_REGEN = "Total num of passive regeneration";
    public static final String TOT_PASIV_REGEN_HEX = "Total_num_of_passive_regeneration_Hex";
    public static final String DPF_PREE_DIFF = "Dpf pressure differential";
    public static final String DPF_PREE_DIFF_HEX = "Dpf_pressure_differential_Hex";
    public static final String DPF_INLET_TEMP = "Dpf inlet temperature";
    public static final String DPF_INLET_TEMP_HEX = "Dpf_inlet_temperature_Hex";
    public static final String DPF_OUTLET_TEMP = "Dpf outlet temperature";
    public static final String DPF_OUTLET_TEMP_HEX = "Dpf_outlet_temperature_Hex";
    public static final String DPF_SOOT_LOAD = "Dpf soot load";
    public static final String DPF_SOOT_LOAD_HEX = "Dpf_soot_load_Hex";
    public static final String DPF_ASH_LOAD = "Dpf ash load";
    public static final String DPF_ASH_LOAD_HEX = "Dpf_ash_load_Hex";
    public static final String SCR_INLETNOX = "Scr inlet nox";
    public static final String SCR_INLETNOX_HEX = "Scr_inlet_nox_Hex";
    public static final String SCR_OUTLETNOX = "Scr outlet nox";
    public static final String SCR_OUTLETNOX_HEX = "Scr_outlet_nox_Hex";
    public static final String KEY_AUTO_START = "isTripAutostart";
    public static final String ENGINE_VIN = "Engine vin";
    private static final String KEY_VEHICLE_MAKE = "Vehicle Make";
    private static final String KEY_VEHICLE_MODEL = "Vehicle Model";
    private static final String KEY_DATA_SYNC = "Datasync";
    private static final String KEY_DB_STORAGE = "Database storage";
    private static String VehicleNumber;

    public void setAdapterData() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(CAB_INFO, true);
        editor.putBoolean(ENG_INFO, true);
        editor.putBoolean(ENG_INFO2, true);
        editor.putBoolean(TRANS_INFO, true);

        editor.putBoolean(CRUS_SET, true);
        editor.putBoolean(CRUS_SETSPEED, true);
        editor.putBoolean(CRUS_ONOFF, true);
        editor.putBoolean(CRUS_COAST, true);
        editor.putBoolean(CRUS_RESUME, true);
        editor.putBoolean(CRUS_ACCEL, true);
        editor.putBoolean(CRUS_ACTIVE, true);
        editor.putBoolean(CRUS_STATE, true);

        editor.putBoolean(ENGINE_VIN, true);
        editor.putBoolean(VIN, true);
        editor.putBoolean(ENG_TORQ, true);

        editor.putBoolean(CON_STATUS, true);
        editor.putBoolean(ADAP_VER, true);
        editor.putBoolean(ADAP_SLEEP, true);
        editor.putBoolean(ADAP_LED_BRI, true);
        editor.putBoolean(ADAP_NAME, true);
        editor.putBoolean(ADAP_PASS, true);
        editor.putBoolean(ADAP_ERR, true);

        editor.putBoolean(ENG_RPM, true);
        editor.putBoolean(SPEED, true);
        editor.putBoolean(PLOAD, true);
        editor.putBoolean(PTORQ, true);
        editor.putBoolean(DPTORQ, true);
        editor.putBoolean(THROTTLEPOSI, true);
        editor.putBoolean(BATTERYCHARGING, true);

        editor.putBoolean(ODOMETER, true);
        editor.putBoolean(TOTALFUELUSED, true);
        editor.putBoolean(TOTALIDLEFUELUSED, true);
        editor.putBoolean(AVG_FUEL_ECONOMY, true);
        editor.putBoolean(INST_FUEL_ECONOMY, true);
        editor.putBoolean(FUEL_RATE, true);
        editor.putBoolean(TOTAL_ENG_HRS, true);
        editor.putBoolean(TOTAL_ENG_IDLE_HRS, true);
        editor.putBoolean(FUEL_LEVEL, true);
        editor.putBoolean(COOLANT_LEVEL, true);
        editor.putBoolean(FAN_STATE, true);
        editor.putBoolean(ACC_PEDEL_POSI, true);
        editor.putBoolean(PARK_SETT, true);
        editor.putBoolean(CLUTH_SETT, true);
        editor.putBoolean(BRAKE_SETT, true);
        editor.putBoolean(ENG_DIST, true);
        editor.putBoolean(GPS_LOCATION, true);
        editor.putBoolean(BRAKE_AIR_PRESSURE, true);

        editor.putBoolean(OIL_TEMP, true);
        editor.putBoolean(COOLANT_TEMP, true);
        editor.putBoolean(INTAKE_AIR_TEMP, true);
        editor.putBoolean(TANS_TEMP, true);
        editor.putBoolean(OIL_PRESSURE, true);
        editor.putBoolean(INTAKE_AIR_PRE, true);

        editor.putBoolean(FAULT_SOURCE, true);
        editor.putBoolean(FAULT_SPN, true);
        editor.putBoolean(FAULT_FMI, true);
        editor.putBoolean(FAULT_OCCUR, true);
        editor.putBoolean(FAULT_CONVER, true);

        editor.putBoolean(BAROMETRIC_PRE, true);
        editor.putBoolean(FUEL_TMP, true);
        editor.putBoolean(ENG_INTAKEMANIFOLD_PRESS, true);
        editor.putBoolean(ENG_INTAKEMANIFOLD_TEMP, true);
        editor.putBoolean(AMBIENT_TEMP, true);
        editor.putBoolean(ENG_TURBO_SPEED, true);
        editor.putBoolean(ENG_CRANK_PRESSURE, true);
        editor.putBoolean(TOT_ACT_REGEN, true);
        editor.putBoolean(TOT_PASIV_REGEN, true);
        editor.putBoolean(DPF_PREE_DIFF, true);
        editor.putBoolean(DPF_INLET_TEMP, true);
        editor.putBoolean(DPF_OUTLET_TEMP, true);
        editor.putBoolean(DPF_SOOT_LOAD, true);
        editor.putBoolean(DPF_ASH_LOAD, true);
        editor.putBoolean(SCR_INLETNOX, true);
        editor.putBoolean(SCR_OUTLETNOX, true);
        editor.apply();
    }

    public void updateAdapterData(String Key, boolean value) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(Key, value);
        editor.apply();

    }

    public boolean getAdapterData(String key) {
        SharedPreferences prefs = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(key, false);

    }

    public boolean isDataExist(String key) {
        SharedPreferences prefs = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.contains(key);

    }

    public void storeToken(String tokenStr) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_TOKEN, tokenStr);
        editor.apply();
    }

    public String getToken() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public void saveDeviceConnection(boolean isdeviceConnected) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DEVICE_CONNECTION, isdeviceConnected);
        editor.apply();
    }

    public boolean getDeviceConnection() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(DEVICE_CONNECTION, false);
    }

    public void setVehicleNumber(String vehicleNumber) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_VEHICLE_NUM, vehicleNumber);
        editor.apply();
    }


    public void setVehicleMake(String vehicleMake) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_VEHICLE_MAKE, vehicleMake);
        editor.apply();
    }

    public void setVehicleModel(String vehicleModel) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_VEHICLE_MODEL, vehicleModel);
        editor.apply();
    }

    public String getVehicleMake() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_VEHICLE_MAKE, null);
    }

    public String getVehicleModel() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_VEHICLE_MODEL, null);
    }

    public String getVehicleNumber() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_VEHICLE_NUM, null);
    }


    public void setVehicleID(String VehicleId) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_VEHICLE_ID, VehicleId);
        editor.apply();
    }

    public void setTripID(int tripId) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_TRIP_ID, tripId);
        editor.apply();
//            vehicleNumber=VehicleNumber;
    }

    public int getTripID() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_TRIP_ID, -1);
    }

    public void setDeviceType(String key, boolean type) {
        SharedPreferences
                sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, type);
        editor.apply();
    }

    public boolean getDeviceType(String key) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(key, false);
    }


   /* public Trip getRunningTrip() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        Trip trip = new Trip();

        trip.ID = sharedPreferences.getLong(TRIP_KEY_ID, -1);
        trip.SelfTripId = sharedPreferences.getLong(TRIP_KEY_SELF_TRIP_ID, -1);
        trip.TripId = sharedPreferences.getLong(TRIP_KEY_TRIP_ID, -1);
        trip.TripName = sharedPreferences.getString(TRIP_KEY_NAME, null);
        trip.Description = sharedPreferences.getString(TRIP_KEY_DESCRIPTION, null);
        trip.StartTime = sharedPreferences.getLong(TRIP_KEY_STARTTIME, new Date().getTime());
        trip.EndTime = sharedPreferences.getLong(TRIP_KEY_ENDTIME, new Date().getTime());
        trip.StartLocation = sharedPreferences.getString(TRIP_KEY_START_LOCATION, null);
        trip.EndLocation = sharedPreferences.getString(TRIP_KEY_END_LOCATION, null);
        trip.VehicleID = sharedPreferences.getString(KEY_VEHICLE_ID, "");
        trip.TenantId = sharedPreferences.getString(KEY_TENANTID, "");
        trip.Status = sharedPreferences.getInt(TRIP_KEY_STATUS, TripStatus.Stop.getValue());

        return trip;
    }*/


    public void setPGNNumber(String PGN) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("PGN_NUMBER", PGN);
        editor.apply();
    }

    public String getPGNNumber() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString("PGN_NUMBER", null);
    }

    public void setTripAutoStartStatus(boolean checked) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_AUTO_START, checked);
        editor.apply();
    }

    public Boolean canTripAutostart() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(KEY_AUTO_START, false);
    }

    public void saveDataSyncTime(String time) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_DATA_SYNC, time);
        editor.apply();
    }

    public String getDataSyncTime() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_DATA_SYNC, "1 min");
    }

    public void setMemorySize(int size) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_DB_STORAGE, size);
        editor.apply();
    }

    public int getMemorySize() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(KEY_DB_STORAGE, 0);
    }

    public void setFaultAlertCount(int count) {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(FAULT_ALERT_COUNT, count);
        editor.apply();
    }

    public int getFaultAlertCount() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(FAULT_ALERT_COUNT, 0);
    }


    public String getVehicleID() {
        SharedPreferences sharedPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_VEHICLE_ID, null);
    }

    public void saveDriverScore(List<DriverScore> tList) {
        int overSpeeding = 0, hardBraking = 0, aggresiveAcc = 0, vehicleStop = 0, driverScore = 0;
        int cnt = 0;
        try {
            for (DriverScore score :
                    tList) {
                if (score.driverBehaviour != null) {
                    overSpeeding = overSpeeding + score.driverBehaviour.overSpeeding;
                    hardBraking = hardBraking + score.driverBehaviour.hardBraking;
                    aggresiveAcc = hardBraking + score.driverBehaviour.aggressiveAccelerator;
                    vehicleStop = vehicleStop + score.driverBehaviour.vehicleStops;
                    driverScore = driverScore + score.driverBehaviour.driverScore;
                    cnt++;
                }
            }
            overSpeeding = overSpeeding / cnt;
            hardBraking = hardBraking / cnt;
            aggresiveAcc = aggresiveAcc / cnt;
            vehicleStop = vehicleStop ;
            driverScore = driverScore / cnt;

            Editor mPreferences = mSharedPrefs.edit();
            mPreferences.putString(Constants.KEY_OVER_SPEEDING, Integer.toString(overSpeeding));
            mPreferences.putString(Constants.KEY_HARD_BRAKING, Integer.toString(hardBraking));
            mPreferences.putString(Constants.KEY_AGGRESSIVE_ACCELERATOR, Integer.toString(aggresiveAcc));
            mPreferences.putString(Constants.KEY_VEHICLE_STOPS, Integer.toString(vehicleStop));
            mPreferences.putString(Constants.KEY_DRIVER_SCORE, Integer.toString(driverScore));
            mPreferences.apply();
        } catch (Exception e) {
            LogUtil.d(TAG, "Error while saving score into pref");
        }
    }

    public DriverScore getDriverScore() {

        DriverScore score = new DriverScore();
        score.driverBehaviour = score.new behaviour();
        SharedPreferences mPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        score.driverBehaviour.overSpeeding = Integer.parseInt(mPreferences.getString(Constants.KEY_OVER_SPEEDING, "0"));
        score.driverBehaviour.hardBraking = Integer.parseInt(mPreferences.getString(Constants.KEY_HARD_BRAKING, "0"));
        score.driverBehaviour.aggressiveAccelerator = Integer.parseInt(mPreferences.getString(Constants.KEY_AGGRESSIVE_ACCELERATOR, "0"));
        score.driverBehaviour.vehicleStops = Integer.parseInt(mPreferences.getString(Constants.KEY_VEHICLE_STOPS, "0"));
        score.driverBehaviour.driverScore = Integer.parseInt(mPreferences.getString(Constants.KEY_DRIVER_SCORE, "0"));
        return score;
    }

    public void saveLastTrip(LastTrip lastTrip) {
        Editor mPreferences = mSharedPrefs.edit();
        mPreferences.putString(Constants.KEY_MILES, lastTrip.miles);
        mPreferences.putString(Constants.KEY_TRIP_DURATION, lastTrip.tripDuration);
        mPreferences.putString(Constants.KEY_START_LOCATION, lastTrip.startLocation);
        mPreferences.putString(Constants.KEY_END_LOCATION, lastTrip.endLocation);
        mPreferences.putString(Constants.KEY_START_TIME, lastTrip.startTime);
        mPreferences.putString(Constants.KEY_END_TIME, lastTrip.endTime);
        mPreferences.putString(Constants.KEY_TRIP_DATE_TIME, lastTrip.tripDateTime);
        mPreferences.apply();
    }

    public LastTrip getLastTrip() {
        LastTrip lastTrip = new LastTrip();
        SharedPreferences mPreferences = mCtx.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        lastTrip.miles = mPreferences.getString(Constants.KEY_MILES, "0");
        lastTrip.startLocation = mPreferences.getString(Constants.KEY_START_LOCATION, "0");
        lastTrip.endLocation = mPreferences.getString(Constants.KEY_END_LOCATION, "0");
        lastTrip.startTime = mPreferences.getString(Constants.KEY_START_TIME, "0");
        lastTrip.endTime = mPreferences.getString(Constants.KEY_END_TIME, "0");
        lastTrip.tripDateTime = mPreferences.getString(Constants.KEY_TRIP_DATE_TIME, "0");
        lastTrip.tripDuration = mPreferences.getString(Constants.KEY_TRIP_DURATION, "0");
        return lastTrip;
    }
}