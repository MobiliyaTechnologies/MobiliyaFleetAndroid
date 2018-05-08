package com.mobiliya.fleet.utils;

import android.annotation.SuppressLint;

@SuppressWarnings({"ALL", "unused"})
public class Constants {

    final public static String PROTOCOL = "PROTOCOL";
    final public static String OBD = "OBD2";
    final public static String J1939 = "J1939";


    public static final String IOT_PROD_CONNSTRING = "HostName=MSFleetIotHub.azure-devices.net;DeviceId=mobileToIotHubDev;SharedAccessKey=7SEQU8wY7BqnUkJtTLSJ8nZqs64RGaclqhcM+2Yqt0k=";
    public static final String IOT_NEW_CONNECTION = "HostName=MSFleetIotHub.azure-devices.net;DeviceId=mobileToIotHubDev;SharedAccessKey=ZfAVd9KW3OrErrbv3xoZJt5Jj1CISzkkVrEhMoL6gdM=";

    public static final String IOT_CONNSTRING = IOT_NEW_CONNECTION;


    public static final String PREF_NAME = "UserSessionInfo";
    public static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String PROD_URL = "https://cloudprod.azurewebsites.net//"; //Production
    private static final String QA_URL = "https://cloudqa.azurewebsites.net//"; //QA

    private static final String IDENTITY_DEV_URL = "http://identity-service.azurewebsites.net/";//Dev
    public static final String FLEET_DEV_URL = "http://fleet-service.azurewebsites.net/";//Dev
    public static final String VEHICLES = "/vehicles";
    public static final String REGISTRATION_NUMBER = "?registrationNumber=";


    public static final String TRIP_SERVICE_BASEURL_PROD = "http://trip-service.azurewebsites.net/";


    public static final String BASE_URL = IDENTITY_DEV_URL;
    public static final String LOGIN_URL = BASE_URL + "login";
    public static final String FORGOT_PASSWORD_URL = BASE_URL + "forgot-password";
    public static final String GET_USER_URL = BASE_URL + "users";

    public static final String PREF_EMAIL = "email";
    public static final String PREF_PASSWORD = "password";
    public static final String TENANT_ID = "tenantId";
    public static final String PREF_USER_LOGED_IN = "isUSerLogedIn";
    public static final String PREF_ACCESS_TOKEN = "accessToken";
    public static final String PREF_EXPIRES = "expires";

    public static final String PREF_BT_DEVICE_ADDRESS = "btDeviceAddress";
    public static final String PREF_BT_DEVICE_NAME = "btDeviceName";
    public static final String PREF_ADAPTER_PROTOCOL = "deviceAdapterProtocol";
    public static final String PREF_MOVED_TO_DASHBOARD = "movedToDashBoard";
    public static final String STATUS = "status";


    public static final String KEY_ID = "keyId";
    public static final String KEY_FIRSTNAME = "keyfirstName";
    public static final String KEY_LASTNAME = "keyLastName";
    public static final String KEY_MOBILENO = "keyMobileNo";
    public static final String KEY_STATUS = "keyStatus";
    public static final String KEY_ROLEID = "keyRoleId";
    public static final String KEY_FLEETID = "keyFleetId";
    public static final String KEY_TENANTID = "keyTenantId";


    public static final String TRIP_KEY_ID = "trip_local_db_id";
    public static final String TRIP_KEY_SELF_TRIP_ID = "trip_self_trip_id";
    public static final String TRIP_KEY_TRIP_ID = "trip_server_db_id";
    public static final String TRIP_KEY_NAME = "tripname";
    public static final String TRIP_KEY_DESCRIPTION = "tripdesc";
    public static final String TRIP_KEY_STARTTIME = "tripstarttime";
    public static final String TRIP_KEY_ENDTIME = "tripendtime";
    public static final String TRIP_KEY_START_LOCATION = "trip_start_location";
    public static final String TRIP_KEY_END_LOCATION = "trip_end_location";
    public static final String TRIP_KEY_STATUS = "trip_status";

    public static final String KEY_COMPANY_NAME = "companyName";
    public static final String KEY_VEHICLE_REGISTRATION_NO = "vehicleRegistrationNo";
    public static final String KEY_VEHICLE_BRAND = "vehicleBrand";
    public static final String KEY_VEHICLE_MODEL = "vehicleModel";
    public static final String KEY_VEHICLE_FUEL_TYPE = "vehicleFuelType";
    public static final String KEY_VEHICLE_YEAR_MANUFACTURE = "vehicleManufacture";
    public static final String KEY_VEHICLE_COLOR = "vehicleColor";
    public static final String KEY_VEHICLE_ID = "vehicleId";
    public static final String SEND_IOT_DATA_FORCEFULLY = "iotData";

    public static final int BROADCAST_TO_ACTIVITY = 0;
    public static final int BROADCAST_TO_IOT = 1;

    public static final int DASHBOARD_REQUEST_CODE = 2;
    public static final int SETTINGS_RESULT_CODE = 3;
    public static final int FORGOT_REQUEST_CODE = 4;
    public static final int ACCESS_LOCATION = 1;

    public static final String LOCAL_RECEIVER_ACTION_NAME = "VehicleDataUpdates";
    public static final String LOCAL_RECEIVER_NAME = "VehicleData";

    public static final String DASHBOARD_RECEIVER_ACTION_NAME = "DashboardDataUpdates";
    public static final String DASHBOARD_RECEIVER_NAME = "DahsboardData";

    public static final String LOCAL_SERVICE_RECEIVER_ACTION_NAME = "ServiceVehicleDataUpdates";
    public static final String DATABASEFULL = "Database full";
    public static final String SIGNOUT = "SignOut";
    public static final String SUCCESS = "Success";

    @SuppressLint("SdCardPath")
    public static final String LOCAL_PATH = "mobiliya_fleet_sample.txt";
    public static final int SYNC_DATA_TIME = 1;
    public static final String KEY_SYNC_DATA_TIME = "syncDataOnIOT";
    public static final String MESSAGE = "message";
    public static final String SYNC_TIME = "synctime";
    public static final int SET_DEFAULT_MEMORY_SIZE = 50;


    /**
     * Trip related apis
     **/
    public static final String UPDATE_TRIP_URL = TRIP_SERVICE_BASEURL_PROD + "%s/trips/%s";
    public static final String ADD_TRIP_URL = TRIP_SERVICE_BASEURL_PROD + "%s/trips";
    public static final String GET_TRIP_LIST_URL = TRIP_SERVICE_BASEURL_PROD + "%s/trips";
    public static final String GET_TRIP_DETAIL_URL = TRIP_SERVICE_BASEURL_PROD + "%s/trips/%s";
    public static final String TRIPID = "tripid";
    public static final String ADAPTER_STATUS = "adapter_status";

    public static final String TIME_ONGOING = "Triptime";
    public static final String STOPS_ONGOING = "stops";
    public static final String TOTAL_MILES_ONGOING = "totalmilage";
    public static final String FUEL_ONGOING = "fuel";
    public static final String MILES_ONGOING = "milage";

    public static final String LAST_SYNC_DATE = "lastsync";
    public static final String KEY_MOVE_TO_BT_SCREEN = "moveToBtScreen";
    public static final String LOCATION_NOT_CHANGED = "Gps_not_enabled";
    public static final String SPEEDING = "Speeding";
    public static final String KEY_OVER_SPEEDING = "overSpeeding";
    public static final String KEY_HARD_BRAKING = "hardBraking";
    public static final String KEY_AGGRESSIVE_ACCELERATOR = "aggressiveAccelerator";
    public static final String KEY_VEHICLE_STOPS = "vehicleStop";
    public static final String KEY_DRIVER_SCORE = "driverScore";
    public static final String KEY_MILES = "miles";
    public static final String KEY_TRIP_DURATION = "tripDuration";
    public static final String KEY_START_LOCATION = "startLocation";
    public static final String KEY_END_LOCATION = "endLocation";
    public static final String KEY_START_TIME = "startTime";
    public static final String KEY_END_TIME = "endTime";
    public static final String KEY_TRIP_DATE_TIME = "tripDateTime";
    public static final String NOTIFICATION_PAUSE_BROADCAST = "NOTIFICATION_PAUSE";
    public static final String NOTIFICATION_STOP_BROADCAST = "NOTIFICATION_STOP";
    public static final String MAP_TOKEN = "sk.eyJ1IjoibW9iaWxpeWExMjM0IiwiYSI6ImNqZ3g5bjEwcTFpaWYzM3MzdTltaXlxa2wifQ.aTtyhsaulAemRph24Crn_A";
}

