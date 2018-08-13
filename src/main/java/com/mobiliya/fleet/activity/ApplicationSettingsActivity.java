package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.NotificationManagerUtil;
import com.mobiliya.fleet.utils.SharePref;

import static com.mobiliya.fleet.utils.CommonUtil.deletAllDatabaseTables;
import static com.mobiliya.fleet.utils.CommonUtil.trimCache;
import static com.mobiliya.fleet.utils.Constants.SYNC_TIME;

public class ApplicationSettingsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "ApplicationSettingsActivity";
    private RadioGroup radioGroup;
    private RadioButton radioGps, radioAdapter;
    private TextView mOfflineStorage_tv;
    private TextView mDataSyncTime_tv;
    private volatile int mSyncTime;
    private int mDefaultSize;
    private SharePref pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_application_settings);
        pref = SharePref.getInstance(this);
        mSyncTime = pref.getItem(Constants.KEY_SYNC_DATA_TIME, Constants.SYNC_DATA_TIME);
        if (SharePref.getInstance(getBaseContext()).getMemorySize() == 0) {
            SharePref.getInstance(getBaseContext()).setMemorySize(Constants.SET_DEFAULT_MEMORY_SIZE);
        }
        mOfflineStorage_tv = (TextView) findViewById(R.id.offline_storage);
        mDataSyncTime_tv = (TextView) findViewById(R.id.data_sync_time);
        LinearLayout ivOfflineStorageSub = (LinearLayout) findViewById(R.id.offline_storage_time_sub);
        ivOfflineStorageSub.setOnClickListener(this);
        LinearLayout ivOfflineStorageAdd = (LinearLayout) findViewById(R.id.offline_storage_time_add);
        ivOfflineStorageAdd.setOnClickListener(this);
        LinearLayout ivDataSyncTimeSub = (LinearLayout) findViewById(R.id.data_sync_time_sub);
        ivDataSyncTimeSub.setOnClickListener(this);
        LinearLayout ivDataSyncTimeAdd = (LinearLayout) findViewById(R.id.data_sync_time_add);
        ivDataSyncTimeAdd.setOnClickListener(this);
        radioGroup = (RadioGroup) findViewById(R.id.rd_distance);
        radioGps = (RadioButton) findViewById(R.id.gps_distance);
        radioAdapter = (RadioButton) findViewById(R.id.adapDistance);
        pref = SharePref.getInstance(getBaseContext());
        if (pref.getBooleanItem(Constants.GPS_DISTANCE, true)) {
            radioGps.setChecked(true);
            radioAdapter.setChecked(false);
        } else {
            radioGps.setChecked(false);
            radioAdapter.setChecked(true);
        }
        setSyncTimeOnViews();
        mDefaultSize = pref.getMemorySize();
        mOfflineStorage_tv.setText(String.valueOf(mDefaultSize) + " MB");
    }


    public void startHelpActivity(View view) {
        startActivity(new Intent(ApplicationSettingsActivity.this, HelpSettingsActivity.class));
        overridePendingTransition(R.anim.enter, R.anim.leave);
    }

    public void saveSelected(View view) {
        boolean mIsSkipEnabled = pref.getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
        int selectedId = radioGroup.getCheckedRadioButtonId();

        if (mIsSkipEnabled) {
            if (selectedId == R.id.adapDistance) {
                Toast.makeText(getBaseContext(), "Cannot get distance when adapter is disconnected", Toast.LENGTH_SHORT).show();
            } else if (selectedId == R.id.gps_distance) {
                Toast.makeText(getBaseContext(), getString(R.string.data_save), Toast.LENGTH_SHORT).show();
            }
            pref.addItem(Constants.GPS_DISTANCE, true);
        } else {
            Trip trip = DatabaseProvider.getInstance(getApplicationContext()).getCurrentTrip();
            if (trip != null) {
                Toast.makeText(getBaseContext(), getString(R.string.app_setting_on_going_trip), Toast.LENGTH_LONG).show();
                return;
            }
            if (selectedId == R.id.adapDistance) {
                pref.addItem(Constants.GPS_DISTANCE, false);
            } else if (selectedId == R.id.gps_distance) {
                pref.addItem(Constants.GPS_DISTANCE, true);
            }
            Toast.makeText(getBaseContext(), getString(R.string.data_save), Toast.LENGTH_SHORT).show();
        }
    }

    public void back(View view) {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.data_sync_time_add:
                if (mSyncTime < 120) {
                    switch (mSyncTime) {
                        case 5:
                            mSyncTime = 10;
                            break;
                        case 10:
                            mSyncTime = 20;
                            break;
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
                if (mSyncTime > 5) {
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
                        case 20:
                            mSyncTime = 10;
                            break;
                        case 10:
                            mSyncTime = 5;
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

    @SuppressLint("SetTextI18n")
    private void setSyncTimeOnViews() {
        pref.addItem(Constants.KEY_SYNC_DATA_TIME, mSyncTime);
        if (mSyncTime <= 30) {
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
    }

    private void setOfflineStorageOnViews(String size) {
        mOfflineStorage_tv.setText(size);
        pref.setMemorySize(mDefaultSize);
    }
}
