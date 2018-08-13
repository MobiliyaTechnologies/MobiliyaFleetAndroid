package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.j1939.api.enums.ConnectionStates;
import com.mobiliya.fleet.AcceleratorApplication;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.ApiCallBackListener;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.adapters.DashboardHLVAdapter;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.io.AbstractGatewayService;
import com.mobiliya.fleet.io.J1939DongleService;
import com.mobiliya.fleet.io.ObdGatewayService;
import com.mobiliya.fleet.location.GPSTracker;
import com.mobiliya.fleet.location.GpsLocationReceiver;
import com.mobiliya.fleet.models.DriverScore;
import com.mobiliya.fleet.models.LastTrip;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.models.Vehicle;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.DateUtils;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.NotificationManagerUtil;
import com.mobiliya.fleet.utils.SharePref;
import com.mobiliya.fleet.utils.TripManagementUtils;
import com.mobiliya.fleet.utils.TripStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.mobiliya.fleet.activity.ConfigureUrlActivity.getTripServiceUrl;
import static com.mobiliya.fleet.utils.CommonUtil.getTimeDiff;
import static com.mobiliya.fleet.utils.CommonUtil.showToast;

public class DashboardActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "DashboardActivity";
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private String mProtocol = null;
    //private SharePref pref = null;
    private AbstractGatewayService mService;
    private TextView mAdapterNameTextView;
    private TextView mAdapterConnectionStateTextView;
    private TextView mVehicleModeltv;
    private TextView mRegistrationNotv;
    private TextView mYearOfManufacturtv;
    private TextView mFueltv;
    private LinearLayout mReconnectBtn;
    private boolean mIsAdapterConnected = false;
    private Boolean mIsServiceConnected = false;
    private RelativeLayout mRelLayout;
    private BluetoothAdapter mBluetoothAdapter;
    private String oldSpn = "", oldFmi = "";
    private LinearLayout mFaultsLayout;
    private TextView mFaultstv;
    public static PopupWindow sPopupWindow;
    public View mPopupView;
    public int mCurrentX, mCurrentY;
    private TextView mMilestv;
    private TextView mTripduration;
    private TextView mStartLocationtv;
    private TextView mStartTimetv;
    private TextView mEndLocationtv;
    private TextView mEndTimetv, mVehicleColor;
    private TextView mTripDateTime, mTextAdapterStatus;
    private LinearLayout mBtStatusLayout;
    public static ImageButton sTv_btn_pause;
    public Trip mTrip;
    Trip mLastSynctrip = null;
    private View mWithlasttrip;
    private View mWithoutlasttrip;
    public TextView mMilesDriven;
    private TextView mTripTime, mScore, mScoreLastTrip;
    private Boolean mIsSkipEnabled = false;
    private ProgressBar mProgressDriver, mProgressDriverLastTrip;
    private LinearLayout mFloatingMilesLayout, mFloatingTimeLayout;
    private LinearLayout mLastTripLayout, mScoreLayout, mScoreLayoutLastTrip;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        intiViews();
        LogUtil.d(TAG, "onCreate");
        mIsSkipEnabled = SharePref.getInstance(this).getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mIsSkipEnabled) {
            if (!mBluetoothAdapter.isEnabled()) {
                LogUtil.d(TAG, "Bluetooth is not enable, trying to enable..");
                mBluetoothAdapter.enable();
            }
        }
        bindViews();
        mProtocol = SharePref.getInstance(this).getItem(Constants.PREF_ADAPTER_PROTOCOL);
        String mAdapterName = SharePref.getInstance(this).getItem(Constants.PREF_BT_DEVICE_NAME, "");
        LogUtil.d(TAG, "Protocol called: " + mProtocol);
        initAdapter();
        if (mIsSkipEnabled) {
            mTextAdapterStatus.setText("Not connected");
        } else {
            if (!mAdapterName.isEmpty()) {
                mAdapterNameTextView.setText(mAdapterName);
            }
            mReconnectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    reConnectToAdapter();
                }
            });
        }
        connectToAdapetrService();
        setupBadge();
        //GPSTracker.getInstance(this).getLocation();
    }

    private void intiViews() {
        mRelLayout = (RelativeLayout) findViewById(R.id.dashbord_layout);
        mFueltv = (TextView) findViewById(R.id.fuel_type);
        mYearOfManufacturtv = (TextView) findViewById(R.id.year_manifacture);
        mVehicleModeltv = (TextView) findViewById(R.id.vehicle_model);
        mRegistrationNotv = (TextView) findViewById(R.id.vehicle_number);
        mAdapterNameTextView = (TextView) findViewById(R.id.device_id_text_view);
        mAdapterConnectionStateTextView = (TextView) findViewById(R.id.device_connection_state_text_view);
        mReconnectBtn = (LinearLayout) findViewById(R.id.retry_connection_image_button);
        mBtStatusLayout = (LinearLayout) findViewById(R.id.bt_status_layout);
        mTextAdapterStatus = (TextView) findViewById(R.id.click_to_connect);
        mVehicleColor = (TextView) findViewById(R.id.vehicleColor);
        LinearLayout btnCloseFaults = (LinearLayout) findViewById(R.id.close_faults);
        btnCloseFaults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFaultsLayout.setVisibility(View.GONE);
            }
        });
        mFaultsLayout = (LinearLayout) findViewById(R.id.faults_layout);
        mFaultsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFaultsLayout.setVisibility(View.GONE);
                startActivity(new Intent(DashboardActivity.this, DiagnosticActivity.class));
            }
        });
        mFaultstv = (TextView) findViewById(R.id.faults);
        mWithoutlasttrip = (View) findViewById(R.id.without_last_trip);
        mLastTripLayout = (LinearLayout) findViewById(R.id.without_last_trip).findViewById(R.id.lasttrip);
        mLastTripLayout.setOnClickListener(this);

        mWithlasttrip = (View) findViewById(R.id.with_last_trip);
        mTripDateTime = (TextView) findViewById(R.id.tv_triptime);
        mMilestv = (TextView) findViewById(R.id.tv_miles);
        mTripduration = (TextView) findViewById(R.id.tv_time);
        mStartLocationtv = (TextView) findViewById(R.id.tv_startlocation);
        mStartTimetv = (TextView) findViewById(R.id.tv_starttime);
        mEndLocationtv = (TextView) findViewById(R.id.tv_endlocation);
        mEndTimetv = (TextView) findViewById(R.id.tv_endttime);
        mScoreLayout = (LinearLayout) findViewById(R.id.scrore);
        mScoreLayout.setOnClickListener(this);
        mScoreLayoutLastTrip = (LinearLayout) findViewById(R.id.scroreLastTrip);
        mScore = (TextView) findViewById(R.id.score);
        mScoreLastTrip = (TextView) findViewById(R.id.scoreLastTrip);
        mScoreLastTrip.setOnClickListener(this);
        mProgressDriver = (ProgressBar) findViewById(R.id.progressScore);
        mProgressDriver.setOnClickListener(this);
        mProgressDriverLastTrip = (ProgressBar) findViewById(R.id.progressScoreLastTrip);
        mProgressDriverLastTrip.setOnClickListener(this);
    }

    private void bindViews() {
        Vehicle vehicle = SharePref.getInstance(this).getVehicleData();
        mRegistrationNotv.setText(vehicle.getRegistrationNo());
        mFueltv.setText(vehicle.getFuleType());
        mVehicleModeltv.setText(vehicle.getModel());
        mYearOfManufacturtv.setText(vehicle.getYearOfManufacture());
        mVehicleColor.setText(vehicle.getVehicleColor());
    }

    private void reConnectToAdapter() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mIsSkipEnabled) {
            if (!mBluetoothAdapter.isEnabled()) {
                LogUtil.d(TAG, "Bluetooth is not enable, trying to enable..");
                mAdapterConnectionStateTextView.setText(getResources().getString(R.string.status_bluetooth_connecting));
                mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
                mReconnectBtn.setVisibility(View.GONE);
                mBluetoothAdapter.enable();
            } else if (mService != null) {
                LogUtil.i(TAG, "reConnectToAdapter button clicked");
                mAdapterConnectionStateTextView.setText(getResources().getString(R.string.status_bluetooth_connecting));
                mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
                mReconnectBtn.setVisibility(View.GONE);
                mService.connectToAdapter();
            }
        }else {
            if (mService != null) {
                LogUtil.i(TAG, "reConnectToAdapter button clicked");
                mAdapterConnectionStateTextView.setText(getResources().getString(R.string.status_bluetooth_connecting));
                mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
                mReconnectBtn.setVisibility(View.GONE);
                mService.connectToAdapter();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsServiceConnected) {
            LogUtil.d(TAG, "onDestroy() called");
            unbindService(mServiceConn);
        }
        if (sPopupWindow != null) {
            if (sPopupWindow.isShowing())
                sPopupWindow.dismiss();
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        GPSTracker.getInstance(this).getLocation();
        //setAdapterConnectionStatus();
        if (AcceleratorApplication.sIsAbdapterConnected) {
            mAdapterConnectionStateTextView.setText(getResources().getText(R.string.status_obd_connected));
            mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
            mReconnectBtn.setVisibility(View.GONE);
        } else {
            reConnectToAdapter();
            mAdapterConnectionStateTextView.setVisibility(View.GONE);
            mReconnectBtn.setVisibility(View.VISIBLE);
        }
        setScoreWithTripsView(null);
        getSpeedLimit();
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        try {
            // Register for broadcasts on BluetoothAdapter state change
            IntentFilter intentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mReceiverBT, intentFilter);
            //Receiver to show message on db full
            registerReceiver(
                    mDatabaseMessageReceiver, new IntentFilter(Constants.DATABASEFULL));

            registerReceiver(
                    mParameterStatusReceiver, new IntentFilter(Constants.DASHBOARD_RECEIVER_ACTION_NAME));
            registerReceiver(mParameterReceiver, new IntentFilter(Constants.LOCAL_RECEIVER_ACTION_NAME));
            IntentFilter filter = new IntentFilter();
            filter.addAction(Constants.NOTIFICATION_PAUSE_BROADCAST);
            filter.addAction(Constants.NOTIFICATION_STOP_BROADCAST);
            registerReceiver(mNotificationReceiver, filter);

            CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        } catch (Exception e) {
            LogUtil.i(TAG, "broadcastReceiver is already registered");
        }

        //Code to show popup dialog if trip is ongoing
        mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        if (mTrip != null) {
            //initDataSyncTimer();
            if (mTrip.status != TripStatus.Stop.getValue()) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        showFloatingWindow();
                        if (mTrip.status == TripStatus.Pause.getValue()) {
                            sTv_btn_pause.setImageDrawable(getDrawable(R.drawable.play_icon));
                        } else {
                            sTv_btn_pause.setImageDrawable(getDrawable(R.drawable.pause));
                        }
                    }
                }, 100);

            } else {
                if (sPopupWindow != null) {
                    if (sPopupWindow.isShowing()) {

                        sPopupWindow.dismiss();
                    }
                }
            }
        } else {
            if (sPopupWindow != null) {
                if (sPopupWindow.isShowing()) {

                    sPopupWindow.dismiss();
                }
            }
        }
        getLastTrip();
        setProgressScore();
        getDriverScore();
    }

    public void getSpeedLimit() {
        String URL = getTripServiceUrl(getApplicationContext());
        String mTenantId = SharePref.getInstance(this).getUser().getTenantId();
        URL = URL + mTenantId + "/" + "config";
        LogUtil.d(TAG, "getDriverScore() URL:" + URL);
        try {
            VolleyCommunicationManager.getInstance().SendRequest(URL, Request.Method.GET, "", this, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {
                            if (result.getString("message").equals("Success")) {
                                String data = result.get("data").toString();
                                JSONObject jsonObj = new JSONObject(data);
                                int speedLimit = (int) jsonObj.get("speedLimit");
                                SharePref.getInstance(getApplicationContext()).setSpeedLimit(speedLimit);
                                LogUtil.d(TAG, "Limit:" + speedLimit);
                            } else {
                                LogUtil.d(TAG, "Got result but without success");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        LogUtil.d(TAG, "driver result is null");
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    LogUtil.d(TAG, "onError");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setProgressScore() {
        DriverScore Score = SharePref.getInstance(this).getDriverScore();
        int score = Score.driverBehaviour.driverScore;
        if (score != 0) {
            mScore.setText(Integer.toString(score));
            mScoreLastTrip.setText(Integer.toString(score));
            mProgressDriver.setProgress(score);
            mProgressDriverLastTrip.setProgress(score);
        }
    }

    @Override
    protected void onPause() {
        /*if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }*/
        try {
            unregisterReceiver(mReceiverBT);
            unregisterReceiver(
                    mDatabaseMessageReceiver);
            unregisterReceiver(
                    mParameterStatusReceiver);
            unregisterReceiver(mParameterReceiver);
            unregisterReceiver(mNotificationReceiver);
            CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
        } catch (Exception e) {
            LogUtil.i(TAG, "broadcastReceiver is already unregistered");
        }
        if (sPopupWindow != null) {
            if (sPopupWindow.isShowing())
                sPopupWindow.dismiss();
        }
        super.onPause();
    }

    private void initAdapter() {
        // Calling the RecyclerView
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // The number of Columns
        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new DashboardHLVAdapter(DashboardActivity.this, mProtocol);
        mRecyclerView.setAdapter(mAdapter);

    }

    /*method to bind to service based on selected adapter protocol type*/
    private void connectToAdapetrService() {
        Intent mServiceIntent;
        if (mProtocol.equals(Constants.J1939)) {
            mServiceIntent = new Intent(this, J1939DongleService.class);
        } else {
            mServiceIntent = new Intent(this, ObdGatewayService.class);
        }
        bindService(mServiceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            LogUtil.i(TAG, "ServiceConnection -> onServiceConnected");
            mIsServiceConnected = true;
            mService = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            mService.setContext(DashboardActivity.this);
            mService.setIsVehicleActivity(false);
            LogUtil.d(TAG, "Starting live data");
            mIsAdapterConnected = mService.isAdapterConnected();
            if (!mIsAdapterConnected) {
                LogUtil.d(TAG, "Adapter is not connected");
                reConnectToAdapter();
            } else {
                if (mProtocol.equalsIgnoreCase(Constants.OBD)) {
                    mService.connectToAdapter();
                } else {
                    mService.getVehicleData();
                }
                LogUtil.d(TAG, "Adapter is already connected");
            }
            setAdapterConnectionStatus();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            LogUtil.d(TAG, className.toString() + " service is unbound");
        }
    };

    /*method to handle reconnet button visibility based on adapter connection status*/
    private void setAdapterConnectionStatus() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LogUtil.i(TAG, "setAdapterConnectionStatus");
                if (mService == null) {
                    mAdapterConnectionStateTextView.setVisibility(View.GONE);
                    mReconnectBtn.setVisibility(View.VISIBLE);
                    LogUtil.d(TAG, "return setAdapterConnectionStatus mService is null");
                    reConnectToAdapter();
                    return;
                }
                mIsAdapterConnected = mService.isAdapterConnected();
                LogUtil.i(TAG, "setAdapterConnectionStatus ->" + mIsAdapterConnected);
                if (mIsAdapterConnected) {
                    mAdapterConnectionStateTextView.setText(getResources().getText(R.string.status_obd_connected));
                    mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
                    mReconnectBtn.setVisibility(View.GONE);
                } else {
                    mAdapterConnectionStateTextView.setVisibility(View.GONE);
                    mReconnectBtn.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    /*method will update adapter connection status on UI*/
    public void setBTStatus(String status) {
        super.setBTStatus(status);
        if (mIsSkipEnabled) {
            mTextAdapterStatus.setText("Not connected");
            return;
        }
        LogUtil.i(TAG, "Adapter Status received -> " + status);
        if (status.equalsIgnoreCase(getResources().getString(R.string.adapter_connected))
                || status.equalsIgnoreCase(getResources().getString(R.string.connected))
                || status.equals(ConnectionStates.Connected.name())) {
            mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
            mAdapterConnectionStateTextView.setText(getResources().getString(R.string.status_bluetooth_connected));
            mReconnectBtn.setVisibility(View.GONE);
        } else if (status.equals(ConnectionStates.Connecting.name()) || status.contains(getResources().getString(R.string.connection_attempt))) {
            mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
            mAdapterConnectionStateTextView.setText(getResources().getString(R.string.status_bluetooth_connecting));
            mReconnectBtn.setVisibility(View.GONE);
        } else if (status.equalsIgnoreCase(ConnectionStates.DataTimeout.name()) || status.equalsIgnoreCase(ConnectionStates.Disconnected.name())
                || status.equalsIgnoreCase(ConnectionStates.DataError.name()) || status.equalsIgnoreCase(getResources().getString(R.string.adapter_not_connected))) {
            mAdapterConnectionStateTextView.setVisibility(View.GONE);
            mReconnectBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if (requestCode == Constants.DASHBOARD_REQUEST_CODE && (resultCode == Constants.SETTINGS_RESULT_CODE || resultCode == Constants.SIGN_OUT_RESULT_CODE)) {
            finish();
            startNextActivity(new SignInActivity());
        }
    }

    private void startNextActivity(Activity activity) {
        LogUtil.d(TAG, "startNextActivity");
        Intent intent;
        if (activity != null) {
            intent = new Intent(DashboardActivity.this, activity.getClass());
            startActivityForResult(intent, Constants.DASHBOARD_REQUEST_CODE);
        }
    }

    private BroadcastReceiver mParameterStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d(TAG, "onReceive() parameter received");
            // Get extra data included in the Intent
            @SuppressWarnings("unchecked") HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra(Constants.DASHBOARD_RECEIVER_NAME);
            String status = hashMap.get(Constants.STATUS);
            LogUtil.d(TAG, "Status : " + status);
            setBTStatus(status);
        }
    };


    @SuppressLint("InflateParams")
    public void showFloatingWindow() {
        LayoutInflater layoutInflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        mPopupView = layoutInflater.inflate(R.layout.buttons_layout, null);
        sPopupWindow = new PopupWindow(mPopupView, RelativeLayout.LayoutParams.MATCH_PARENT, (int) getResources().getDimension(R.dimen.dim_100));
        sTv_btn_pause = (ImageButton) mPopupView.findViewById(R.id.btn_pause);
        ImageButton mOpenScreen = (ImageButton) mPopupView.findViewById(R.id.start_activity);
        mFloatingMilesLayout = (LinearLayout) mPopupView.findViewById(R.id.floating_miles_layout);
        mFloatingTimeLayout = (LinearLayout) mPopupView.findViewById(R.id.floating_time_layout);
        mMilesDriven = (TextView) mPopupView.findViewById(R.id.miles_driven);
        float milage = SharePref.getInstance(getApplicationContext()).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
        mMilesDriven.setText("" + String.format("%.2f", milage) + " miles");
        mTripTime = (TextView) mPopupView.findViewById(R.id.trip_time);
        setTime();
        mFloatingMilesLayout.setOnClickListener(this);
        mFloatingTimeLayout.setOnClickListener(this);
        mOpenScreen.setOnClickListener(this);
        sTv_btn_pause.setOnClickListener(this);

        ImageButton btnStop = (ImageButton) mPopupView.findViewById(R.id.btn_stop);
        btnStop.setOnClickListener(this);

        mCurrentX = 50;
        mCurrentY = 20;
        try {
            sPopupWindow.showAtLocation(mRelLayout, Gravity.BOTTOM, mCurrentX, mCurrentY);
            sPopupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            sPopupWindow.setFocusable(true);
        } catch (WindowManager.BadTokenException e) {
            LogUtil.d(TAG, "Bad token exception");
        }
    }

    Date startDate = null;
    private BroadcastReceiver mDatabaseMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            showToast(DashboardActivity.this, context.getString(R.string.storage_message));
            /*if (startDate == null) {
                startDate = new Date();
                showToast(DashboardActivity.this, "Your offline storage is full,Please go online.");
            } else {

                long diff = new Date().getTime() - startDate.getTime();
                long diffHours = diff / (60 * 60 * 1000) % 24;
                if (diffHours == 5) {
                    showToast(DashboardActivity.this, "Your offline storage is full,Please go online.");
                }
            }*/
        }
    };

    private void setupBadge() {
        int faultAlertCount = SharePref.getInstance(this).getFaultAlertCount();

        if (faultAlertCount > 0) {
            mFaultsLayout.setVisibility(View.VISIBLE);
            try {
                int faultCount = (faultAlertCount > 99) ? 99 : faultAlertCount;
                mFaultstv.setText("" + faultCount + " " + getResources().getString(R.string._2_active_faults_1_inactive_faults));
                LogUtil.d(TAG, "faults detected count" + faultCount + " faultAlertCount" + faultAlertCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            mFaultsLayout.setVisibility(View.GONE);
        }
    }

    private BroadcastReceiver mParameterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d(TAG, "onReceive() parameter received");
            // Get extra data included in the Intent
            HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra(Constants.LOCAL_RECEIVER_NAME);
            //mParamsList = gson.fromJson(message, type);
            String spn = hashMap.get("FaultSPN");
            //String fmi = hashMap.get("FaultFMI");
            String totalHours = hashMap.get("TotalHours");
            String fuelUsed = hashMap.get("FuelUsed");

            setTime();
            setMiles();

            if (spn != null && !spn.equalsIgnoreCase("")) {
                if (!oldSpn.equalsIgnoreCase(spn)) {
                    setupBadge();
                    oldSpn = spn;
                }
            }
        }
    };


    private void setTime() {
        if (mTrip != null && mTripTime != null) {
            String diff = getTimeDiff(getBaseContext(), mTrip);
            mTripTime.setText(diff);
        }
    }


    public void getLastTrip() {
        final SharePref pref = SharePref.getInstance(this);
        TripManagementUtils.getLastTrip(DashboardActivity.this, new ApiCallBackListener() {
            @Override
            public void onSuccess(JSONObject result) {
                if (result != null) {
                    try {
                        if (result.getString("message").equals("Success")) {
                            String data = result.get("data").toString();
                            JSONArray dataarr = result.getJSONArray("data");
                            if (dataarr != null && dataarr.length() > 0) {
                                Type type = new TypeToken<List<Trip>>() {
                                }.getType();
                                Gson gson = new Gson();
                                List<Trip> tList = gson.fromJson(data, type);
                                Trip lasttrip = tList.get(0);
                                mLastSynctrip = lasttrip;
                                String start = lasttrip.startLocation.split("#")[1];
                                String end = lasttrip.endLocation;
                                if (!"NA".equals(end)) {
                                    end = lasttrip.endLocation.split("#")[1];
                                }
                                LastTrip lastTrip = new LastTrip();
                                lastTrip.miles = lasttrip.milesDriven;
                                lastTrip.tripDuration = lasttrip.tripDuration;
                                lastTrip.startLocation = start;
                                lastTrip.endLocation = end;
                                lastTrip.startTime = DateUtils.tripDetailFormat(lasttrip.startTime);
                                lastTrip.endTime = DateUtils.tripDetailFormat(lasttrip.endTime);
                                lastTrip.tripDateTime = DateUtils.dashbordTripTime(lasttrip.startTime);
                                setLastTripOnViews(lastTrip);
                                pref.saveLastTrip(lastTrip);
                                setScoreWithTripsView("true");
                            }
                        } else {
                            setScoreWithTripsView("false");

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    setScoreWithTripsView("false");

                }
            }

            @Override
            public void onError(VolleyError result) {
                setScoreWithTripsView("false");
            }
        });
    }

    public void setLastTripOnViews(LastTrip lastTrip) {
        String milesDriven = lastTrip.miles;
        if (!"NA".equals(milesDriven)) {
            mMilestv.setText(lastTrip.miles + " Miles");
        } else {
            mMilestv.setText(lastTrip.miles);
        }
        mTripduration.setText(lastTrip.tripDuration);
        mStartLocationtv.setText(lastTrip.startLocation);
        mEndLocationtv.setText(lastTrip.endLocation);
        mStartTimetv.setText(lastTrip.startTime);
        mEndTimetv.setText(lastTrip.endTime);
        mTripDateTime.setText(lastTrip.tripDateTime);
    }

    public void getDriverScore() {
        String URL = getTripServiceUrl(getApplicationContext());
        String mTenantId = SharePref.getInstance(this).getUser().getTenantId();
        String mUserId = SharePref.getInstance(this).getUser().getId();
        URL = URL + mTenantId + "/" + "driver?userId=" + mUserId;
        LogUtil.d(TAG, "getDriverScore() URL:" + URL);
        try {
            VolleyCommunicationManager.getInstance().SendRequest(URL, Request.Method.GET, "", this, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {
                            if (result.getString("message").equals("Success")) {
                                String data = result.get("data").toString();
                                JSONArray dataarr = result.getJSONArray("data");
                                if (dataarr != null && dataarr.length() > 0) {
                                    Type type = new TypeToken<List<DriverScore>>() {
                                    }.getType();
                                    Gson gson = new Gson();
                                    List<DriverScore> tList = gson.fromJson(data, type);
                                    SharePref.getInstance(DashboardActivity.this).saveDriverScore(tList);
                                    LogUtil.d(TAG, "tList");
                                    setProgressScore();
                                }

                            } else {
                                LogUtil.d(TAG, "Got result but without success");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        LogUtil.d(TAG, "driver result is null");
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    LogUtil.d(TAG, "onError");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void setScoreWithTripsView(String flag) {
        LastTrip lastTrip = SharePref.getInstance(this).getLastTrip();
        if (!("0".equalsIgnoreCase(lastTrip.miles) && "0".equalsIgnoreCase(lastTrip.startLocation) &&
                "0".equalsIgnoreCase(lastTrip.endLocation) && "0".equalsIgnoreCase(lastTrip.startTime) &&
                "0".equalsIgnoreCase(lastTrip.endTime) && "0".equalsIgnoreCase(lastTrip.tripDateTime))) {
            mWithlasttrip.setVisibility(View.GONE);
            mWithoutlasttrip.setVisibility(View.VISIBLE);
            setLastTripOnViews(lastTrip);
            return;
        }
        if (TextUtils.isEmpty(flag)) {
            LogUtil.d(TAG, "flag is empty or null");
            return;
        }
        //if (!flag) {
        if (!"true".equalsIgnoreCase(flag)) {
            mWithlasttrip.setVisibility(View.VISIBLE);
            mWithoutlasttrip.setVisibility(View.GONE);
        } else {
            mWithlasttrip.setVisibility(View.GONE);
            mWithoutlasttrip.setVisibility(View.VISIBLE);
        }
    }

    private void setMiles() {
        if (mMilesDriven != null) {
            float milage = SharePref.getInstance(getApplicationContext()).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
            mMilesDriven.setText("" + String.format("%.2f", milage) + " miles");
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scrore:
            case R.id.scoreLastTrip:
            case R.id.progressScore:
                Intent i = new Intent(DashboardActivity.this, DriverScoreActivity.class);
                startActivity(i);
                overridePendingTransition(R.anim.enter, R.anim.leave);
                break;
            case R.id.start_activity:
            case R.id.floating_miles_layout:
            case R.id.floating_time_layout:
                Intent intent = new Intent(DashboardActivity.this, TripActivity.class);//The class you want to show
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.bottom_to_top, R.anim.top_to_bottom);
                break;
            case R.id.btn_pause:
                Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
                if (trip != null) {
                    if (trip.status == TripStatus.Pause.getValue()) {
                        TripManagementUtils.resumeTrip(DashboardActivity.this);
                        sTv_btn_pause.setImageDrawable(getDrawable(R.drawable.pause));
                    } else {
                        TripManagementUtils.pauseTrip(DashboardActivity.this);
                        sTv_btn_pause.setImageDrawable(getDrawable(R.drawable.play_icon));
                    }
                }
                break;
            case R.id.btn_stop:
                showStopDialog();
                break;
            case R.id.lasttrip:
                if (mWithoutlasttrip.getVisibility() == View.VISIBLE) {
                    if (mLastSynctrip != null) {
                        Intent startdetailsPage = new Intent(DashboardActivity.this, TripDetailsActivity.class);
                        startdetailsPage.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startdetailsPage.putExtra(Constants.TRIPID, mLastSynctrip.commonId);
                        startActivity(startdetailsPage);
                        overridePendingTransition(R.anim.enter, R.anim.leave);
                    } else {
                        //showToast(this, getString(R.string.no_internet_connection));
                    }
                }
                break;
        }
    }

    public void showStopDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.stop_trip_dialog));
        alert.setMessage(getString(R.string.do_want_to_stop_trip));
        alert.setCancelable(false);
        alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                if (TripManagementUtils.stopTrip(DashboardActivity.this) > 0) {
                    if (sPopupWindow != null) {
                        try {
                            sPopupWindow.dismiss();
                        } catch (Exception ex) {
                            LogUtil.d(TAG, ex.getMessage());
                        }
                    }
                    getLastTrip();
                    NotificationManagerUtil.getInstance().dismissNotification(getBaseContext());
                }

            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.dashboard_header_start_color));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.dashboard_header_start_color));
    }

    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.NOTIFICATION_STOP_BROADCAST)) {
                if (sPopupWindow != null) {
                    try {
                        sPopupWindow.dismiss();
                        getLastTrip();
                    } catch (Exception ex) {
                        LogUtil.d(TAG, ex.getMessage());
                    }
                }
            }

            if (intent.getAction().equals(Constants.NOTIFICATION_PAUSE_BROADCAST)) {
                if (sPopupWindow != null) {
                    try {
                        Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
                        if (TripStatus.Pause.getValue() == trip.status) {
                            sTv_btn_pause.setImageDrawable(getDrawable(R.drawable.play_icon));
                        } else {
                            sTv_btn_pause.setImageDrawable(getDrawable(R.drawable.pause));
                        }
                    } catch (Exception ex) {
                        LogUtil.d(TAG, ex.getMessage());
                    }
                }
            }
        }
    };


    private final BroadcastReceiver mReceiverBT = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //Indicates the local Bluetooth adapter is off.
                        LogUtil.d(TAG, "Bluetooth STATE_OFF");
                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        //Indicates the local Bluetooth adapter is turning on. However local clients should wait for STATE_ON before attempting to use the adapter.
                        LogUtil.d(TAG, "Bluetooth STATE_TURNING_ON");
                        break;

                    case BluetoothAdapter.STATE_ON:
                        //Indicates the local Bluetooth adapter is on, and ready for use.
                        LogUtil.d(TAG, "Bluetooth STATE_ON");
                        if (!mIsSkipEnabled && mService != null) {
                            mService.connectToAdapter();
                        }
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Indicates the local Bluetooth adapter is turning off. Local clients should immediately attempt graceful disconnection of any remote links.
                        LogUtil.d(TAG, "Bluetooth STATE_TURNING_OFF");
                        break;
                }
            }
        }
    };
}
