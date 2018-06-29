package com.mobiliya.fleet.io;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.mobiliya.fleet.AcceleratorApplication;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.Parameter;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.net.IOTHubCommunication;
import com.mobiliya.fleet.services.CustomIgnitionStatusInterface;
import com.mobiliya.fleet.services.GPSTracker;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import roboguice.service.RoboService;

import static com.mobiliya.fleet.utils.Constants.GPS_DISTANCE;
import static com.mobiliya.fleet.utils.TripManagementUtils.SendLocalTripsToServer;

@SuppressWarnings({"ALL", "unused"})
public abstract class AbstractGatewayService extends RoboService{
    static final Queue<Message> EventsQueue = new LinkedList<Message>();
    private static final String TAG = AbstractGatewayService.class.getName();
    public static CustomIgnitionStatusInterface sIgnitionStatusCallback;
    final BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();
    private final IBinder binder = new AbstractGatewayServiceBinder();
    protected Parameter mParameter;
    protected boolean isVehicleActivity = false;
    protected PowerManager powerManager;
    protected PowerManager.WakeLock wakeLock;
    protected GPSTracker gpsTracker;
    Context ctx;
    boolean isRunning = false;
    Long queueCounter = 0L;
    private Timer timer;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            LogUtil.d(TAG, "Service BroadcastReceiver onReceive to change data sync time");
            // You can also include some extra data.
            String value = i.getExtras().get(Constants.MESSAGE).toString();

            if (value.equalsIgnoreCase(Constants.SYNC_TIME)) {
                int delay = SharePref.getInstance(ctx).getItem(Constants.KEY_SYNC_DATA_TIME, Constants.SYNC_DATA_TIME);
                initDataSyncTimer(delay);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        isRunning = true;
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "Creating service..");
        //getLocation();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String adapter = SharePref.getInstance(this).getItem(Constants.PREF_ADAPTER_PROTOCOL, "");
            Boolean mIsSkipEnabled = SharePref.getInstance(this).getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
            if (mIsSkipEnabled) {
                adapter = "";
            }
            LogUtil.d(TAG, "Creating service for Oreo and above devices");
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(new NotificationChannel("com.mobiliya.fleet.io.AbstractGatewayService"
                    , "App Service", NotificationManager.IMPORTANCE_DEFAULT));
            nm.createNotificationChannel(new NotificationChannel("com.mobiliya.fleet.io.AbstractGatewayServiceInfo"
                    , "Download Info", NotificationManager.IMPORTANCE_DEFAULT));
            Notification.Builder mBuilder = new Notification.Builder(this)
                    .setContentTitle("" + adapter + " Service running")
                    .setSmallIcon(R.drawable.notificationwhite)
                    .setColor(this.getResources().getColor(R.color.dashboard_header_start_color))
                    .setAutoCancel(false)
                    .setOngoing(true)
                    .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                    .setOnlyAlertOnce(true);
            NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);

            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = "mobiliya Fleet";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mBuilder.setChannelId(CHANNEL_ID);
            notificationManager.createNotificationChannel(mChannel);

            startForeground(1, mBuilder.build());
        }
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "AcceleratorLockTag");
        wakeLock.acquire();
        gpsTracker = GPSTracker.getInstance(this);

        gpsTracker.getLocation();
        LogUtil.d(TAG, "call initDataSyncTimer.");
        initDataSyncTimer(Constants.SYNC_DATA_TIME);
        registerReceiver(
                mMessageReceiver, new IntentFilter(Constants.LOCAL_SERVICE_RECEIVER_ACTION_NAME));
        LogUtil.d(TAG, "Service created.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "Destroying service...");
        super.onDestroy();
        IOTHubCommunication.getInstance(getBaseContext()).closeClient();
        try {
            timer.cancel();
            timer = null;
            mParameter = null;
            unregisterReceiver(
                    mMessageReceiver);
            wakeLock.release();
        } catch (Exception e) {
            LogUtil.i(TAG, "broadcastReceiver is already unregistered");
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean queueEmpty() {
        return jobsQueue.isEmpty();
    }

    /**
     * This method will add a job to the queue while setting its ID to the
     * internal queue counter.
     *
     * @param job the job to queue.
     */
    public void queueJob(ObdCommandJob job) {
        queueCounter++;
        LogUtil.d(TAG, "Adding job[" + queueCounter + "] to queue..");

        job.setId(queueCounter);
        try {
            jobsQueue.put(job);
            LogUtil.d(TAG, "Job queued successfully.");
        } catch (InterruptedException e) {
            job.setState(ObdCommandJob.ObdCommandJobState.QUEUE_ERROR);
            LogUtil.e(TAG, "Failed to queue job.");
        }
    }

    public void setContext(Context c) {
        ctx = c;
    }

    public void setIsVehicleActivity(boolean mIsVehicleActivity) {
        isVehicleActivity = mIsVehicleActivity;
    }

    abstract protected void executeQueue() throws InterruptedException;

    abstract public void startService() throws IOException;

    abstract public void stopService();

    /*method to initialize timer task to upload vehicle information to iotHub*/
    private void initDataSyncTimer(final int delay) {
        LogUtil.d(TAG, "initDataSyncTimer called with delay:" + delay);
        if (timer != null) {
            timer.cancel();
            timer = null;
            timer = new Timer();
        } else {
            timer = new Timer();
        }

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LogUtil.i("AbstractGatewayService Timer", "timer callback callled ->" + delay + " Sec");
                processData();
            }
        }, 0, (delay * 1000));
    }

    public void processData() {
        LogUtil.d(TAG, "processData called");
        if (!SharePref.getInstance(this).getBooleanItem(Constants.PREF_MOVED_TO_DASHBOARD, false)) {
            LogUtil.d(TAG, "Return since acitivity is not moved to dashboard");
            return;
        }
        if (ctx == null) {
            return;
        }
        if (mParameter == null) {
            mParameter = new Parameter();
        }
        //gpsTracker.getLocation();
        if (gpsTracker.getIsGPSTrackingEnabled()) {
            mParameter.Latitude = String.valueOf(gpsTracker.getLatitude());
            mParameter.Longitude = String.valueOf(gpsTracker.getLongitude());

            LogUtil.d(TAG, "GPSTracker latitude: " + gpsTracker.getLatitude() + " longitude: " + gpsTracker.getLongitude());
        }
        mParameter.TenantId = SharePref.getInstance(ctx).getUser().getTenantId();
        mParameter.UserId = SharePref.getInstance(ctx).getUser().getId();
        mParameter.VehicleId = SharePref.getInstance(ctx).getVehicleID();
        mParameter.isConnected = !SharePref.getInstance(ctx).getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);

        Trip trip = DatabaseProvider.getInstance(getApplicationContext()).getCurrentTrip();
        if (trip != null) {
            if (SharePref.getInstance(ctx).getBooleanItem(GPS_DISTANCE, true)) {
                float distance = Float.valueOf(String.format("%.2f", gpsTracker.getDistance()));
                if (distance >= 0) {
                    LogUtil.d(TAG, "GPS_DISTANCE :" + distance);
                    mParameter.Distance = distance;
                }
            }else {
                LogUtil.d(TAG, "Adapter distance from dongle Distance:" + mParameter.Distance);
            }
            CommonUtil.milesDriven(getApplicationContext(), mParameter.Distance);
            mParameter.TripId = trip.commonId;
        } else {
            mParameter.TripId = "NA";
        }
        if (gpsTracker.getLatitude() != 0.0 && gpsTracker.getLongitude() != 0.0) {
            addParameterToDatabase(mParameter);
        }

        LogUtil.i("AbstractGatewayService Timer", "Parameter.TenantId ->" + mParameter.TenantId);
        if (CommonUtil.isOnline(getBaseContext())) {
            LogUtil.i("AbstractGatewayService Timer", "broadcastToIoTHub ->");
            broadcastToIoTHub();
            LogUtil.i("AbstractGatewayService", "Sending trip details");
            SendLocalTripsToServer(ctx);
        }else {
            LogUtil.i("AbstractGatewayService", "No internet Connection ->");
        }
    }

    /*method to send message to iotHub*/
    private void broadcastToIoTHub() {
        Parameter[] parameters = DatabaseProvider.getInstance(AbstractGatewayService.this).getParameterData();
        if (parameters != null && parameters.length > 0) {
            LogUtil.d("AbstractGatewayService Timer", "broadcastToIoTHub called ->");
            IOTHubCommunication.getInstance(AbstractGatewayService.this).SendMessage(parameters);
            if (mParameter != null && !TextUtils.isEmpty(mParameter.ParameterDateTime)) {
                LogUtil.d(TAG, "ParameterDateTime Time:" + mParameter.ParameterDateTime);
            }
        }
    }

    private void addParameterToDatabase(Parameter param) {
        DatabaseProvider.getInstance(ctx).addParameter(param);
    }

    public boolean isAdapterConnected() {
        return AcceleratorApplication.sIsAbdapterConnected;
    }

    public void connectToAdapter() {
    }

    public void getVehicleData() {
    }

    public void startTimer() {
    }

    public class AbstractGatewayServiceBinder extends Binder {
        public AbstractGatewayService getService() {
            return AbstractGatewayService.this;
        }
    }

}
