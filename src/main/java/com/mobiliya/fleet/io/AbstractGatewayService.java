package com.mobiliya.fleet.io;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.google.inject.Inject;
import com.mobiliya.fleet.AcceleratorApplication;
import com.mobiliya.fleet.activity.VehicleHealthAcitivity;
import com.mobiliya.fleet.db.DatabaseProvider;
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
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import roboguice.service.RoboService;

import static com.mobiliya.fleet.utils.TripManagementUtils.SendLocalTripsToServer;

@SuppressWarnings({"ALL", "unused"})
public abstract class AbstractGatewayService extends RoboService {
    static final int NOTIFICATION_ID = 1;
    private static final String TAG = AbstractGatewayService.class.getName();
    private final IBinder binder = new AbstractGatewayServiceBinder();
    @Inject
    final NotificationManager notificationManager = null;
    Context ctx;
    boolean isRunning = false;
    Long queueCounter = 0L;
    final BlockingQueue<ObdCommandJob> jobsQueue = new LinkedBlockingQueue<>();
    protected Parameter mParameter;
    protected boolean isVehicleActivity = false;
    private Timer timer;
    static final Queue<Message> EventsQueue = new LinkedList<Message>();
    protected PowerManager powerManager;
    protected PowerManager.WakeLock wakeLock;
    protected GPSTracker gpsTracker;
    public static CustomIgnitionStatusInterface sIgnitionStatusCallback;
    // Run the executeQueue in a different thread to lighten the UI thread
    private final Thread t = new Thread(new Runnable() {
        @Override
        public void run() {
            /*try {
                executeQueue();
            } catch (InterruptedException e) {
                t.interrupt();
            }*/
        }
    });

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
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "AcceleratorLockTag");
        gpsTracker = GPSTracker.getInstance(this);
        wakeLock.acquire();
        t.start();
        LogUtil.d(TAG, "call initDataSyncTimer.");
        initDataSyncTimer(Constants.SYNC_DATA_TIME);
        registerReceiver(
                mMessageReceiver, new IntentFilter(Constants.LOCAL_SERVICE_RECEIVER_ACTION_NAME));
        LogUtil.d(TAG, "Service created.");
    }

    @Override
    public void onDestroy() {
        LogUtil.d(TAG, "Destroying service...");
        notificationManager.cancel(NOTIFICATION_ID);
        t.interrupt();
        LogUtil.d(TAG, "Service destroyed.");
        super.onDestroy();
        try {
            //timer.cancel();
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

    /**
     * Show a notification while this service is running.
     */
    void showNotification(String contentTitle, String contentText) {
        final PendingIntent contentIntent = PendingIntent.getActivity(ctx, 0, new Intent(ctx, VehicleHealthAcitivity.class), 0);
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(ctx);
        notificationBuilder.setContentTitle(contentTitle)
                .setContentText(contentText).setSmallIcon(com.mobiliya.fleet.R.drawable.ic_btcar)
                .setContentIntent(contentIntent)
                .setWhen(System.currentTimeMillis());
        // can cancel?
        if (true) {
            notificationBuilder.setOngoing(true);
        } else {
            notificationBuilder.setAutoCancel(true);
        }
        if (false) {
            notificationBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        }
        if (true) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.getNotification());
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

    @SuppressWarnings("unused")
    public class AbstractGatewayServiceBinder extends Binder {
        public AbstractGatewayService getService() {
            return AbstractGatewayService.this;
        }
    }

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

    /*method to initialize timer task to upload vehicle information to iotHub*/
    protected void initDataSyncTimer(final int delay) {
        /*if (!SharePref.getInstance(this).getBooleanItem(Constants.PREF_MOVED_TO_DASHBOARD, false)) {
            LogUtil.d(TAG, "Return since acitivity is not moved to dashboard");
            return;
        }*/

        LogUtil.d(TAG, "initDataSyncTimer called with delay:" + delay);
        if (timer != null) {
            timer.cancel();
            timer = null;
            timer = new Timer();
        } else {
            timer = new Timer();
        }

        int delayTime = delay;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LogUtil.i("AbstractGatewayService Timer", "timer callback callled ->" + delay+" Sec");
                processData();
            }
        }, 0, (delayTime * 1000));
    }

    public void processData() {
        LogUtil.d(TAG, "processData called");
        if (ctx == null) {
            return;
        }
        if (mParameter == null) {
            mParameter = new Parameter();
        }

        if (gpsTracker.getIsGPSTrackingEnabled()) {
            mParameter.Latitude = String.valueOf(gpsTracker.getLatitude());
            mParameter.Longitude = String.valueOf(gpsTracker.getLongitude());

            LogUtil.d(TAG, "GPSTracker latitude: " + gpsTracker.getLatitude() + " longitude: " + gpsTracker.getLongitude());
        }
        if (CommonUtil.isNetworkConnected(AbstractGatewayService.this)) {
            LogUtil.i("AbstractGatewayService", "Sending trip details");
            SendLocalTripsToServer(ctx);
        }
        if (mParameter != null) {

            mParameter.TenantId = SharePref.getInstance(ctx).getUser().getTenantId();
            mParameter.UserId = SharePref.getInstance(ctx).getUser().getId();
            mParameter.VehicleId = SharePref.getInstance(ctx).getVehicleID();
            if (!SharePref.getInstance(ctx).getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false)) {
                mParameter.isConnected = true;
            } else {
                mParameter.isConnected = false;
            }
            Trip trip = DatabaseProvider.getInstance(getApplicationContext()).getCurrentTrip();
            if (trip != null) {
                mParameter.TripId = trip.commonId;
                CommonUtil.milesDriven(getApplicationContext(), String.valueOf(mParameter.Distance));
            } else {
                mParameter.TripId = "NA";
            }
            addParameterToDatabase(mParameter);
            LogUtil.i("AbstractGatewayService Timer", "Paramters ->" + mParameter.TenantId);
            if (CommonUtil.isNetworkConnected(AbstractGatewayService.this)) {
                LogUtil.i("AbstractGatewayService Timer", "broadcastToIoTHub ->");
                broadcastToIoTHub();
            }
        } else {
            boolean flag = SharePref.getInstance(ctx).getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
            if (flag && CommonUtil.isNetworkConnected(AbstractGatewayService.this)) {
                LogUtil.i("AbstractGatewayService Timer", "broadcastToIoTHub ->");
                broadcastToIoTHub();
            }
        }
    }

    /*method to send message to iotHub*/
    private void broadcastToIoTHub() {
        Parameter[] parameters = DatabaseProvider.getInstance(AbstractGatewayService.this).getParameterData();
        if (parameters != null && parameters.length > 0) {
            try {
                LogUtil.d("AbstractGatewayService Timer", "broadcastToIoTHub called ->");

                IOTHubCommunication.getInstance(AbstractGatewayService.this).SendMessage(parameters);
                if (mParameter != null && !TextUtils.isEmpty(mParameter.ParameterDateTime)) {
                    LogUtil.d(TAG, "ParameterDateTime Time:" + mParameter.ParameterDateTime);
                }
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addParameterToDatabase(Parameter param) {
        DatabaseProvider.getInstance(ctx).addParameter(param);
        /*if (!CommonUtil.checkDbSizeExceeds(ctx)) {
            DatabaseProvider.getInstance(ctx).addParameter(param);
        } else {
            Intent intent = new Intent(Constants.DATABASEFULL);
            ctx.sendBroadcast(intent);
            //LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
        }*/
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

    public void stopVehicleData() {
    }

    public void sendAdapterStatusBroadcast(boolean status) {
        Intent intent = new Intent(Constants.ADAPTER_STATUS);
        ctx.sendBroadcast(intent);
        //LocalBroadcastManager.getInstance(ctx).sendBroadcast(intent);
    }

}
