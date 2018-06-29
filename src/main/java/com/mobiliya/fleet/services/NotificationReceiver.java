package com.mobiliya.fleet.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.NotificationManagerUtil;
import com.mobiliya.fleet.utils.TripManagementUtils;
import com.mobiliya.fleet.utils.TripStatus;

import static com.mobiliya.fleet.utils.Constants.ADAPTER_NOTIFICATION;
import static com.mobiliya.fleet.utils.Constants.ONGOINGTRIP_NOTIFICATION;


/**
 * Created by Darshana Pandit on 19-04-2018.
 */

@SuppressWarnings("DefaultFileTemplate")
public class NotificationReceiver extends BroadcastReceiver {

    private static final String TAG = NotificationReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("NotificationReceiver", "I Arrived!!!!");

        String action = intent.getAction();
        if ("PAUSE".equals(action)) {
            String type = intent.getStringExtra("TYPE");
            if(ONGOINGTRIP_NOTIFICATION.equals(type)){
                pause(context);
                LogUtil.d(TAG,"inside Trip pause");
                NotificationManagerUtil.getInstance().dismissDeviceDisConnectedNotification(context);
            }

            if(ADAPTER_NOTIFICATION.equals(type)){
                pause(context);
                LogUtil.d(TAG,"inside Adpter pause");
                NotificationManagerUtil.getInstance().dismissDeviceDisConnectedNotification(context);
            }


        } else if ("STOP".equals(action)) {
            stop(context);
            NotificationManagerUtil.getInstance().dismissNotification(context);
        }
    }


    public void pause(Context context){
        Trip trip = DatabaseProvider.getInstance(context).getCurrentTrip();
        if (trip != null) {
            if (trip.status == TripStatus.Pause.getValue()) {
                TripManagementUtils.resumeTrip(context);
            } else {
                TripManagementUtils.pauseTripFromNotification((context));
            }

            Intent notificationintent = new Intent();
            notificationintent.setAction(Constants.NOTIFICATION_PAUSE_BROADCAST);
            context.sendBroadcast(notificationintent);
        }
    }

    public void stop(Context context){
        Trip trip = DatabaseProvider.getInstance(context).getCurrentTrip();
        if (trip != null) {
            if (trip.status == TripStatus.Stop.getValue()) {
                Toast.makeText(context, context.getString(R.string.trip_is_already_stopped), Toast.LENGTH_LONG).show();
            } else {
                if (TripManagementUtils.stopTripFromNotification(context) > 0) {
                    Intent notificationintent = new Intent();
                    notificationintent.setAction(Constants.NOTIFICATION_STOP_BROADCAST);
                    context.sendBroadcast(notificationintent);

                }
            }
        }
    }
}