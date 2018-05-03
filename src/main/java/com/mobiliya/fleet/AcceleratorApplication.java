package com.mobiliya.fleet;

import android.app.Application;
import android.content.Intent;

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
    private Intent mServiceIntent;
    public static boolean sIsAbdapterConnected = false;

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d(TAG, "Application onCreate");
        SharePref mPref = SharePref.getInstance(this);
        String mAdapterProtocol = mPref.getItem(Constants.PREF_ADAPTER_PROTOCOL, "");
        try {
            if (!mAdapterProtocol.isEmpty() && mAdapterProtocol.equals(Constants.J1939)) {
                mServiceIntent = new Intent(this, J1939DongleService.class);
                startService(mServiceIntent);
            } else if (!mAdapterProtocol.isEmpty() && mAdapterProtocol.equals(Constants.OBD)) {
                mServiceIntent = new Intent(this, ObdGatewayService.class);
                startService(mServiceIntent);
            }
        } catch (IllegalStateException e) {
            LogUtil.d(TAG, "IllegalStateException while create service from application class");
            e.printStackTrace();
        }

    }

    @Override
    public void onTerminate() {
        LogUtil.d(TAG, "Application onTerminate");
        stopService(mServiceIntent);
        super.onTerminate();
    }
}
