package com.mobiliya.fleet.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.adapters.EngineFaultAdapter;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.FaultModel;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.SharePref;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"ALL", "unused"})
public class DiagnosticActivity extends Activity {
    //Diagnostic Section
    private Map<String, String> mParamList = null;
    private Gson mGson = new Gson();
    private SharePref mPref;
    private List<FaultModel> mFaultslist = new ArrayList<>();
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();
    private HashMap<String, Integer> faultCount = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_diagnostic);
        mPref = SharePref.getInstance(this);
        LinearLayout llBackButton = (LinearLayout) findViewById(R.id.back_button);
        llBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
        });
    }

    public void setTextAndVisibility(TextView txtView, String value) {
        if (txtView != null) {
            txtView.setText(value);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        mPref.setFaultAlertCount(0);
        mFaultslist = DatabaseProvider.getInstance(getApplicationContext()).getFaultList();
        try {
            getFaultCount(mFaultslist);
        }catch (Exception e){
            e.printStackTrace();
        }
        RecyclerView faultListView = (RecyclerView) findViewById(R.id.recycler_view);
        faultListView.setLayoutManager(new LinearLayoutManager(this));
        if (mFaultslist != null) {
            faultListView.setAdapter(new EngineFaultAdapter(this, mFaultslist,faultCount));
        }
    }

    private void getFaultCount(List<FaultModel> list) {
        if (list == null) {
            return;
        } else {
            for (FaultModel model : list) {
                Integer count = faultCount.get(model.description);
                if (count == null) {
                    faultCount.put(model.description, 1);
                }else {
                    count++;
                    faultCount.put(model.description, count);
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    private BroadcastReceiver mDiagnosticReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra(Constants.LOCAL_RECEIVER_NAME);
            Type type = new TypeToken<Map<String, String>>() {
            }.getType();
            mParamList = mGson.fromJson(message, type);
        }
    };

    private BroadcastReceiver LOGOUT_Reciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            String action_finish = arg1.getStringExtra("FINISH");

            if (action_finish.equalsIgnoreCase("ACTION.FINISH.LOGOUT")) {
                finish();
            }
        }
    };
}
