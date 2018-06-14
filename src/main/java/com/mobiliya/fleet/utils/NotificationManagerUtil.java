package com.mobiliya.fleet.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.activity.SplashActivity;
import com.mobiliya.fleet.services.NotificationReceiver;

import static android.content.Context.NOTIFICATION_SERVICE;

@SuppressWarnings({"deprecation", "WeakerAccess", "UnusedParameters", "DefaultFileTemplate"})
public class NotificationManagerUtil {
    public NotificationManager notificationManager;
    public Notification.Builder mBuilder;


    private static NotificationManagerUtil myObj;

    /**
     * Create private constructor
     */
    private NotificationManagerUtil() {

    }

    /**
     * Create a static method to get instance.
     */
    public static NotificationManagerUtil getInstance() {
        if (myObj == null) {
            myObj = new NotificationManagerUtil();
        }
        return myObj;
    }

    public void createNotification(Context cxt) {
        Intent intentApp = new Intent(cxt, SplashActivity.class);
        PendingIntent pintentApp = PendingIntent.getActivity(cxt, (int) System.currentTimeMillis(), intentApp, 0);

        Intent intentStop = new Intent(cxt, NotificationReceiver.class);
        intentStop.setAction("STOP");
        PendingIntent pintentStop = PendingIntent.getBroadcast(cxt, (int) System.currentTimeMillis(), intentStop, 0);

        Intent intentPause = new Intent(cxt, NotificationReceiver.class);
        intentPause.setAction("PAUSE");
        PendingIntent pintentPause = PendingIntent.getBroadcast(cxt, (int) System.currentTimeMillis(), intentPause, 0);

        // Build notification
        SharePref.getInstance(cxt).addItem(Constants.TIME_ONGOING,"0");
        mBuilder = new Notification.Builder(cxt)
                .setContentTitle("Trip is Ongoing")
                .setContentText("Trip time: " + "0")
                .setSmallIcon(R.drawable.notificationwhite)
                .setColor(cxt.getResources().getColor(R.color.dashboard_header_start_color))
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pintentApp)
                .addAction(R.drawable.ic_action_pause, "Pause", pintentPause)
                .addAction(R.drawable.ic_action_stop, "Stop", pintentStop);
        notificationManager = (NotificationManager) cxt.getSystemService(NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";// The id of the channel.
            CharSequence name = "mobiliya Fleet";// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mBuilder.setChannelId(CHANNEL_ID);
            notificationManager.createNotificationChannel(mChannel);
        }

        notificationManager.notify(123458, mBuilder.build());

    }

    public void dismissNotification(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(123458);
        notificationManager = null;
        SharePref.getInstance(context).addItem(Constants.TIME_ONGOING, "0.0");
    }

    public void dismissNotificationFromUpdate(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(123458);
        notificationManager = null;
    }

    public void upDateNotification(Context cxt, String diff) {
        try {
            if (mBuilder != null && notificationManager != null) {
                mBuilder.setContentText("Trip time: " + diff);
                notificationManager.notify(123458, mBuilder.build());
            } else {
                dismissNotificationFromUpdate(cxt);
                createNotification(cxt);
                upDateNotification(cxt, diff);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
