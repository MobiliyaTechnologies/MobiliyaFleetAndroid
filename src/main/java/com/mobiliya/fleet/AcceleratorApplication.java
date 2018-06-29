package com.mobiliya.fleet;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import com.mobiliya.fleet.io.AbstractGatewayService;
import com.mobiliya.fleet.io.J1939DongleService;
import com.mobiliya.fleet.io.ObdGatewayService;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

/**
 * Created by prashant on 23-02-2018.
 */

@SuppressWarnings({"ALL", "unused"})
public class AcceleratorApplication extends Application {
    private static final String TAG = "Application";
    //private Intent mServiceIntent;
    public static boolean sIsAbdapterConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "Application onCreate");
        //org.apache.log4j.BasicConfigurator.configure();
        /*SharePref mPref = SharePref.getInstance(this);
        String mAdapterProtocol = mPref.getItem(Constants.PREF_ADAPTER_PROTOCOL, "");
        try {
            if (mAdapterProtocol.equals(Constants.OBD)) {
                mServiceIntent = new Intent(this, ObdGatewayService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mServiceIntent);
                } else {
                    startService(mServiceIntent);
                }
            } else {
                mServiceIntent = new Intent(this, J1939DongleService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mServiceIntent);
                } else {
                    startService(mServiceIntent);
                }
            }
        } catch (IllegalStateException e) {
            LogUtil.d(TAG, "IllegalStateException while create service from application class");
            e.printStackTrace();
        }*/

    }

    @Override
    public void onTerminate() {
        LogUtil.d(TAG, "Application onTerminate");
        super.onTerminate();
    }
}
