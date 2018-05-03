package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
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
import com.j1939.api.enums.ConnectionStates;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import com.mobiliya.fleet.models.DriverScore;
import com.mobiliya.fleet.models.LastTrip;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.models.Vehicle;
import com.mobiliya.fleet.services.GpsLocationReceiver;
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

import static com.mobiliya.fleet.utils.CommonUtil.showToast;

@SuppressWarnings({"ALL", "unused"})
public class DashboardActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "DashboardActivity";
    private RecyclerView mRecyclerView;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecyclerView.Adapter mAdapter;
    private String mProtocol = null;
    private SharePref pref = null;
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
    private TextView mMilesDriven;
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

        pref = SharePref.getInstance(this);
        mIsSkipEnabled = pref.getBooleanItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mIsSkipEnabled) {
            if (!mBluetoothAdapter.isEnabled()) {
                LogUtil.d(TAG, "Bluetooth is not enable, trying to enable..");
                mBluetoothAdapter.enable();
            }
        }
        bindViews();
        mProtocol = pref.getItem(Constants.PREF_ADAPTER_PROTOCOL);
        String mAdapterName = pref.getItem(Constants.PREF_BT_DEVICE_NAME, "");
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
        Vehicle vehicle = pref.getVehicleData();
        mRegistrationNotv.setText(vehicle.getRegistrationNo());
        mFueltv.setText(vehicle.getFuleType());
        mVehicleModeltv.setText(vehicle.getModel());
        mYearOfManufacturtv.setText(vehicle.getYearOfManufacture());
        mVehicleColor.setText(vehicle.getVehicleColor());
    }

    private void reConnectToAdapter() {
        if (mService != null) {
            LogUtil.i(TAG, "reConnectToAdapter button clicked");
            mAdapterConnectionStateTextView.setText(getResources().getString(R.string.status_bluetooth_connecting));
            mAdapterConnectionStateTextView.setVisibility(View.VISIBLE);
            mReconnectBtn.setVisibility(View.GONE);
            mService.connectToAdapter();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsServiceConnected) {
            LogUtil.d(TAG, "onDestroy() service disconnected");
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
        connectToAdapetrService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setAdapterConnectionStatus();
        setScoreWithTripsView(null);
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        try {
            //Receiver to show message on db full
            registerReceiver(
                    mDatabaseMessageReceiver, new IntentFilter(Constants.DATABASEFULL));

            registerReceiver(
                    mParameterStatusReceiver, new IntentFilter(Constants.DASHBOARD_RECEIVER_ACTION_NAME));
            registerReceiver(
                    mParameterReceiver, new IntentFilter(Constants.LOCAL_RECEIVER_ACTION_NAME));
            registerReceiver(
                    mParameterReceiver, new IntentFilter(Constants.LOCAL_RECEIVER_ACTION_NAME));
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
            if (mTrip.status != TripStatus.Stop.getValue()) {
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        showFloatingWindow();
                    }
                }, 100);

            } else {
                if (sPopupWindow != null) {
                    if (sPopupWindow.isShowing())
                        sPopupWindow.dismiss();
                }
            }
        } else {
            if (sPopupWindow != null) {
                if (sPopupWindow.isShowing())
                    sPopupWindow.dismiss();
            }
        }
        getLastTrip();
        setProgressScore();
        getDriverScore();
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
        try {
            unregisterReceiver(
                    mDatabaseMessageReceiver);
            unregisterReceiver(
                    mParameterStatusReceiver);
            unregisterReceiver(
                    mParameterReceiver);
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
        if (requestCode == Constants.DASHBOARD_REQUEST_CODE && resultCode == Constants.SETTINGS_RESULT_CODE) {
            finish();
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
        } catch (WindowManager.BadTokenException e) {
            LogUtil.d(TAG, "Bad token exception");
        }
    }

    Date startDate = null;
    private BroadcastReceiver mDatabaseMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {

            if (startDate == null) {
                startDate = new Date();
                showToast(DashboardActivity.this, "Your offline storage is full,Please go online.");
            } else {

                long diff = new Date().getTime() - startDate.getTime();
                long diffHours = diff / (60 * 60 * 1000) % 24;
                if (diffHours == 5) {
                    showToast(DashboardActivity.this, "Your offline storage is full,Please go online.");
                }
            }
        }
    };

    private void setupBadge() {
        int faultAlertCount = pref.getFaultAlertCount();

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
            String distance = hashMap.get("Distance");

            totalHours = "-1.0".equalsIgnoreCase(totalHours) ? "NA" : totalHours;
            fuelUsed = "-1.0".equalsIgnoreCase(fuelUsed) ? "NA" : fuelUsed;
            distance = "-1.0".equalsIgnoreCase(distance) ? "NA" : distance;
            String speeding = hashMap.get("Speed");

            setTime();
            if (distance != null) {
                setMiles(distance);
            }

            if (spn != null && !spn.equalsIgnoreCase("")) {
                if (!oldSpn.equalsIgnoreCase(spn)) {
                    setupBadge();
                    oldSpn = spn;
                }
            }
        }
    };

    private void setTime() {
        //calculation for trip time
        if (mTrip != null) {
            Date start = DateUtils.getDateFromString(mTrip.startTime);
            String diff = DateUtils.getTimeDifference(start);

            SharePref.getInstance(getBaseContext()).addItem(Constants.TIME_ONGOING, diff);
            if (mTripTime != null) {
                mTripTime.setText(diff);
                NotificationManagerUtil.getInstance().upDateNotification(getBaseContext(), diff);
            }
        }
    }


    public void getLastTrip() {
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
        String URL = Constants.TRIP_SERVICE_BASEURL_PROD;
        String mTenantId = SharePref.getInstance(this).getUser().getTenantId();
        String mUserId = SharePref.getInstance(this).getUser().getId();
        URL = URL + mTenantId + "/" + "driver?userId=" + mUserId;
        LogUtil.d(TAG, "getDriverScore() URL:" + URL);
        try {
            VolleyCommunicationManager.getInstance().SendRequest(URL, Request.Method.GET, "", getApplicationContext(), new VolleyCallback() {
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
        LastTrip lastTrip = pref.getLastTrip();
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

    public void setMiles(String distance) {
        LogUtil.d(TAG, "distance:" + distance);
        //calculation for miles driven
        if (!"NA".equalsIgnoreCase(distance)) {
            float distanceValue = Float.parseFloat(distance);
            if (distanceValue <= 0) {
                LogUtil.d(TAG, "distance value is less then 0");
                return;
            }
        }
        //calculation for miles driven
        float firstmilage = SharePref.getInstance(getApplicationContext()).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
        LogUtil.d(TAG, "firstmilage:" + firstmilage);
        float milesdriven = 0.0f;
        if (firstmilage == 0.0f) {
            if (!"NA".equalsIgnoreCase(distance)) {
                firstmilage = Float.parseFloat(distance);
                SharePref.getInstance(getApplicationContext()).addItem(Constants.TOTAL_MILES_ONGOING, firstmilage);
                LogUtil.d(TAG, "distance: first milage" + firstmilage);
            } else {
                if (mMilesDriven != null) {
                    mMilesDriven.setText("0 miles");
                }
            }
        } else {
            if (!"NA".equalsIgnoreCase(distance)) {
                milesdriven = Float.parseFloat(distance) - firstmilage;
                if (milesdriven <= 0) {
                    LogUtil.d(TAG, "Miles driven less then 0");
                    if (mMilesDriven != null) {
                        mMilesDriven.setText("0 miles");
                    }
                    return;
                }

                SharePref.getInstance(getApplicationContext()).addItem(Constants.MILES_ONGOING, milesdriven);
                if (mMilesDriven != null) {
                    int dist = (int) Math.round(milesdriven);
                    mMilesDriven.setText("" + dist + " Miles");
                    LogUtil.d(TAG, "distance: miles driver" + milesdriven);
                }
            } else {
                float miles = SharePref.getInstance(getApplicationContext()).getItem(Constants.MILES_ONGOING, 0.0f);
                LogUtil.d(TAG, "Distance travel :" + miles);
                if (mMilesDriven != null) {
                    mMilesDriven.setText(miles + " miles");
                }
            }
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
                        showToast(DashboardActivity.this, getString(R.string.trip_is_already_paused));
                    } else {
                        TripManagementUtils.pauseTrip(DashboardActivity.this);
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
                TripManagementUtils.stopTrip(DashboardActivity.this);
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
        }
    };

}
