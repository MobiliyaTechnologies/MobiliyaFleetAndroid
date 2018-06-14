package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
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
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.DateUtils;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import static com.mobiliya.fleet.utils.CommonUtil.deletAllDatabaseTables;
import static com.mobiliya.fleet.utils.CommonUtil.trimCache;
import static com.mobiliya.fleet.utils.Constants.GET_USER_URL;
import static com.mobiliya.fleet.utils.Constants.LAST_SYNC_DATE;
import static com.mobiliya.fleet.utils.Constants.SYNC_TIME;

@SuppressWarnings({"ALL", "unused"})
public class SettingsActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "SettingsActivity";
    private SharePref mPref;
    private TextView mUserName_tv;
    private TextView mCompanyName_tv;
    private TextView mOfflineStorage_tv;
    private TextView mDataSyncTime_tv;
    private volatile int mSyncTime;
    private TextView mAboutUs_tv;
    private TextView mUserInfo_tv;
    private TextView mVehicleInfo_tv;
    private TextView mLastSync_tv;
    private int mDefaultSize;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initViews();
        mPref = SharePref.getInstance(this);
        mSyncTime = mPref.getItem(Constants.KEY_SYNC_DATA_TIME, Constants.SYNC_DATA_TIME);
        if (SharePref.getInstance(getBaseContext()).getMemorySize() == 0) {
            SharePref.getInstance(getBaseContext()).setMemorySize(Constants.SET_DEFAULT_MEMORY_SIZE);
        }
        mDefaultSize = mPref.getMemorySize();
        mOfflineStorage_tv.setText(String.valueOf(mDefaultSize) + " MB");
        setSyncTimeOnViews();

        if (CommonUtil.isNetworkConnected(getBaseContext())) {
            getUserDetails();
        } else {
            setValues(mPref.getUser());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    private void initViews() {
        mUserName_tv = (TextView) findViewById(R.id.user_name);
        mCompanyName_tv = (TextView) findViewById(R.id.company_name);
        mAboutUs_tv = (TextView) findViewById(R.id.aboutUs);
        mAboutUs_tv.setOnClickListener(this);
        mUserInfo_tv = (TextView) findViewById(R.id.userInfo);
        mUserInfo_tv.setOnClickListener(this);
        mVehicleInfo_tv = (TextView) findViewById(R.id.vehicleInfo);
        mVehicleInfo_tv.setOnClickListener(this);
        mOfflineStorage_tv = (TextView) findViewById(R.id.offline_storage);
        mLastSync_tv = (TextView) findViewById(R.id.last_sync);

        mDataSyncTime_tv = (TextView) findViewById(R.id.data_sync_time);
        ImageView ivOfflineStorageSub = (ImageView) findViewById(R.id.offline_storage_time_sub);
        ivOfflineStorageSub.setOnClickListener(this);
        ImageView ivOfflineStorageAdd = (ImageView) findViewById(R.id.offline_storage_time_add);
        ivOfflineStorageAdd.setOnClickListener(this);
        ImageView ivDataSyncTimeSub = (ImageView) findViewById(R.id.data_sync_time_sub);
        ivDataSyncTimeSub.setOnClickListener(this);
        ImageView ivDataSyncTimeAdd = (ImageView) findViewById(R.id.data_sync_time_add);
        ivDataSyncTimeAdd.setOnClickListener(this);
        Button btSignOut = (Button) findViewById(R.id.btn_sign_out);
        btSignOut.setOnClickListener(this);
        LinearLayout ivBackButton = (LinearLayout) findViewById(R.id.back_button);
        ivBackButton.setOnClickListener(this);
    }

    private void setValues(User user) {
        LogUtil.d(TAG, "setValues");
        mUserName_tv.setText("" + user.getFirstName() + " " + user.getLastName());
        mCompanyName_tv.setText(mPref.getItem(Constants.KEY_COMPANY_NAME, ""));

        String date = mPref.getItem(LAST_SYNC_DATE, "NA");
        if (!"NA".equalsIgnoreCase(date)) {
            String synceddate = DateUtils.getDateForLastsync(date);
            mLastSync_tv.setText(synceddate);
            LogUtil.d(TAG, "setValues set last sync date: " + synceddate);
        } else {
            LogUtil.d(TAG, "setValues set last sync date: Data not in sync");
            mLastSync_tv.setText("Data not sync");
        }
    }

    private void getUserDetails() {
        {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setIndeterminate(true);
            dialog.setMessage("Please wait....");
            dialog.setCancelable(false);
            dialog.show();

            String email = mPref.getItem(Constants.PREF_EMAIL);
            String access_token = mPref.getItem(Constants.PREF_ACCESS_TOKEN);

            String USER_URL = Constants.getIdentityURLs(getApplicationContext(), GET_USER_URL) + "?email=" + email;
            LogUtil.d(TAG, "Get User details for email:" + email);
            LogUtil.d(TAG, "Get User details for URL:" + USER_URL);

            JSONObject jsonBody = new JSONObject();
            try {
                VolleyCommunicationManager.getInstance().SendRequest(USER_URL, Request.Method.GET, "", this, new VolleyCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        if (result != null) {
                            try {

                                ResponseModel response = new ResponseModel();
                                response.setData(result.get("data"));
                                response.setMessage(result.getString("message"));
                                dialog.dismiss();
                                JSONArray responseArray = (JSONArray) response.getData();
                                JSONObject responseData = (JSONObject) responseArray.get(0);
                                User user = mPref.convertUserResponse(responseData);
                                mPref.setUserData(user);
                                setValues(user);
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
                        Toast.makeText(SettingsActivity.this, getString(R.string.failed_to_get_userInfo),
                                Toast.LENGTH_LONG).show();
                        User user = mPref.getUser();
                        setValues(user);
                    }
                });
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onBackPressed() {
        finishCurrectActivity();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.userInfo:
                startActivityForResult(new Intent(SettingsActivity.this, UserInfoActivity.class), Constants.DASHBOARD_REQUEST_CODE);
                overridePendingTransition(R.anim.enter, R.anim.leave);
                break;
            case R.id.aboutUs:
                startActivity(new Intent(SettingsActivity.this, AboutUsActivity.class));
                overridePendingTransition(R.anim.enter, R.anim.leave);
                break;
            case R.id.btn_sign_out:
                Intent intent = new Intent(Constants.SIGNOUT);
                sendBroadcast(intent);
                mPref.clearPreferences();
                deletAllDatabaseTables(getBaseContext());
                try {
                    trimCache(getBaseContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                setResult(Constants.SETTINGS_RESULT_CODE);
                startNextActivity(new SignInActivity());
                finish();
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                break;
            case R.id.vehicleInfo:
                startNextActivity(new VehicleInfoActivity());
                overridePendingTransition(R.anim.enter, R.anim.leave);
                break;
            case R.id.back_button:
                finishCurrectActivity();
                break;
            case R.id.data_sync_time_add:
                if (mSyncTime < 120) {
                    switch (mSyncTime) {
                        case 20:
                            mSyncTime = 30;
                            break;
                        case 30:
                            mSyncTime = 60;
                            break;
                        case 60:
                            mSyncTime = 120;
                            break;
                    }
                } else {
                    LogUtil.d(TAG, "Sync time should not be greter then 30 min");
                }
                setSyncTimeOnViews();
                broadcastToMessage();
                break;
            case R.id.data_sync_time_sub:
                if (mSyncTime > 20) {
                    switch (mSyncTime) {
                        case 120:
                            mSyncTime = 60;
                            break;
                        case 60:
                            mSyncTime = 30;
                            break;
                        case 30:
                            mSyncTime = 20;
                            break;
                    }
                } else {
                    LogUtil.d(TAG, "Sync time should not be greter then 30 min");
                }
                setSyncTimeOnViews();
                broadcastToMessage();
                break;
            case R.id.offline_storage_time_sub:
                if (mDefaultSize > 10) {
                    mDefaultSize--;
                    String size_minus = String.valueOf(mDefaultSize) + " MB";
                    setOfflineStorageOnViews(size_minus);

                } else {
                    LogUtil.d(TAG, "cannot set less then 10 MB storage");
                }
                break;
            case R.id.offline_storage_time_add:
                if (mDefaultSize < 100) {
                    mDefaultSize++;
                    String size_add = String.valueOf(mDefaultSize) + " MB";
                    setOfflineStorageOnViews(size_add);
                } else {
                    LogUtil.d(TAG, "cannot set more then 99 MB storage");
                }
                break;
        }

    }

    private void finishCurrectActivity() {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }


    private void setOfflineStorageOnViews(String size) {
        mOfflineStorage_tv.setText(size);
        mPref.setMemorySize(mDefaultSize);

    }

    @SuppressLint("SetTextI18n")
    private void setSyncTimeOnViews() {
        mPref.addItem(Constants.KEY_SYNC_DATA_TIME, mSyncTime);
        if (mSyncTime == Constants.SYNC_DATA_TIME || mSyncTime == 30) {
            mDataSyncTime_tv.setText("" + mSyncTime + " Sec");
        } else {
            mDataSyncTime_tv.setText("0" + Math.round(mSyncTime/60) + " Min");
        }
    }

    private void broadcastToMessage() {
        LogUtil.d(TAG, "broadcast sync time change message with delay " + mSyncTime);
        Intent intent = new Intent(Constants.LOCAL_SERVICE_RECEIVER_ACTION_NAME);
        intent.putExtra(Constants.MESSAGE, SYNC_TIME);
        getApplicationContext().sendBroadcast(intent);
        //LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void startNextActivity(Activity activity) {
        LogUtil.d(TAG, "startNextActivity");
        Intent intent;
        if (activity != null) {
            intent = new Intent(SettingsActivity.this, activity.getClass());
            startActivityForResult(intent, Constants.DASHBOARD_REQUEST_CODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.DASHBOARD_REQUEST_CODE && (resultCode == Constants.SIGN_OUT_RESULT_CODE)) {
            setResult(Constants.SIGN_OUT_RESULT_CODE);
            finish();
        }
    }
}
