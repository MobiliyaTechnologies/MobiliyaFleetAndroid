package com.mobiliya.fleet.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.mobiliya.fleet.io.J1939DongleService;
import com.mobiliya.fleet.io.ObdGatewayService;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";
    private Intent mServiceIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        LogUtil.d(TAG,"onReceive called");
        try {
            SharePref mPref = SharePref.getInstance(context);
            String mAdapterProtocol = mPref.getItem(Constants.PREF_ADAPTER_PROTOCOL, "");
            LogUtil.d(TAG,"onReceive protocol: "+mAdapterProtocol);
            if (mAdapterProtocol.equals(Constants.OBD)) {
                if(!CommonUtil.isServiceRunning(ObdGatewayService.class, context)) {
                    mServiceIntent = new Intent(context, ObdGatewayService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(mServiceIntent);
                    } else {
                        context.startService(mServiceIntent);
                    }
                }
            } else if(mAdapterProtocol.equals(Constants.J1939)) {
                if(!CommonUtil.isServiceRunning(J1939DongleService.class, context)) {
                    mServiceIntent = new Intent(context, J1939DongleService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(mServiceIntent);
                    } else {
                        context.startService(mServiceIntent);
                    }
                }
            }
        } catch (IllegalStateException e) {
            LogUtil.d(TAG, "IllegalStateException while create service from application class");
            e.printStackTrace();
        } catch (Exception e){
            LogUtil.d(TAG, "Exception while create service from application class");
            e.printStackTrace();
        }
    }
}