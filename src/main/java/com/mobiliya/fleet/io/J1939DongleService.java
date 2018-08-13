package com.mobiliya.fleet.io;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.widget.Toast;

import com.j1939.api.Const;
import com.j1939.api.J1939Adapter;
import com.j1939.api.Vehicle;
import com.j1939.api.enums.CANBusSpeeds;
import com.j1939.api.enums.ConnectionStates;
import com.j1939.api.enums.RecordingModes;
import com.j1939.api.enums.RetrievalMethods;
import com.j1939.api.enums.SleepModes;
import com.mobiliya.fleet.AcceleratorApplication;
import com.mobiliya.fleet.activity.BaseActivity;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.FaultModel;
import com.mobiliya.fleet.models.Parameter;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.NotificationManagerUtil;
import com.mobiliya.fleet.utils.SPNData;
import com.mobiliya.fleet.utils.SharePref;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import static com.mobiliya.fleet.utils.CommonUtil.getTimeDiff;
import static com.mobiliya.fleet.utils.TripManagementUtils.updateLocations;


/**
 * Class is responsible for handling connection and events of j1939 dongle
 */

public class J1939DongleService extends AbstractGatewayService {
    // j1939 adapter and service
    private J1939Adapter j1939;
    private Context serviceContext;
    private static final String TAG = "j1939DongleService";
    private final int appRecordingMode = RecordingModes.Never.getValue();
    private int faultCount;
    private int faultIndex;
    private Boolean mIsSkipEnabled = false;
    private SharePref mPref;
    int mSpeedCount = 0;
    boolean isSpeedLowered = true;
    private Timer mTimer;
    private static boolean sStatusConnected = false;

    @Override
    protected void executeQueue() throws InterruptedException {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate");
        serviceContext = this;
        registerReceiver(
                mSignOutReceiver, new IntentFilter(Constants.SIGNOUT));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.d(TAG, "onStartCommand() called");
        return START_STICKY;
    }

    /*method to initialize timer task*/
    private void initDataSyncTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
            mTimer = new Timer();
        } else {
            mTimer = new Timer();
        }

        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LogUtil.i(TAG, "3 sec timer callback callled ->" + 3);
                performAction();
            }
        }, 0, (3 * 1000));
    }


    private void performAction() {
        LogUtil.d(TAG, "called runnable");
        HashMap<String, String> commandResult = new HashMap<>();
        if (gpsTracker.getIsGPSTrackingEnabled()) {
            //gpsTracker.getLocation();
            commandResult.put("Latitude", String.valueOf(gpsTracker.getLatitude()));
            commandResult.put("Longitude", String.valueOf(gpsTracker.getLongitude()));
            commandResult.put("RPM", "NA");
            if (mParameter != null) {
                mParameter.Latitude = String.valueOf(gpsTracker.getLatitude());
                mParameter.Longitude = String.valueOf(gpsTracker.getLongitude());
                updateLocations(getBaseContext(), mParameter);
                try {
                    float speed = Float.valueOf(String.format("%.2f", gpsTracker.getSpeed()));
                    int speedInt = Math.round(speed);
                    if (speedInt >= 0) {
                        mParameter.Speed = speedInt;
                        commandResult.put("Speed", String.valueOf(mParameter.Speed)+" mph");
                        commandResult.put("Speedcount", String.valueOf("0"));
                    }
                    LogUtil.d(TAG, "Vehicle speed with out adapter:" + speed);

                    float distance = Float.valueOf(String.format("%.2f", gpsTracker.getDistance()));
                    if (distance >= 0) {
                        mParameter.Distance = distance;
                    }
                    LogUtil.d(TAG, "Vehicle distance with out adapter:" + distance);
                } catch (Exception e) {
                    LogUtil.d(TAG, "exception on calculation of speed");
                }
                LogUtil.d(TAG, "Coordinates - latitude " + mParameter.Latitude + " Longitude:" + mParameter.Longitude + " Accuracy " + gpsTracker.getAccuracy());
            }
        }
        sendMessageToActivity(commandResult);
    }

    @Override
    public void startService() throws IOException {
        isRunning = true;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void connectToAdapter() {
        LogUtil.d(TAG, "connectToAdapter called");

        mIsSkipEnabled = SharePref.getInstance(this).getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
        LogUtil.d(TAG, "return, send data without adapter");
        if (mIsSkipEnabled) {
            initDataSyncTimer();
            return;
        }

        if (j1939 != null) {
            j1939.Disconnect(true);
        }
        if (j1939 == null) {
            j1939 = new J1939Adapter(this, eventHandler);
            j1939.SetBLEName("BlueFire LE");
            j1939.SetBT2Name("BlueFire");
        }

        // Set to use an insecure connection.
        // Note, there are other Android devices that require this other than just ICS (4.0.x).
        if (android.os.Build.VERSION.RELEASE.startsWith("4.0."))
            j1939.SetInsecureConnection(true);
        else
            j1939.SetInsecureConnection(false);

        // Initialize adapter properties
        initializeAdapter();

        // Setup to receive API events
        ReceiveEventsThreading ReceiveEventsThread = new ReceiveEventsThreading();
        ReceiveEventsThread.start();
        getVehicleData();


        ConnectAdapterThread connectThread = new ConnectAdapterThread();
        connectThread.start();
    }

    @Override
    public void stopService() {
        LogUtil.d(TAG, "stopService adapter will disconnect");
        disconnectAdapter();
        isRunning = false;
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        // kill service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
            stopSelf();
        } else {
            stopSelf();
        }
        mParameter = null;
    }

    // j1939 Event Handler
    private static final Handler eventHandler = new Handler() {
        @Override
        @SuppressLint("HandlerLeak")
        public void handleMessage(Message msg) {
            Message handleMessage = new Message();
            handleMessage.what = msg.what;
            handleMessage.obj = msg.obj;
            EventsQueue.add(handleMessage);
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy");
        try {
            unregisterReceiver(
                    mSignOutReceiver);
        } catch (Exception ex) {
            ex.getMessage();
        }
    }

    /* Initialize J1939 adapter properties */
    private void initializeAdapter() {
        // Set Bluetooth adapter type
        j1939.UseBLE = true;
        j1939.UseBT21 = false;

        // Set to receive notifications from the adapter.
        // Note, this should only be used during testing.
        j1939.SetNotificationsOnOff(false);

        // Set to ignore data bus settings
        j1939.SetIgnoreJ1939(false);
        j1939.SetIgnoreJ1708(false);
        j1939.SetIgnoreOBD2(true);

        // Set the minimum interval
//        j1939.SetMinInterval(appMinInterval);

        // Set the BLE Disconnect Wait Timeout.
        // Note, in order for BLE to release the connection to the adapter and allow reconnects
        // or subsequent connects, it must be completely closed. Unfortunately Android does not
        // have a way to detect this other than waiting a set amount of time after disconnecting
        // from the adapter. This wait time can vary with the Android version and the make and
        // model of the mobile device. The default is 2 seconds. If your app experiences numerous
        // unable to connect and j1939 LE fails to show up under Bluetooth settings, try increasing
        // this value.
        j1939.SetBLEDisconnectWaitTime(j1939.BleDisconnectWaitTime);

        // Set the Bluetooth discovery timeout.
        // Note, depending on the number of Bluetooth devices present on the mobile device,
        // discovery could take a long time.
        // Note, if this is set to a high value, the app needs to provide the user with the
        // capability of canceling the discovery.
//        j1939.SetDiscoveryTimeout(appDiscoveryTimeout);

        // Set number of Bluetooth connection attempts.
        // Note, if the mobile device does not connect, try setting this to a value that
        // allows for a consistent connection. If you're using multiple adapters and have
        // connection problems, un-pair all devices before connecting.
        // Note: Bluetooth Classic (UseBT21) uses Com sockets and they can block for a
        // considerably amount of time depending on the OEM device. It is therefore recommended
        // that you adjust the MaxConnectAttempts, MaxReconnectAttempts, and the DiscoveryTimeout
        // to compensate for this duration.
        j1939.SetMaxConnectAttempts(j1939.MaxConnectAttempts);
        j1939.SetMaxReconnectAttempts(j1939.DiscoveryTimeout);
        j1939.SetBluetoothRecycleAttempt(j1939.MaxConnectAttempts);

        // Set the device and adapter ids
        String appDeviceId = "";
        j1939.SetDeviceId(appDeviceId);
        String appAdapterId = "";
        LogUtil.i("Adapter", " initializeAdapter()- Adapter id -->" + appAdapterId);
        j1939.SetAdapterId(appAdapterId);

        // Set the connect to last adapter setting
        j1939.SetConnectToLastAdapter(true);

        // Set the adapter security parameters
        j1939.SetSecurity(false, false, "", "");

        // Set to optimize data retrieval
        j1939.SetOptimizeDataRetrieval(false);

        // Set the send all packets option
        j1939.SetSendAllPackets(true);

        // Set streaming and recording mode
        j1939.ELD.SetStreaming(false);
        j1939.ELD.SetRecordingMode(RecordingModes.forValue(appRecordingMode));
        j1939.SetSleepMode(SleepModes.NoSleep);
        j1939.SetPerformanceModeOn(false);
        j1939.SetDisconnectedReboot(true, 60);
    }


    /* j1939 Event Handler Thread */
    @SuppressWarnings({"InfiniteLoopStatement", "unused"})
    private class ReceiveEventsThreading extends Thread {
        public void run() {
            while (true) {
                if (!EventsQueue.isEmpty()) {
                    final Message handleMessage = EventsQueue.poll();
                    if (handleMessage != null && ctx != null) {
                        ((BaseActivity) ctx).runOnUiThread(new Runnable() {
                            public void run() {
                                processEvent(handleMessage);
                            }
                        });
                    }
                }
                threadSleep(1); // allow other threads to execute
            }
        }
    }

    /* Handle adapter state events*/
    private void processEvent(Message msg) {
        try {
            ConnectionStates connectionState = ConnectionStates.values()[msg.what];
            showDeviceStatus(connectionState.name());
            switch (connectionState) {
                case Initializing:
                case Initialized:
                case Discovering:
                case Disconnecting:
                    // Status only
                    break;

                case Connecting:
                    if (j1939 != null && !j1939.IsReconnecting())
                        ((BaseActivity) ctx).setBTStatus("Connection Attempt " + j1939.ConnectAttempts());
                    break;

                case Connected:
                    //adapterConnected();
                    break;

                case NotAuthenticated:
                    adapterNotAuthenticated();
                    break;

                case Disconnected:
                    adapterDisconnected();
                    break;

                case Reconnecting:
                    adapterReconnecting();
                    break;

                case Reconnected:
                    break;

                case NotReconnected:
                    adapterNotReconnected();
                    break;

                case CANStarting:
                    CANStarting();
                    break;

                case J1708Restarting:
                    break;

                case ELDConnected:
                    //adapterConnected();
                    break;

                case NotConnected:
                    adapterNotConnected();
                    break;

                case DataChanged:
                    //adapterConnected();
                    if (j1939 != null && j1939.IsVehicleDataChanged())
                        showTruckData();
                    break;

                case Heartbeat:
                    adapterConnected();
                    return; // do not show status

                case CANFilterFull:
                    break;

                case Notification:
                    return; // do not show status

                case AdapterMessage:
                    return; // do not show status

                case AdapterReboot:
                    adapterNotConnected();
                    return; // do not show status

                case DataError:
                    adapterNotConnected();
                    return; // do not show status

                case DataTimeout:
                    adapterNotConnected();
                    return; // do not show status

                case BluetoothTimeout:
                    adapterNotConnected();
                    break;

                case AdapterTimeout:
                    adapterNotConnected();
                    break;

                case SystemError:
                    adapterNotConnected();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showDeviceStatus(String deviceStatus) {
        String status = "";
        if (deviceStatus.equalsIgnoreCase("Connecting") || deviceStatus.equalsIgnoreCase("NotConnected") || deviceStatus.equalsIgnoreCase("Disconnected")) {
            status = deviceStatus;
        } else if (deviceStatus.equalsIgnoreCase("DataChanged") ||
                deviceStatus.equalsIgnoreCase("Heartbeat") ||
                deviceStatus.equalsIgnoreCase("Authenticated") ||
                deviceStatus.equalsIgnoreCase("AdapterConnected") ||
                deviceStatus.equalsIgnoreCase("Notification") ||
                deviceStatus.equalsIgnoreCase("J1939Starting") ||
                deviceStatus.equalsIgnoreCase("Connected") ||
                deviceStatus.equalsIgnoreCase("CANFilterFull")) {
            deviceStatus = "Connected";
            status = deviceStatus;
        }
        HashMap<String, String> dashboardData = new HashMap<>();
        dashboardData.put(Constants.STATUS, status);
        sendMessageToDashboard(dashboardData);
    }

    /* start fetching data from adapter and show status as adapter is connected */
    private void adapterConnected() {
        if (sStatusConnected) {
            LogUtil.d(TAG, "sStatusConnected: true so return");
            return;
        }
        sStatusConnected = true;
        // Connect to ELD
        j1939.ELD.Connect();
        AcceleratorApplication.sIsAbdapterConnected = true;
        LogUtil.d(TAG, "AcceleratorApplication.sIsAbdapterConnected, Adapter is connected");
        J1939DongleService.sIgnitionStatusCallback.onConnectionStatusChange(true);

        // Get adapter data
        getAdapterData();
        getVehicleData();
        showTruckData();

        String Message = "Adapter is connected.";
        if (((BaseActivity) ctx) != null) {
            ((BaseActivity) ctx).setBTStatus(Message);
        }
    }

    /* method to fetch CAN bus speed and re retrive truck data */
    private void CANStarting() {
        // Get the CAN bus speed
        CANBusSpeeds CANBusSpeed = j1939.GetCANBusSpeed();
        String Message;
        if (j1939.IsOBD2())
            Message = "OBD2";
        else
            Message = "J1939";
        Message += " is starting, CAN bus speed is ";

        switch (CANBusSpeed) {
            case K250:
                Message += "250K.";
                break;
            case K500:
                Message += "500K.";
                break;
            default:
                Message += "unknown.";
                break;
        }
    }

    // Start retrieving data after connecting to the adapter
    private void getAdapterData() {
        // Check for an incompatible version.
        if (!j1939.IsCompatible()) {
            Toast.makeText(this, "The Adapter is not compatible with this API.", Toast.LENGTH_LONG).show();
            disconnectAdapter();
        }
    }

    private void threadSleep(int Interval) {
        try {
            Thread.sleep(Interval);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void adapterNotAuthenticated() {
        adapterNotConnected();
        Toast.makeText(this, "You are not authorized to access this adapter. Check for the correct adapter, the 'Connect to Last Adapter' setting, or your 'User Name and Password'.", Toast.LENGTH_LONG).show();
    }

    private void adapterDisconnected() {

        adapterNotConnected(false);
        String Message = "Adapter disconnected.";
    }

    private void adapterReconnecting() {
        String Message = "Reconnecting to the Adapter.";
    }

    private void adapterNotReconnected() {
        adapterNotConnected(false);

        String Message = "Adapter did not reconnect.";
    }

    private void adapterNotConnected() {
        adapterNotConnected(true);
    }

    private void adapterNotConnected(boolean logMessage) {
        sStatusConnected = false;
        AcceleratorApplication.sIsAbdapterConnected = false;
        J1939DongleService.sIgnitionStatusCallback.onConnectionStatusChange(false);

        String Message = "Adapter not connected.";
        ((BaseActivity) ctx).setBTStatus(Message);
    }

    /*Request data from the adapter. */
    public void getVehicleData() {
        LogUtil.d(TAG, "getVehicleData()");
        //initDataSyncTimer(Constants.SYNC_DATA_TIME);
        if (j1939 == null) {
            LogUtil.d(TAG, "getVehicleData() return j1939 object is null");
            connectToAdapter();
            return;
        }
        RetrievalMethods retrievalMethod = RetrievalMethods.OnChange; // do not use OnInterval with this many data requests
        int retrievalInterval = j1939.GetMinInterval(); // should be MinInterval or greater with this many requests
        int hoursInterval = 30 * Const.OneSecond; // hours only change every 3 minutes

        // Note, be careful not to request too much data at one time otherwise you run the risk of filling up
        // the CAN Filter buffer. You can experiment with combining data retrievals to determine how much you can
        // request before filling the CAN Filter buffer (you get an error if you do).

        // Start monitoring for faults.
        // Note, this clears the CAN Filter so it must be before any other requests for data.
        j1939.GetFaults();

        // Start monitoring all other truck data
        j1939.GetEngineData1(retrievalMethod, retrievalInterval); // RPM, Percent Torque, Driver Torque, Torque Mode
        j1939.GetEngineData2(retrievalMethod, retrievalInterval); // Percent Load, Accelerator Pedal Position
        j1939.GetEngineData3(retrievalMethod, retrievalInterval); // Vehicle Speed, Max Set Speed, Brake Switch, Clutch Switch, Park Brake Switch, Cruise Control Settings and Switches
        j1939.GetOdometer(retrievalMethod, retrievalInterval); // Distance and Odometer
        j1939.GetEngineHours(retrievalMethod, hoursInterval); // Total Engine Hours, Total Idle Hours
        j1939.GetBrakeData(retrievalMethod, retrievalInterval); // Application Pressure, Primary Pressure, Secondary Pressure
        j1939.GetBatteryVoltage(retrievalMethod, retrievalInterval); // Battery Voltage
        j1939.GetFuelData(retrievalMethod, retrievalInterval); // Fuel Used, Idle Fuel Used, Fuel Rate, Instant Fuel Economy, Avg Fuel Economy, Throttle Position
        j1939.GetTemps(retrievalMethod, retrievalInterval); // Oil Temp, Coolant Temp, Intake Manifold Temperature
        j1939.GetPressures(retrievalMethod, retrievalInterval); // Oil Pressure, Coolant Pressure, Intake Manifold(Boost) Pressure
        j1939.GetCoolantLevel(retrievalMethod, retrievalInterval); // Coolant Level
        j1939.GetTransmissionGears(retrievalMethod, retrievalInterval); // Selected and Current Gears (not available in J1708)
    }

    private void disconnectAdapter() {
        try {
            // Wait for the adapter to disconnect so that the Connect button
            // is not displayed too prematurely.
            LogUtil.d(TAG, "adapter will disconnect now");
            if (j1939 != null) {
                j1939.Disconnect(true);
            }
            j1939 = null;
            AcceleratorApplication.sIsAbdapterConnected = false;
            J1939DongleService.sIgnitionStatusCallback.onConnectionStatusChange(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* method will pass truck data to activity for displaying it on screen*/
    private void showTruckData() {
        if (mParameter == null) {
            mParameter = new Parameter();
        }
        //update mParameter to send this on IOT
        updateParameter();

        //Below data to shown on vehicle health
        HashMap<String, String> commandResult = new HashMap<>();
        commandResult.put("RPM", formatInt(Vehicle.RPM));
        commandResult.put("Speed", formatFloat(Vehicle.Speed * Const.KphToMph, 2));
        commandResult.put("AccelPedal", formatFloat(Vehicle.AccelPedal, 2));
        commandResult.put("PctLoad", formatInt(Vehicle.PctLoad));
        commandResult.put("PctTorque", formatInt(Vehicle.PctTorque));
        commandResult.put("DrvPctTorque", formatInt(Vehicle.DrvPctTorque));
        commandResult.put("TorqueMode", String.valueOf(Vehicle.TorqueMode));

        commandResult.put("Distance", formatFloat(Vehicle.Distance * Const.MetersToMiles, 2));
        commandResult.put("HiResDistance", formatFloat(Vehicle.HiResDistance * Const.MetersToMiles, 2));
        commandResult.put("LoResDistance", formatFloat(Vehicle.LoResDistance * Const.KmToMiles, 2));
        commandResult.put("Odometer", formatFloat(Vehicle.Odometer * Const.MetersToMiles, 2));
        commandResult.put("HiResOdometer", formatFloat(Vehicle.HiResOdometer * Const.MetersToMiles, 2));
        commandResult.put("LoResOdometer", formatFloat(Vehicle.LoResOdometer * Const.KmToMiles, 2));

        commandResult.put("TotalHours", formatFloat(Vehicle.TotalHours, 3));
        commandResult.put("IdleHours", formatFloat(Vehicle.IdleHours, 3));
        commandResult.put("BrakeAppPressure", formatFloat(Vehicle.BrakeAppPressure * Const.kPaToPSI, 2));
        commandResult.put("Brake1AirPressure", formatFloat(Vehicle.Brake1AirPressure * Const.kPaToPSI, 2));
        commandResult.put("CurrentGear", formatInt(Vehicle.CurrentGear));
        commandResult.put("SelectedGear", formatInt(Vehicle.SelectedGear));
        commandResult.put("BatteryPotential", formatFloat(Vehicle.BatteryPotential, 2));

        commandResult.put("FuelRate", formatFloat(Vehicle.FuelRate * Const.LphToGalPHr, 2));
        commandResult.put("FuelUsed", formatFloat(Vehicle.FuelUsed * Const.LitersToGal, 3));
        commandResult.put("HiResFuelUsed", formatFloat(Vehicle.HiResFuelUsed * Const.LitersToGal, 3));
        commandResult.put("IdleFuelUsed", formatFloat(Vehicle.IdleFuelUsed * Const.LitersToGal, 3));
        commandResult.put("AvgFuelEcon", formatFloat(Vehicle.AvgFuelEcon * Const.KplToMpg, 2));
        commandResult.put("InstFuelEcon", formatFloat(Vehicle.InstFuelEcon * Const.KplToMpg, 2));
        commandResult.put("ThrottlePos", formatFloat(Vehicle.ThrottlePos, 2));

        commandResult.put("OilTemp", formatFloat(celciusToFarenheit(Vehicle.OilTemp), 2));
        commandResult.put("OilPressure", formatFloat(Vehicle.OilPressure * Const.kPaToPSI, 2));
        commandResult.put("IntakeTemp", formatFloat(celciusToFarenheit(Vehicle.IntakeTemp), 2));
        commandResult.put("IntakePressure", formatFloat(Vehicle.IntakePressure * Const.kPaToPSI, 2));
        commandResult.put("CoolantTemp", formatFloat(celciusToFarenheit(Vehicle.CoolantTemp), 2));
        commandResult.put("CoolantPressure", formatFloat(Vehicle.CoolantPressure * Const.kPaToPSI, 2));
        commandResult.put("CoolantLevel", formatFloat(Vehicle.CoolantLevel, 2));

        String breakSwitch = "";
        if (String.valueOf(Vehicle.BrakeSwitch).equalsIgnoreCase("off")) {
            breakSwitch = "0";
        } else if (String.valueOf(Vehicle.BrakeSwitch).equalsIgnoreCase("on")) {
            breakSwitch = "1";
        } else {
            breakSwitch = String.valueOf(Vehicle.BrakeSwitch);
        }
        commandResult.put("BrakeSwitch", breakSwitch);
        String clutchSwitch = "";
        if (String.valueOf(Vehicle.ClutchSwitch).equalsIgnoreCase("off")) {
            clutchSwitch = "0";
        } else if (String.valueOf(Vehicle.ClutchSwitch).equalsIgnoreCase("on")) {
            clutchSwitch = "1";
        } else {
            clutchSwitch = String.valueOf(Vehicle.ClutchSwitch);
        }
        commandResult.put("ClutchSwitch", clutchSwitch);
        commandResult.put("ParkBrakeSwitch", String.valueOf(Vehicle.ParkBrakeSwitch));
        commandResult.put("CruiseOnOff", String.valueOf(Vehicle.CruiseOnOff));
        commandResult.put("CruiseState", String.valueOf(Vehicle.CruiseState));
        commandResult.put("CruiseSetSpeed", formatFloat(Vehicle.CruiseSetSpeed * Const.KphToMph, 0));

        float MaxSpeed = Vehicle.MaxSpeed;
        if (Vehicle.HiResMaxSpeed > 0)
            MaxSpeed = Vehicle.HiResMaxSpeed;
        commandResult.put("MaxSpeed", formatFloat(MaxSpeed * Const.KphToMph, 0));

        commandResult.put("Truck RPM", formatInt(Vehicle.RPM));
        commandResult.put("Truck Speed", formatFloat(Vehicle.Speed * Const.KphToMph, 2));
        commandResult.put("Truck Distance", formatFloat(Vehicle.Distance * Const.MetersToMiles, 2));
        commandResult.put("Truck Odometer", formatFloat(Vehicle.Odometer * Const.MetersToMiles, 2));
        commandResult.put("Truck TotalHours", formatFloat(Vehicle.TotalHours, 3));

        commandResult.put("Truck EngineVIN", Vehicle.EngineVIN);
        commandResult.put("Truck EngineMake", Vehicle.EngineMake);
        commandResult.put("Truck EngineModel", Vehicle.EngineModel);
        commandResult.put("Truck EngineSerialNo", Vehicle.EngineSerialNo);
        commandResult.put("Truck EngineUnitNo", Vehicle.EngineUnitNo);
        commandResult.put("Truck TotalHours", formatFloat(Vehicle.TotalHours, 3));
        commandResult.put("Truck TotalHours", formatFloat(Vehicle.TotalHours, 3));
        commandResult.put("FaultSPN", mParameter.FaultSPN);
        commandResult.put("FaultFMI", mParameter.FaultFMI);


        if (gpsTracker.getIsGPSTrackingEnabled()) {
            commandResult.put("latitude", String.valueOf(gpsTracker.getLatitude()));
            commandResult.put("Logitude", String.valueOf(gpsTracker.getLongitude()));
            mParameter.Latitude = String.valueOf(gpsTracker.getLatitude());
            mParameter.Longitude = String.valueOf(gpsTracker.getLongitude());
            LogUtil.d(TAG, "GPSTracker latitude: " + gpsTracker.getLatitude() + " longitude: " + gpsTracker.getLongitude());

            updateLocations(getBaseContext(), mParameter);

        }

        try {
            String speed = formatFloat(Vehicle.Speed * Const.KphToMph, 2);
            int speedcount = getSpeedCount(speed);
            commandResult.put("Speedcount", String.valueOf(speedcount));
        } catch (NumberFormatException e) {
            LogUtil.d(TAG, "error number format exception");
        }
        //saveToPref(commandResult);
        sendMessageToActivity(commandResult);
    }


    private int getSpeedCount(String speed) {
        if (!TextUtils.isEmpty(speed)) {
            if (speed != null) {
                mSpeedCount = SharePref.getInstance(getApplicationContext()).getItem(Constants.SPEEDING, 0);
                int limit = SharePref.getInstance(getApplicationContext()).getSpeedLimit();
                Float speedingInt = Float.parseFloat(speed);
                if (speedingInt < limit) {
                    isSpeedLowered = true;
                }
                if (speedingInt > limit && isSpeedLowered) {
                    mSpeedCount++;
                    SharePref.getInstance(getApplicationContext()).addItem(Constants.SPEEDING, mSpeedCount);
                    isSpeedLowered = false;
                }
                return mSpeedCount;
            }
        }
        return 0;
    }

    private void updateParameter() {
        mParameter.VIN = Vehicle.VIN;
        mParameter.AccelPedal = Vehicle.AccelPedal != -1 ? Vehicle.AccelPedal : mParameter.AccelPedal;
        mParameter.PctLoad = Vehicle.PctLoad != -1 ? Vehicle.PctLoad : mParameter.PctLoad;
        mParameter.PctTorque = Vehicle.PctTorque != -1 ? Vehicle.PctTorque : mParameter.PctTorque;
        mParameter.DrvPctTorque = Vehicle.DrvPctTorque != -1 ? Vehicle.DrvPctTorque : mParameter.DrvPctTorque;
        //mParameter.TorqueMode = String.valueOf(Vehicle.TorqueMode).equalsIgnoreCase("NA") ? mParameter.TorqueMode : String.valueOf(Vehicle.TorqueMode);
        mParameter.TorqueMode = TextUtils.isEmpty(String.valueOf(Vehicle.TorqueMode)) ? mParameter.TorqueMode : String.valueOf(Vehicle.TorqueMode);

        mParameter.HiResDistance = Vehicle.HiResDistance != -1 ? Vehicle.HiResDistance * Const.MetersToMiles : mParameter.HiResDistance;
        mParameter.LoResDistance = Vehicle.LoResDistance != -1 ? Vehicle.LoResDistance * Const.KmToMiles : mParameter.LoResDistance;

        mParameter.HiResOdometer = Vehicle.HiResOdometer != -1 ? Vehicle.HiResOdometer * Const.MetersToMiles : mParameter.HiResOdometer;
        mParameter.LoResOdometer = Vehicle.LoResOdometer != -1 ? Vehicle.LoResOdometer * Const.KmToMiles : mParameter.LoResOdometer;

        mParameter.IdleHours = Vehicle.IdleHours != -1 ? Vehicle.IdleHours : mParameter.IdleHours;
        mParameter.BrakeAppPressure = Vehicle.BrakeAppPressure != -1 ? Vehicle.BrakeAppPressure * Const.kPaToPSI : mParameter.BrakeAppPressure;
        mParameter.Brake1AirPressure = Vehicle.Brake1AirPressure != -1 ? Vehicle.Brake1AirPressure * Const.kPaToPSI : mParameter.Brake1AirPressure;
        mParameter.Brake2AirPressure = Vehicle.Brake2AirPressure != -1 ? Vehicle.Brake2AirPressure * Const.kPaToPSI : mParameter.Brake2AirPressure;
        mParameter.CurrentGear = Vehicle.CurrentGear != -1 ? Vehicle.CurrentGear : mParameter.CurrentGear;
        mParameter.SelectedGear = Vehicle.SelectedGear != -1 ? Vehicle.SelectedGear : mParameter.SelectedGear;
        mParameter.BatteryPotential = Vehicle.BatteryPotential != -1 ? Vehicle.BatteryPotential : mParameter.BatteryPotential;
        mParameter.PrimaryFuelLevel = Vehicle.PrimaryFuelLevel != -1 ? Vehicle.PrimaryFuelLevel : mParameter.PrimaryFuelLevel; //TODO: Need to verify at server
        mParameter.SecondaryFuelLevel = Vehicle.SecondaryFuelLevel != -1 ? Vehicle.SecondaryFuelLevel : mParameter.SecondaryFuelLevel;
        mParameter.FuelRate = Vehicle.FuelRate != -1 ? Vehicle.FuelRate * Const.LphToGalPHr : mParameter.FuelRate;
        mParameter.FuelUsed = Vehicle.FuelUsed != -1 ? Vehicle.FuelUsed * Const.LitersToGal : mParameter.FuelUsed;
        mParameter.HiResFuelUsed = Vehicle.HiResFuelUsed != -1 ? Vehicle.HiResFuelUsed * Const.LitersToGal : mParameter.HiResFuelUsed;
        mParameter.IdleFuelUsed = Vehicle.IdleFuelUsed != -1 ? Vehicle.IdleFuelUsed * Const.LitersToGal : mParameter.IdleFuelUsed;
        mParameter.AvgFuelEcon = Vehicle.AvgFuelEcon != -1 ? Vehicle.AvgFuelEcon * Const.KplToMpg : mParameter.AvgFuelEcon;
        mParameter.InstFuelEcon = Vehicle.InstFuelEcon != -1 ? Vehicle.InstFuelEcon * Const.KplToMpg : mParameter.InstFuelEcon;
        mParameter.ThrottlePos = Vehicle.ThrottlePos != -1 ? Vehicle.ThrottlePos : mParameter.ThrottlePos;

        mParameter.OilTemp = Vehicle.OilTemp != -1.0F ? celciusToFarenheit(Vehicle.OilTemp) : mParameter.OilTemp;
        mParameter.OilPressure = Vehicle.OilPressure != -1 ? Vehicle.OilPressure * Const.kPaToPSI : mParameter.OilPressure;
        mParameter.TransTemp = Vehicle.TransTemp != -1.0F ? celciusToFarenheit(Vehicle.TransTemp) : mParameter.TransTemp;
        mParameter.IntakeTemp = Vehicle.IntakeTemp != -1.0F ? celciusToFarenheit(Vehicle.IntakeTemp) : mParameter.IntakeTemp;
        mParameter.IntakePressure = Vehicle.IntakePressure != -1 ? Vehicle.IntakePressure * Const.kPaToPSI : mParameter.IntakePressure;
        mParameter.CoolantTemp = Vehicle.CoolantTemp != -1.0F ? celciusToFarenheit(Vehicle.CoolantTemp) : mParameter.CoolantTemp;
        mParameter.CoolantPressure = Vehicle.CoolantPressure != -1 ? Vehicle.CoolantPressure * Const.kPaToPSI : mParameter.CoolantPressure;
        mParameter.CoolantLevel = Vehicle.CoolantLevel != -1.0F ? Vehicle.CoolantLevel : mParameter.CoolantLevel;
/*
        mParameter.BrakeSwitch = !String.valueOf(Vehicle.BrakeSwitch).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.BrakeSwitch) : mParameter.BrakeSwitch;
        mParameter.ClutchSwitch = !String.valueOf(Vehicle.ClutchSwitch).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.ClutchSwitch) : mParameter.ClutchSwitch;
        mParameter.ParkBrakeSwitch = !String.valueOf(Vehicle.ParkBrakeSwitch).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.ParkBrakeSwitch) : mParameter.ParkBrakeSwitch;
        mParameter.CruiseOnOff = !String.valueOf(Vehicle.CruiseOnOff).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.CruiseOnOff) : mParameter.CruiseOnOff;
        mParameter.CruiseSet = !String.valueOf(Vehicle.CruiseSet).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.CruiseSet) : mParameter.CruiseSet;
        mParameter.CruiseCoast = !String.valueOf(Vehicle.CruiseCoast).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.CruiseCoast) : mParameter.CruiseCoast;
        mParameter.CruiseResume = !String.valueOf(Vehicle.CruiseResume).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.CruiseResume) : mParameter.CruiseResume;
        mParameter.CruiseAccel = !String.valueOf(Vehicle.CruiseAccel).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.CruiseAccel) : mParameter.CruiseAccel;
        mParameter.CruiseActive = !String.valueOf(Vehicle.CruiseActive).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.CruiseActive) : mParameter.CruiseActive;
        mParameter.CruiseState = !String.valueOf(Vehicle.CruiseState).equalsIgnoreCase("NA") ? String.valueOf(Vehicle.CruiseState) : mParameter.CruiseState;
*/
        mParameter.BrakeSwitch = !TextUtils.isEmpty(String.valueOf(Vehicle.BrakeSwitch)) ? String.valueOf(Vehicle.BrakeSwitch) : mParameter.BrakeSwitch;
        mParameter.ClutchSwitch = !TextUtils.isEmpty(String.valueOf(Vehicle.ClutchSwitch)) ? String.valueOf(Vehicle.ClutchSwitch) : mParameter.ClutchSwitch;
        mParameter.ParkBrakeSwitch = !TextUtils.isEmpty(String.valueOf(Vehicle.ParkBrakeSwitch)) ? String.valueOf(Vehicle.ParkBrakeSwitch) : mParameter.ParkBrakeSwitch;
        mParameter.CruiseOnOff = !TextUtils.isEmpty(String.valueOf(Vehicle.CruiseOnOff)) ? String.valueOf(Vehicle.CruiseOnOff) : mParameter.CruiseOnOff;
        mParameter.CruiseSet = !TextUtils.isEmpty(String.valueOf(Vehicle.CruiseSet)) ? String.valueOf(Vehicle.CruiseSet) : mParameter.CruiseSet;
        mParameter.CruiseCoast = !TextUtils.isEmpty(String.valueOf(Vehicle.CruiseCoast)) ? String.valueOf(Vehicle.CruiseCoast) : mParameter.CruiseCoast;
        mParameter.CruiseResume = !TextUtils.isEmpty(String.valueOf(Vehicle.CruiseResume)) ? String.valueOf(Vehicle.CruiseResume) : mParameter.CruiseResume;
        mParameter.CruiseAccel = !TextUtils.isEmpty(String.valueOf(Vehicle.CruiseAccel)) ? String.valueOf(Vehicle.CruiseAccel) : mParameter.CruiseAccel;
        mParameter.CruiseActive = !TextUtils.isEmpty(String.valueOf(Vehicle.CruiseActive)) ? String.valueOf(Vehicle.CruiseActive) : mParameter.CruiseActive;
        mParameter.CruiseState = !TextUtils.isEmpty(String.valueOf(Vehicle.CruiseState)) ? String.valueOf(Vehicle.CruiseState) : mParameter.CruiseState;

        mParameter.CruiseSetSpeed = Vehicle.CruiseSetSpeed != -1 ? Vehicle.CruiseSetSpeed * Const.KphToMph : mParameter.CruiseSetSpeed;
        mParameter.HiResMaxSpeed = (Vehicle.HiResMaxSpeed > 0) ? Vehicle.HiResMaxSpeed : mParameter.HiResMaxSpeed;

        mParameter.RPM = Vehicle.RPM != -1 ? Vehicle.RPM : mParameter.RPM;
        mParameter.Speed = Vehicle.Speed != -1 ? Vehicle.Speed * Const.KphToMph : mParameter.Speed;
        //mParameter.VehicleSpeed = mParameter.Speed;
        mParameter.Distance = Vehicle.Distance != -1 ? Vehicle.Distance * Const.MetersToMiles : mParameter.Distance;
        mParameter.Odometer = Vehicle.Odometer != -1 ? Vehicle.Odometer * Const.MetersToMiles : mParameter.Odometer;
        mParameter.TotalHours = Vehicle.TotalHours != -1.0F ? Vehicle.TotalHours : mParameter.TotalHours;

        mParameter.EngineVIN = Vehicle.EngineVIN;
        mParameter.EngineMake = Vehicle.EngineMake;
        mParameter.EngineModel = Vehicle.EngineModel;
        mParameter.EngineSerialNo = Vehicle.EngineSerialNo;
        mParameter.EngineUnitNo = Vehicle.EngineUnitNo;

        if (Objects.equals(Vehicle.EngineVIN, Const.NA) && Objects.equals(Vehicle.EngineMake, Const.NA))
            mParameter.EngineVIN = "";
            // Waiting just for VIN
        else if (Objects.equals(Vehicle.EngineVIN, Const.NA))
            mParameter.EngineVIN = "";
            // Waiting just for ID
        else if (Objects.equals(Vehicle.EngineMake, Const.NA))
            mParameter.EngineVIN = "";

        if (!Objects.equals(Vehicle.EngineVIN, Const.NA))
            j1939.StopRetrievingEngineVIN();
        if (!Objects.equals(Vehicle.EngineMake, Const.NA))
            j1939.StopRetrievingEngineId();


        if (Vehicle.GetFaultCount() == 0) {
            faultIndex = -1; // reset to show fault
        } else // faults found
        {
            if (Vehicle.GetFaultCount() != faultCount) // additional faults
            {
                faultCount = Vehicle.GetFaultCount();
                faultIndex = 0; // show first fault
            }
        }
        String faultConversion = "";
        String faultOccurrence = "";
        String faultFMI = "";
        String faultSPN = "";
        String faultSource = "";
        if (faultIndex < 0) {
            faultSource = "";
            faultSPN = "";
            faultFMI = "";
            faultOccurrence = "";
            faultConversion = "";
        } else {
            faultSource = String.valueOf(Vehicle.GetFaultSource(faultIndex));
            int spn = Vehicle.GetFaultSPN(faultIndex);
            faultSPN = String.valueOf(spn);
            int fmi = Vehicle.GetFaultFMI(faultIndex);
            faultFMI = String.valueOf(fmi);
            faultOccurrence = String.valueOf(Vehicle.GetFaultOccurrence(faultIndex));
            faultConversion = String.valueOf(Vehicle.GetFaultConversion(faultIndex));

            FaultModel fault = SPNData.getInstance(serviceContext).getError(spn, fmi);
            LogUtil.d(TAG, "faultSPN " + faultSPN);
            LogUtil.d(TAG, "faultSource " + faultSource);
            LogUtil.d(TAG, "faultFMI " + faultFMI);
            LogUtil.d(TAG, "faultOccurrence " + faultOccurrence);
            LogUtil.d(TAG, "faultConversion " + faultConversion);

            LogUtil.d(TAG, "faultSPN 1:" + fault.spn);
            LogUtil.d(TAG, "faultFMI 1:" + fault.fmi);
            LogUtil.d(TAG, "faultDescription 1:" + fault.description);
            LogUtil.d(TAG, "faultUnit 1:" + fault.unit);
            mParameter.FaultDescription = fault.description;

            DatabaseProvider.getInstance(serviceContext).addFault(fault);
            int faultAlertCount = SharePref.getInstance(serviceContext).getFaultAlertCount();
            SharePref.getInstance(serviceContext).setFaultAlertCount(faultAlertCount + 1);
        }

        mParameter.FaultSource = !Objects.equals(faultSource, "") ? faultSource : mParameter.FaultSource;
        mParameter.FaultSPN = !Objects.equals(faultSPN, "") ? faultSPN : mParameter.FaultSPN;
        mParameter.FaultFMI = !Objects.equals(faultFMI, "") ? faultFMI : mParameter.FaultFMI;
        mParameter.FaultOccurrence = !Objects.equals(faultOccurrence, "") ? faultOccurrence : mParameter.FaultOccurrence;
        mParameter.FaultConversion = !Objects.equals(faultConversion, "") ? faultConversion : mParameter.FaultConversion;

        mParameter.MaxSpeed = Vehicle.MaxSpeed != -1 ? Vehicle.MaxSpeed : mParameter.MaxSpeed;
        mParameter.HiResMaxSpeed = Vehicle.HiResMaxSpeed != -1 ? Vehicle.HiResMaxSpeed : mParameter.HiResMaxSpeed;
        /*mParameter.Make = !Vehicle.Make.equalsIgnoreCase("NA") ? Vehicle.Make : mParameter.Make;
        mParameter.Model = !Vehicle.Model.equalsIgnoreCase("NA") ? Vehicle.Model : mParameter.Model;
        mParameter.SerialNo = !Vehicle.SerialNo.equalsIgnoreCase("NA") ? Vehicle.SerialNo : mParameter.SerialNo;
        mParameter.UnitNo = !Vehicle.UnitNo.equalsIgnoreCase("NA") ? Vehicle.UnitNo : mParameter.UnitNo;
*/
        mParameter.Make = !TextUtils.isEmpty(Vehicle.Make) ? Vehicle.Make : mParameter.Make;
        mParameter.Model = !TextUtils.isEmpty(Vehicle.Model) ? Vehicle.Model : mParameter.Model;
        mParameter.SerialNo = !TextUtils.isEmpty(Vehicle.SerialNo) ? Vehicle.SerialNo : mParameter.SerialNo;
        mParameter.UnitNo = !TextUtils.isEmpty(Vehicle.UnitNo) ? Vehicle.UnitNo : mParameter.UnitNo;

        mParameter.AdapterId = j1939.GetAdapterId();
        mParameter.FirmwareVersion = j1939.GetFirmwareVersion();
        mParameter.HardwareVersion = j1939.GetHardwareVersion();
        mParameter.AdapterSerialNo = j1939.SerialNo();
        mParameter.HardwareType = String.valueOf(j1939.GetHardwareType());
        mParameter.SleepMode = String.valueOf(j1939.SleepMode());
        mParameter.LedBrightness = j1939.LedBrightness();

    }


    private void sendMessageToActivity(HashMap<String, String> msg) {

        Trip ongoingtrip = DatabaseProvider.getInstance(getApplicationContext()).getCurrentTrip();
        if (ongoingtrip != null) {
            String diff = getTimeDiff(getBaseContext(), ongoingtrip);
            NotificationManagerUtil.getInstance().upDateNotification(getBaseContext(), diff);
        }

        Intent intent = new Intent(Constants.LOCAL_RECEIVER_ACTION_NAME);
        // You can also include some extra data.
        intent.putExtra(Constants.LOCAL_RECEIVER_NAME, msg);
        //LocalBroadcastManager.getInstance(serviceContext).sendBroadcast(intent);
        serviceContext.sendBroadcast(intent);
    }

    private void sendMessageToDashboard(HashMap<String, String> msg) {
        Intent intent = new Intent(Constants.DASHBOARD_RECEIVER_ACTION_NAME);
        // You can also include some extra data.
        intent.putExtra(Constants.DASHBOARD_RECEIVER_NAME, msg);
        serviceContext.sendBroadcast(intent);
    }

    /* method to stop previous data retrieval if any*/
    public void stopVehicleData() {
        if (j1939 != null) {
            j1939.StopDataRetrieval();
        }

    }

    /* method to format int value before display*/
    private String formatInt(int data) {
        if (data < 0)
            return "NA";
        else
            return String.valueOf(data);
    }

    /* method to format float value for display*/
    private String formatFloat(float data, int precision) {
        if (data < 0)
            return "NA";

        return formatDecimal(data, precision);
    }

    private String formatDecimal(double data, int precision) {
        BigDecimal bd = new BigDecimal(data);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);
        return String.valueOf(bd.floatValue());
    }

    /*Connect to the adapter. Note, this is a blocking call and must run in it's own thread.*/
    @SuppressWarnings("unused")
    private class ConnectAdapterThread extends Thread {
        public void run() {
            j1939.Connect();
        }
    }

    private float celciusToFarenheit(float temp) {
        if (temp < 0)
            return -1;
        else
            return (temp * 1.8F + 32F);
    }

    private BroadcastReceiver mSignOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            stopService();
        }
    };
}
