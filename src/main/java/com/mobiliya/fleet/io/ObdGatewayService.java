package com.mobiliya.fleet.io;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.github.pires.obd.commands.protocol.LineFeedOffCommand;
import com.github.pires.obd.commands.protocol.ObdResetCommand;
import com.github.pires.obd.commands.protocol.SelectProtocolCommand;
import com.github.pires.obd.commands.protocol.TimeoutCommand;
import com.github.pires.obd.commands.temperature.AmbientAirTemperatureCommand;
import com.github.pires.obd.enums.ObdProtocols;
import com.github.pires.obd.exceptions.UnsupportedCommandException;
import com.google.inject.Inject;
import com.mobiliya.fleet.AcceleratorApplication;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.activity.BaseActivity;
import com.mobiliya.fleet.activity.ConfigActivity;
import com.mobiliya.fleet.config.ObdConfig;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.io.ObdCommandJob.ObdCommandJobState;
import com.mobiliya.fleet.models.FaultModel;
import com.mobiliya.fleet.models.Parameter;
import com.mobiliya.fleet.services.GPSTracker;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;

/**
 * This service is primarily responsible for establishing and maintaining a
 * permanent connection between the device where the application runs and a more
 * OBD Bluetooth interface.
 * <p/>
 * Secondarily, it will serve as a repository of ObdCommandJobs and at the same
 * time the application state-machine.
 */
@SuppressWarnings({"ALL", "unused"})
public class ObdGatewayService extends AbstractGatewayService {
    private static final String TAG = ObdGatewayService.class.getName();
    @Inject
    private final SharedPreferences prefs = null;
    private BluetoothDevice dev = null;
    private BluetoothSocket sock = null;
    private Context mContext;
    private GPSTracker gpsTracker;
    private Handler handle = new Handler();
    private Handler handleTask = new Handler();
    int mSpeedCount = 0;
    private boolean isSpeedLowered = true;
    private HashMap<String, String> commandResult = new HashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRunning = true;
        LogUtil.d(TAG, "onStartCommand() called");
        startConnection();
        return super.onStartCommand(intent, flags, startId);
    }

    public void startConnection() {
        new Thread(new Runnable() {
            public void run() {
                LogUtil.i(TAG, "connectToAdapter()");
                if (sock != null && sock.isConnected()) {
                    // close socket
                    try {
                        sock.close();
                    } catch (IOException e) {
                        LogUtil.d(TAG, "exception in connectToAdapter while closing socket");
                        LogUtil.e(TAG, e.getMessage());
                    }
                }
                final String remoteDevice = SharePref.getInstance(ctx).getItem(Constants.PREF_BT_DEVICE_ADDRESS);
                LogUtil.i(TAG, "Remote device address:" + remoteDevice);
                final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                dev = btAdapter.getRemoteDevice(remoteDevice);

                LogUtil.d(TAG, "Stopping Bluetooth discovery.");
                btAdapter.cancelDiscovery();
                try {
                    startObdConnection();
                    getVehicleData();
                    handleTask.post(runnable);
                    handle.removeCallbacks(mQueueCommands);
                    handle.post(mQueueCommands);
                } catch (Exception e) {
                    LogUtil.e(
                            TAG,
                            "There was an error while establishing connection. -> "
                                    + e.getMessage()
                    );
                }
            }
        }).start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "onCreate() called");
        mContext = this;
        gpsTracker = GPSTracker.getInstance(this);
        new Thread(new Runnable() {
            public void run() {
                handle.removeCallbacks(mQueueCommands);
                handle.post(mQueueCommands);
            }
        }).start();
        registerReceiver(
                mSignOutReceiver, new IntentFilter(Constants.SIGNOUT));
    }

    public void startService() {
        LogUtil.d(TAG, "Starting service..");
        isRunning = true;
    }

    @Override
    public void getVehicleData() {
        super.getVehicleData();
        LogUtil.i(TAG, "getVehicleData()");
        if (sock != null) {

            // Let's configure the connection.
            LogUtil.d(TAG, "Queueing jobs for connection configuration..");
            queueJob(new ObdCommandJob(new ObdResetCommand()));

            //Below is to give the adapter enough time to reset before sending the commands, otherwise the first startup commands could be ignored.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            queueJob(new ObdCommandJob(new EchoOffCommand()));

            /*
             * Will send second-time based on tests.
             *
             */
            queueJob(new ObdCommandJob(new EchoOffCommand()));
            queueJob(new ObdCommandJob(new LineFeedOffCommand()));
            queueJob(new ObdCommandJob(new TimeoutCommand()));

            // Get protocol from preferences
            final String protocol = prefs.getString(ConfigActivity.PROTOCOLS_LIST_KEY, "AUTO");
            queueJob(new ObdCommandJob(new SelectProtocolCommand(ObdProtocols.valueOf(protocol))));

            // Job for returning dummy data
            queueJob(new ObdCommandJob(new AmbientAirTemperatureCommand()));

            queueCounter = 0L;
            LogUtil.d(TAG, "Initialization jobs queued.");
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.d(TAG, "OnUnbind()");
        return super.onUnbind(intent);
    }

    @Override
    public void connectToAdapter() {
        super.connectToAdapter();
        startConnection();
    }


    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            if (sock == null) {
                LogUtil.d(TAG, "socket is null return");
                return;
            }
            if (isRunning) {
                if (isRunning()) {
                    LogUtil.i(TAG, "Service is running");
                }
                if (queueEmpty()) {
                    LogUtil.i(TAG, "Service queue is empty");
                }
            }
            if (isRunning && queueEmpty()) {
                queueCommands();
            } else {
                LogUtil.d(TAG, "mQueueCommands Service:null or service is not running or service queue is not empty");
            }
            if (isRunning) {
                // run again in period defined in preferences
                if (handle != null) {
                    handle.postDelayed(mQueueCommands, ConfigActivity.getObdUpdatePeriod());
                }
            }
        }
    };

    private void queueCommands() {
        LogUtil.d(TAG, "queueCommands");
        if (isRunning) {
            for (ObdCommand Command : ObdConfig.getCommands()) {
                queueJob(new ObdCommandJob(Command));
            }
        } else {
            LogUtil.d(TAG, "service is not bound");
        }
    }

    /**
     * Start and configure the connection to the OBD interface.
     * <p/>
     *
     * @throws IOException
     */
    private void startObdConnection() throws IOException {
        LogUtil.d(TAG, "Starting OBD connection..");

        try {
            sock = BluetoothManager.connect(dev);
            AcceleratorApplication.sIsAbdapterConnected = true;
            AbstractGatewayService.sIgnitionStatusCallback.onConnectionStatusChange(true);
            LogUtil.i(TAG, "Adapter is connected.");
            String Message = "Adapter is connected.";
            showDeviceStatus("Connected");
            if (((BaseActivity) ctx) != null) {
                ((BaseActivity) ctx).setBTStatus(Message);
            }

        } catch (Exception e2) {
            LogUtil.e(TAG, "There was an error while establishing Bluetooth connection. Stopping app.." + e2);
            String Message = "Adapter not connected.";
            showDeviceStatus("NotConnected");
            if (((BaseActivity) ctx) != null) {
                ((BaseActivity) ctx).setBTStatus(Message);
            }
            throw new IOException();
        }
    }

    private void showDeviceStatus(String deviceStatus) {
        String status = "";
        if (sock != null && sock.isConnected()) {
            status = "Connected";
        } else {
            status = "Disconnected";
        }
        HashMap<String, String> dashboardData = new HashMap<>();
        dashboardData.put(Constants.STATUS, status);
        sendMessageToDashboard(dashboardData);
    }

    private void sendMessageToDashboard(HashMap<String, String> msg) {
        Intent intent = new Intent(Constants.DASHBOARD_RECEIVER_ACTION_NAME);
        // You can also include some extra data.
        intent.putExtra(Constants.DASHBOARD_RECEIVER_NAME, msg);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    /**
     * This method will add a job to the queue while setting its ID to the
     * internal queue counter.
     *
     * @param job the job to queue.
     */
    @Override
    public void queueJob(ObdCommandJob job) {
        // This is a good place to enforce the imperial units option
        job.getCommand().useImperialUnits(prefs.getBoolean(ConfigActivity.IMPERIAL_UNITS_KEY, false));

        // Now we can pass it along
        super.queueJob(job);
    }

    /**
     * Runs the queue until the service is stopped
     */
    protected void executeQueue() throws InterruptedException {
        LogUtil.d(TAG, "Executing queue..");
        while (!Thread.currentThread().isInterrupted()) {
            ObdCommandJob job = null;
            try {
                job = jobsQueue.take();
                // log job
                LogUtil.d(TAG, "Taking job[" + job.getId() + "] from queue..");

                if (job.getState().equals(ObdCommandJobState.NEW)) {
                    LogUtil.d(TAG, "Job state is NEW. Run it..");
                    job.setState(ObdCommandJobState.RUNNING);
                    if (sock.isConnected()) {
                        job.getCommand().run(sock.getInputStream(), sock.getOutputStream());
                    } else {
                        showDeviceStatus("Disconnected");
                        job.setState(ObdCommandJobState.EXECUTION_ERROR);
                        LogUtil.e(TAG, "Can't run command on a closed socket.");
                    }
                } else
                    // log not new job
                    LogUtil.e(TAG,
                            "Job state was not new, so it shouldn't be in queue. BUG ALERT!");
            } catch (InterruptedException i) {
                Thread.currentThread().interrupt();
                showDeviceStatus("Disconnected");
            } catch (UnsupportedCommandException u) {
                if (job != null) {
                    job.setState(ObdCommandJobState.NOT_SUPPORTED);
                }
                showDeviceStatus("Disconnected");
                LogUtil.d(TAG, "Command not supported. -> " + u.getMessage());
            } catch (IOException io) {
                if (io.getMessage().contains("Broken pipe"))
                    job.setState(ObdCommandJobState.BROKEN_PIPE);
                else
                    job.setState(ObdCommandJobState.EXECUTION_ERROR);
                stopService();
                showDeviceStatus("Disconnected");
                LogUtil.e(TAG, "IO error. -> " + io.getMessage());
            } catch (Exception e) {
                if (job != null) {
                    job.setState(ObdCommandJobState.EXECUTION_ERROR);
                }
                showDeviceStatus("Disconnected");
                LogUtil.e(TAG, "Failed to run command. -> " + e.getMessage());
            }

            if (job != null) {
                final ObdCommandJob job2 = job;
                updateParameter(job2);
                stateUpdate(job2);
            }
        }
    }

    public void stateUpdate(final ObdCommandJob job) {
        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {
            cmdResult = getString(R.string.status_obd_no_support);
        } else {
            cmdResult = job.getCommand().getFormattedResult();
        }

        commandResult.put(cmdName, cmdResult);
        commandResult.put("Distance", String.valueOf(mParameter.Distance));
        commandResult.put("Speed", String.valueOf(mParameter.Speed));
        int speedcount = getSpeedCount(String.valueOf(mParameter.Speed));
        commandResult.put("Speedcount", String.valueOf(speedcount));

        if (gpsTracker.getIsGPSTrackingEnabled()) {
            gpsTracker.getLocation();
            commandResult.put("latitude", String.valueOf(gpsTracker.getLatitude()));
            commandResult.put("longitude", String.valueOf(gpsTracker.getLongitude()));
            mParameter.Latitude = String.valueOf(gpsTracker.getLatitude());
            mParameter.Longitude = String.valueOf(gpsTracker.getLongitude());
            LogUtil.d(TAG, "GPSTracker latitude: " + gpsTracker.getLatitude() + " longitude: " + gpsTracker.getLongitude());
        }
    }

    private void sendMessageToActivity() {
        if (commandResult != null && commandResult.size() <= 0) {
            LogUtil.d(TAG, "Command result is zero return");
            return;
        }
        Intent intent = new Intent(Constants.LOCAL_RECEIVER_ACTION_NAME);
        // You can also include some extra data.
        intent.putExtra(Constants.LOCAL_RECEIVER_NAME, commandResult);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    private final Runnable runnable = new Runnable() {
        public void run() {
            LogUtil.d(TAG, "called runnable after 10 sec");
            sendMessageToActivity();
            handleTask.postDelayed(runnable, 10000);//10 sec
        }
    };


    private void updateParameter(final ObdCommandJob job) {

        if (mParameter == null) {
            mParameter = new Parameter();
        }
        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {
            cmdResult = getString(R.string.status_obd_no_support);
        } else {
            cmdResult = job.getCommand().getFormattedResult();
        }
        LogUtil.i("updateParameter", "Command Name -> " + cmdName);
        LogUtil.i("updateParameter", "Result -> " + cmdResult);
        if (!TextUtils.isEmpty(cmdResult) && cmdResult.contains("UNABLETOCONNECT")) {
            LogUtil.d(TAG, "returning since result is unable to connect");
            return;
        }
        try {
            switch (cmdName) {
                case "Reset OBD":
                    mParameter.ResetOBD = cmdResult;
                    break;
                case "Echo Off":
                    mParameter.EchoOff = cmdResult;
                    break;
                case "Line Feed Off":
                    mParameter.LineFeedOff = cmdResult;
                    break;
                case "Command Equivalence Ratio":
                    mParameter.CommandEquivalanceRatio = cmdResult;
                    break;
                case "Distance traveled with MIL on":
                    mParameter.DistanceTraveledWithMILOn = cmdResult;
                    break;
                case "Distance since codes cleared":
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    if (cmdResult != null && cmdResult.length() > 0) {
                        cmdResult = cmdResult.substring(0, cmdResult.length() - 2);
                    }
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    float value = Float.valueOf(cmdResult);
                    LogUtil.d(TAG, "Distance since codes cleared in kms:" + value);
                    value = value * 0.621371F;
                    LogUtil.d(TAG, "Distance since codes cleared in miles:" + value);
                    mParameter.Distance = value;
                    break;
                case "Diagnostic Trouble Codes":
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    mParameter.DiagonosticTroubleCodes = cmdResult;
                    FaultModel faultModel = CommonUtil.getOBDError(cmdResult, this);
                    String faultSPN = faultModel.spn;
                    LogUtil.d(TAG, "Fault Desc:" + faultModel.description + " Code:" + faultSPN);
                    if (getResources().getString(R.string.default_fault).equalsIgnoreCase(faultModel.description)) {
                        break;
                    }
                    DatabaseProvider.getInstance(ctx).addFault(faultModel);
                    int faultAlertCount = SharePref.getInstance(ctx).getFaultAlertCount();
                    SharePref.getInstance(ctx).setFaultAlertCount(faultAlertCount + 1);
                    mParameter.FaultSPN = !Objects.equals(faultSPN, "") ? faultSPN : mParameter.FaultSPN;
                    mParameter.FaultDescription = faultModel.description;
                    break;
                case "Timing Advance":
                    mParameter.TimingAdvance = cmdResult;
                    break;
                case "Air/Fuel Ratio":
                    mParameter.AirFuelRatio = cmdResult;
                    break;
                case "Ambient Air Tempreture":
                    mParameter.AmbientAirTempreture = cmdResult;
                    break;
                case "Control Module Power Supply":
                    mParameter.ControlModulePowerSupply = cmdResult;
                    break;
                case "Engine Oil Tempreture":
                    mParameter.EngineOilTempreture = cmdResult;
                    break;
                case "Engine RPM":
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    if (cmdResult != null && cmdResult.length() > 0) {
                        cmdResult = cmdResult.substring(0, cmdResult.length() - 3);
                    }
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    int rpm = Integer.valueOf(cmdResult);
                    LogUtil.d(TAG, "Engine RPM:" + rpm);
                    mParameter.RPM = rpm;
                    //mParameter.EngineRPM = cmdResult;
                    break;
                case "Engine Runtime":
                    mParameter.EngineRuntime = cmdResult;
                    break;
                case "Fuel Pressure":
                    mParameter.FuelPressure = cmdResult;
                    break;
                case "Fuel Rail Pressure":
                    mParameter.FuelRailPressure = cmdResult;
                    break;
                case "Timeout":
                    mParameter.Timeout = cmdResult;
                    break;
                case "Trouble Codes":
                    mParameter.TroubleCodes = cmdResult;
                    break;
                case "Wideband Air/Fuel Ratio":
                    mParameter.WidebandAirFuelRatio = cmdResult;
                    break;
                case "Throttle Position":
                    mParameter.ThrottlePosition = cmdResult;
                    break;
                case "Vehicle Identification Number (VIN)":
                    mParameter.VehicleIdentificationNumber = cmdResult;
                    break;
                case "Engine Load":
                    mParameter.EngineLoad = cmdResult;
                    break;
                case "Fuel Type":
                    mParameter.FuelType = cmdResult;
                    LogUtil.d(TAG, "Fuel Type :" + cmdResult);
                    break;
                case "Fuel Consumption Rate":
                    mParameter.FuelConsumptionRate = cmdResult;
                    break;
                case "Fuel Level":
                    mParameter.FuelLevel = cmdResult;
                    break;
                case "Long Term Fuel Trim Bank1":
                    mParameter.LongTermFuelTrimBank1 = cmdResult;
                    break;
                case "Long Term Fuel Trim Bank2":
                    mParameter.LongTermFuelTrimBank2 = cmdResult;
                    break;
                case "Short Term Fuel Trim Bank1":
                    mParameter.ShortTermFuelTrimBank1 = cmdResult;
                    break;
                case "Short Term Fuel Trim Bank2":
                    mParameter.ShortTermFuelTrimBank2 = cmdResult;
                    break;
                case "Fuel Ration":
                    mParameter.FuelRation = cmdResult;
                    break;
                case "Intake Mainfold Pressure":
                    mParameter.IntakeMainfoldPressure = cmdResult;
                    break;
                case "Air Intake Temperature":
                    mParameter.AirIntakeTemperature = cmdResult;
                    break;
                case "Engine Coolant Temperature":
                    mParameter.EngineCoolantTemperature = cmdResult;
                    break;
                case "Vehicle Speed":
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    if (cmdResult != null && cmdResult.length() > 0) {
                        cmdResult = cmdResult.substring(0, cmdResult.length() - 4);
                    }
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    float speed = Float.valueOf(cmdResult);
                    mParameter.Speed = speed;
                    mParameter.VehicleSpeed = speed;
                    LogUtil.d(TAG, "Vehicle Speed :" + speed);
                    break;
                case "Mass Air Flow":
                    mParameter.MassAirFlow = cmdResult;
                    LogUtil.d(TAG, "Mass Air Flow :" + cmdResult);
                    break;
                case "Relative accelerator pedal position":
                    if (TextUtils.isEmpty(cmdResult)) {
                        return;
                    }
                    //mParameter.AccelPedal = cmdResult;
                    LogUtil.d(TAG, "Relative accelerator pedal position:" + cmdResult);
                    break;
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            LogUtil.e(TAG, "Into number format exception :" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.e(TAG, "Into exception :" + e.getMessage());
            e.printStackTrace();
        }
    }

    private BroadcastReceiver mSignOutReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            stopService();
        }
    };

    /**
     * Stop OBD connection and queue processing.
     */
    public void stopService() {
        LogUtil.d(TAG, "Stopping service..");
        unregisterReceiver(
                mSignOutReceiver);
        notificationManager.cancel(NOTIFICATION_ID);
        jobsQueue.clear();
        isRunning = false;
        AcceleratorApplication.sIsAbdapterConnected = false;
        AbstractGatewayService.sIgnitionStatusCallback.onConnectionStatusChange(false);
        if (sock != null) {
            // close socket
            try {
                sock.close();
                sock = null;
            } catch (IOException e) {
                LogUtil.e(TAG, e.getMessage());
            }
        }
        stopSelf();
        onDestroy();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public boolean isRunning() {
        return isRunning;
    }

    public static void saveLogcatToFile(Context context, String devemail) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{devemail});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "OBD2 Reader Debug Logs");

        StringBuilder sb = new StringBuilder();
        sb.append("\nManufacturer: ").append(Build.MANUFACTURER);
        sb.append("\nModel: ").append(Build.MODEL);
        sb.append("\nRelease: ").append(Build.VERSION.RELEASE);

        emailIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());

        String fileName = "OBDReader_logcat_" + System.currentTimeMillis() + ".txt";
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + File.separator + "OBD2Logs");
        if (dir.mkdirs()) {
            File outputFile = new File(dir, fileName);
            Uri uri = Uri.fromFile(outputFile);
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

            LogUtil.d("savingFile", "Going to save logcat to " + outputFile);
            //emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(Intent.createChooser(emailIntent, "Pick an Email provider").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

            try {
                @SuppressWarnings("unused")
                Process process = Runtime.getRuntime().exec("logcat -f " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private int getSpeedCount(String speed) {
        if (!TextUtils.isEmpty(speed)) {
            if (speed != null) {

                mSpeedCount = SharePref.getInstance(getApplicationContext()).getItem(Constants.SPEEDING, 0);
                Float speedingInt = Float.parseFloat(speed);
                if (speedingInt < 100) {
                    isSpeedLowered = true;
                }

                if (speedingInt > 100 && isSpeedLowered) {
                    mSpeedCount++;
                    SharePref.getInstance(getApplicationContext()).addItem(Constants.SPEEDING, mSpeedCount);
                    isSpeedLowered = false;
                }
                return mSpeedCount;
            }
        }
        return 0;
    }
}
