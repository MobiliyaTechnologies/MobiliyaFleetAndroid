package com.mobiliya.fleet.adapters;

import android.app.Activity;

import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.io.AbstractGatewayService;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.services.CustomIgnitionStatusInterface;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.NotificationManagerUtil;
import com.mobiliya.fleet.utils.SharePref;
import com.mobiliya.fleet.utils.TripManagementUtils;
import com.mobiliya.fleet.utils.TripStatus;

public class CustomIgnitionListenerTracker {
    public static void showDialogOnIgnitionChange(final Activity activity) {

        try {
            AbstractGatewayService.sIgnitionStatusCallback = new CustomIgnitionStatusInterface() {
                @Override
                public void onConnectionStatusChange(boolean isConnected) {
                    try {
                        Trip trip = DatabaseProvider.getInstance(activity.getBaseContext()).getCurrentTrip();
                        boolean mIsSkipEnabled = SharePref.getInstance(activity).getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
                        if (trip != null) {
                            if (CommonUtil.isAppInBackground(activity.getApplicationContext())) {
                                    NotificationManagerUtil.getInstance().dismissDeviceDisConnectedNotification(activity.getBaseContext());
                                    NotificationManagerUtil.getInstance().createDeviceDisConnectedNotification(activity.getApplicationContext());

                                /*if (isConnected) {
                                    if (trip.status == TripStatus.Pause.getValue()) {
                                        TripManagementUtils.resumeTrip(activity.getApplicationContext());
                                    }
                                }
                                else {
                                    if (trip.status == TripStatus.Start.getValue()) {
                                        TripManagementUtils.pauseTrip(activity);
                                    }
                                }*/
                            } else if (!CommonUtil.isAppInBackground(activity.getApplicationContext()) && !mIsSkipEnabled) {
                                if (!isConnected && trip.status == TripStatus.Start.getValue()) {
                                    TripManagementUtils.showDongleDisconnectedDialog(activity);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            };
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
