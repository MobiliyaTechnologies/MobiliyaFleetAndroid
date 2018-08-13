package com.mobiliya.fleet.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.adapters.VehicleDataAdapter;
import com.mobiliya.fleet.location.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class VehicleHealthActivity extends BaseActivity {

    private static final String TAG = "VehicleHealthActivity";

    private RecyclerView mRecyclerViewParams;
    private VehicleDataAdapter mVehicleDataAdapter;
    private LinkedHashMap<String, String> mParamsList = new LinkedHashMap<>();
    private LinkedHashMap<String, String> mParamsListSlow = new LinkedHashMap<>();

    private LinearLayout mErrorLayout, mDataLayout;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_vehicle_health);
        mErrorLayout = findViewById(R.id.error_layout);
        mDataLayout = findViewById(R.id.data_id);

        mRecyclerViewParams = findViewById(R.id.recycler_view);
        mRecyclerViewParams.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerViewParams.setNestedScrollingEnabled(false);

        mRecyclerViewParams.setNestedScrollingEnabled(false);
        mVehicleDataAdapter = new VehicleDataAdapter(this, mParamsList);
        mRecyclerViewParams.setAdapter(mVehicleDataAdapter);

        LinearLayout mBackButton_ll = findViewById(R.id.back_button);
        mBackButton_ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrentActivity();
            }
        });

    }

    private void finishCurrentActivity() {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }


    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy called");
        unregisterReceiver(
                mParameterReceiver);
        super.onDestroy();
    }

    protected void onResume() {
        super.onResume();
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        LogUtil.d(TAG, "Resuming..");
        try {
            registerReceiver(
                    mParameterReceiver, new IntentFilter(Constants.LOCAL_RECEIVER_ACTION_NAME));
            CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        } catch (Exception e) {
            Log.i("", "broadcastReceiver is already unregistered");
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    private BroadcastReceiver mParameterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d(TAG, "onReceive() parameter received");
            HashMap<String, String> hashMapSlowInterval = (HashMap<String, String>) intent.
                    getSerializableExtra(Constants.LOCAL_RECEIVER_NAME_SLOW);
            HashMap<String, String> hashMapFastInterval = (HashMap<String, String>) intent.
                    getSerializableExtra(Constants.LOCAL_RECEIVER_NAME);
            try {
                hashMapFastInterval.remove("Speedcount");
                hashMapFastInterval.remove("RPM");
                hashMapFastInterval.remove("Vehicle Speed");
                hashMapFastInterval.get("Distance since codes cleared");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (hashMapFastInterval != null && hashMapFastInterval.size() > 0) {
                mParamsList.putAll(hashMapFastInterval);

            }
            if (hashMapSlowInterval != null && hashMapSlowInterval.size() > 0) {
                mParamsListSlow.putAll(hashMapSlowInterval);
            }
            assignValues(mParamsList, mParamsListSlow);
        }
    };

    //assign values to adapter
    public void assignValues(Map<String, String> commandResult, Map<String, String> commandResultSlow) {
        LogUtil.d(TAG, "assignValues");

        LinkedHashMap<String, String> finalList = new LinkedHashMap<>();
        if (commandResult.size() > 0) {
            finalList.put(Constants.VEHICLE_PARAMETERS, "");
        }
        finalList.putAll(commandResult);
        if (commandResultSlow.size() > 0) {
            finalList.put(Constants.ADDITIONAL_PARAMETERS, "");
            finalList.putAll(commandResultSlow);
        }

        if (commandResult != null && commandResult.size() != 0) {
            mErrorLayout.setVisibility(View.GONE);
            mDataLayout.setVisibility(View.VISIBLE);
            mVehicleDataAdapter.addListItem(finalList);
        } else {
            mErrorLayout.setVisibility(View.VISIBLE);
            mDataLayout.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        finishCurrentActivity();
    }
}
