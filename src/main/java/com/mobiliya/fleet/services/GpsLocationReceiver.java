package com.mobiliya.fleet.services;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import com.mobiliya.fleet.activity.ShowDialogActivity;
import com.mobiliya.fleet.utils.Constants;

import java.util.List;

import static android.content.Context.LOCATION_SERVICE;

@SuppressWarnings({"WeakerAccess", "DefaultFileTemplate"})
public class GpsLocationReceiver extends BroadcastReceiver {
    boolean isGPSEnabled = false;
    LocationManager locationManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            //getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (!isGPSEnabled) {
                if (isAppOnForeground(context)) {
                    Intent showDilog = new Intent(context, ShowDialogActivity.class);
                    showDilog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(showDilog);
                }
            }
        }
        if (intent.getAction().matches(Constants.LOCATION_NOT_CHANGED)) {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            //getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!isGPSEnabled) {
                if (isAppOnForeground(context)) {
                    Intent showDilog = new Intent(context, ShowDialogActivity.class);
                    showDilog.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(showDilog);
                }
            }
        }
    }


    public static boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }
}
