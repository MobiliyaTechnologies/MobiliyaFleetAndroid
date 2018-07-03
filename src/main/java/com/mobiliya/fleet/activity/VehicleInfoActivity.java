package com.mobiliya.fleet.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.models.ResponseModel;
import com.mobiliya.fleet.models.Vehicle;
import com.mobiliya.fleet.location.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static com.mobiliya.fleet.activity.ConfigureUrlActivity.getFleetUrl;
import static com.mobiliya.fleet.utils.Constants.REGISTRATION_NUMBER;
import static com.mobiliya.fleet.utils.Constants.VEHICLES;

public class VehicleInfoActivity extends AppCompatActivity {

    private static final String TAG = "VehicleInfoActivity";
    private TextView mBrandName_tv, mModel_tv, mFuelType_tv, mYearOfManifacture_tv;
    private TextView mVehicleRegistrationNo_tv, mStatus_tv, mVehicleName_tv;
    private SharePref mPref;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vehicle_info);
        mPref = SharePref.getInstance(this);
        mBrandName_tv = (TextView) findViewById(R.id.brand_name);
        mModel_tv = (TextView) findViewById(R.id.model);
        mFuelType_tv = (TextView) findViewById(R.id.fuel_type);
        mYearOfManifacture_tv = (TextView) findViewById(R.id.year_manifacture);
        mVehicleRegistrationNo_tv = (TextView) findViewById(R.id.vehicle_registration_no);
        mStatus_tv = (TextView) findViewById(R.id.device_status);
        mVehicleName_tv = (TextView) findViewById(R.id.vehicle_name);
        LinearLayout backButton = (LinearLayout) findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrectActivity();
            }
        });
        initViews();
        mPref = SharePref.getInstance(this);
        String mTenantId = mPref.getItem(Constants.TENANT_ID, "");
        LogUtil.d(TAG, "Tenant id is: " + mTenantId);

        if (CommonUtil.isNetworkConnected(getBaseContext())) {
            getVehicleDetails();
        } else {
            Vehicle vehicle = mPref.getVehicleData();
            setValues(vehicle);
        }
    }

    private void initViews() {
        String vehicleRegistrationNo = mPref.getItem(Constants.KEY_VEHICLE_REGISTRATION_NO, "");
        mVehicleRegistrationNo_tv.setText(vehicleRegistrationNo);
    }

    private void finishCurrectActivity() {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    @Override
    public void onBackPressed() {
        finishCurrectActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        try {
            registerReceiver(
                    mParameterReceiver, new IntentFilter(Constants.DASHBOARD_RECEIVER_ACTION_NAME));

            CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        } catch (Exception e) {
            LogUtil.i(TAG, "broadcastReceiver is already registered");
        }
    }

    @Override
    protected void onPause() {
        try {
            unregisterReceiver(
                    mParameterReceiver);
            CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
        } catch (Exception e) {
            LogUtil.i(TAG, "broadcastReceiver is already unregistered");
        }
        super.onPause();
    }

    private BroadcastReceiver mParameterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d(TAG, "onReceive() parameter received");
            // Get extra data included in the Intent
            @SuppressWarnings("unchecked") HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra(Constants.DASHBOARD_RECEIVER_NAME);
            String status = hashMap.get(Constants.STATUS);
            LogUtil.d(TAG, "Status : " + status);
            mStatus_tv.setText(status);
        }
    };

    private void getVehicleDetails() {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setIndeterminate(true);
            dialog.setMessage("Please wait....");
            dialog.setCancelable(false);
            dialog.show();

            String mTenantId = mPref.getItem(Constants.TENANT_ID, "");
            String mVehicleRegistrationNo = mPref.getItem(Constants.KEY_VEHICLE_REGISTRATION_NO, "");
            LogUtil.d(TAG, "Tenant Id is:" + mTenantId);
            LogUtil.d(TAG, "Vehicle registration no is:" + mVehicleRegistrationNo);
            String VEHICLE_URL = getFleetUrl(getApplicationContext()) +
                    mTenantId + VEHICLES + REGISTRATION_NUMBER + mVehicleRegistrationNo;
            LogUtil.d(TAG, "Get vehicle details:" + VEHICLE_URL);
            try {

                VolleyCommunicationManager.getInstance().SendRequest(VEHICLE_URL, Request.Method.GET, "", this, new VolleyCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        if (result != null) {
                            try {

                                ResponseModel response = new ResponseModel();
                                response.setData(result.get("data"));
                                response.setMessage(result.getString("message"));
                                JSONArray responseArray = (JSONArray) response.getData();
                                JSONObject responseData = (JSONObject) responseArray.get(0);
                                Vehicle vehicle = mPref.convertVehicleResponse(responseData);
                                setValues(vehicle);
                                dialog.dismiss();
                            } catch (JSONException e) {
                                dialog.dismiss();
                                e.printStackTrace();
                            } catch (Exception e) {
                                dialog.dismiss();
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onError(VolleyError result) {
                        dialog.dismiss();
                        Toast.makeText(VehicleInfoActivity.this, getString(R.string.failed_to_get_vehicleInfo),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();
            }
    }

    private void setValues(Vehicle vehicle) {
        LogUtil.d(TAG, "values set");
        mBrandName_tv.setText(vehicle.getBrandName());
        mModel_tv.setText(vehicle.getModel());
        mVehicleRegistrationNo_tv.setText(vehicle.getRegistrationNo());
        mYearOfManifacture_tv.setText(vehicle.getYearOfManufacture());
        mFuelType_tv.setText(vehicle.getFuleType());
        mVehicleName_tv.setText(vehicle.getModel());
    }
}
