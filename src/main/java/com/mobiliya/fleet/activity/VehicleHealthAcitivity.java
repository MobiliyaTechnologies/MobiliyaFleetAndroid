package com.mobiliya.fleet.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.github.pires.obd.commands.ObdCommand;
import com.github.pires.obd.enums.AvailableCommandNames;
import com.google.gson.Gson;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.adapters.VehicleDataAdapter;
import com.mobiliya.fleet.config.ObdConfig;
import com.mobiliya.fleet.io.AbstractGatewayService;
import com.mobiliya.fleet.io.J1939DongleService;
import com.mobiliya.fleet.io.LogCSVWriter;
import com.mobiliya.fleet.io.MockObdGatewayService;
import com.mobiliya.fleet.io.ObdCommandJob;
import com.mobiliya.fleet.io.ObdGatewayService;
import com.mobiliya.fleet.io.ObdProgressListener;
import com.mobiliya.fleet.net.ObdReading;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"ALL", "unused"})
public class VehicleHealthAcitivity extends BaseActivity implements ObdProgressListener, LocationListener {

    private static final String TAG = "VehicleHealthAcitivity";
    private static final int NO_BLUETOOTH_ID = 0;
    private static final int BLUETOOTH_DISABLED = 1;
    private static final int START_LIVE_DATA = 2;
    private static final int STOP_LIVE_DATA = 3;
    private static final int SETTINGS = 4;
    private static final int GET_DTC = 5;
    private static final int TABLE_ROW_MARGIN = 7;
    private static final int NO_ORIENTATION_SENSOR = 8;
    private static final int NO_GPS_SUPPORT = 9;
    private static final int TRIPS_LIST = 10;
    private static final int SAVE_TRIP_NOT_AVAILABLE = 11;
    private static final int REQUEST_ENABLE_BT = 1234;
    private static boolean sBluetoothDefaultIsEnable = false;
    private String mProtocol;
    private final Map<String, String> mCommandResult = new HashMap<String, String>();
    private boolean mGpsIsStarted = false;
    private LogCSVWriter myCSVWriter;
    private Location mLastLocation;
    private SharePref mPref;

    private String mAdapterName;
    private boolean mIsAdapterConnected = false;
    private AbstractGatewayService mAdapterService;


    private boolean mServiceBound;
    private AbstractGatewayService mService;
    private Intent mServiceIntent;

    private RecyclerView mRecyclerViewParams;
    private VehicleDataAdapter mVehicleDataAdapter;
    private HashMap<String, String> mParamsList = new HashMap<>();
    private LinearLayout mBackButton_ll;

    private Gson mGson = new Gson();
    private LinearLayout mErrorLayout;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.acitivity_vehicle_health);
        mErrorLayout = (LinearLayout) findViewById(R.id.error_layout);
        mRecyclerViewParams = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerViewParams.setLayoutManager(new LinearLayoutManager(this));

        mBackButton_ll = (LinearLayout) findViewById(R.id.back_button);
        mBackButton_ll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrectActivity();
            }
        });

        Intent intent = getIntent();
        mProtocol = intent.getStringExtra(Constants.PROTOCOL);
        mPref = SharePref.getInstance(this);
        LogUtil.d(TAG, "Protocol called: " + mProtocol);

        mVehicleDataAdapter = new VehicleDataAdapter(this, mParamsList);
        mRecyclerViewParams.setAdapter(mVehicleDataAdapter);

        final BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            sBluetoothDefaultIsEnable = btAdapter.isEnabled();
        }
        connectToAdapetrService();
    }

    private void finishCurrectActivity() {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }


    private void connectToAdapetrService() {
        LogUtil.i(TAG, "connectToAdapetrService");
        if (mProtocol.equals(Constants.J1939)) {
            mServiceIntent = new Intent(this, J1939DongleService.class);
        } else {
            mServiceIntent = new Intent(this, ObdGatewayService.class);
        }
        bindService(mServiceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    private final Runnable mQueueCommands = new Runnable() {
        public void run() {
            if (mService != null) {
                LogUtil.i(TAG, "Service is not null");
                if (mService.isRunning()) {
                    LogUtil.i(TAG, "Service is running");
                }
                if (mService.queueEmpty()) {
                    LogUtil.i(TAG, "Service queue is empty");
                }
            }
            if (mService != null && mService.isRunning() && mService.queueEmpty()) {
                queueCommands();

                /*double lat = 0;
                double lon = 0;
                double alt = 0;
                final int posLen = 7;
                if (mGpsIsStarted && mLastLocation != null) {
                    lat = mLastLocation.getLatitude();
                    lon = mLastLocation.getLongitude();
                    alt = mLastLocation.getAltitude();

                    StringBuilder sb = new StringBuilder();
                    sb.append("Lat: ");
                    sb.append(String.valueOf(mLastLocation.getLatitude()).substring(0, posLen));
                    sb.append(" Lon: ");
                    sb.append(String.valueOf(mLastLocation.getLongitude()).substring(0, posLen));
                    sb.append(" Alt: ");
                    sb.append(String.valueOf(mLastLocation.getAltitude()));

                }
                if (mPref.getBooleanItem(ConfigActivity.UPLOAD_DATA_KEY, false)) {
                    // Upload the current reading by http
                    final String vin = mPref.getItem(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                    Map<String, String> temp = new HashMap<String, String>();
                    temp.putAll(mCommandResult);
                    ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);


                } else if (mPref.getBooleanItem(ConfigActivity.ENABLE_FULL_LOGGING_KEY, false)) {
                    // Write the current reading to CSV
                    final String vin = mPref.getItem(ConfigActivity.VEHICLE_ID_KEY, "UNDEFINED_VIN");
                    Map<String, String> temp = new HashMap<String, String>();
                    temp.putAll(mCommandResult);
                    ObdReading reading = new ObdReading(lat, lon, alt, System.currentTimeMillis(), vin, temp);
                    myCSVWriter.writeLineCSV(reading);
                }*/
                mCommandResult.clear();
            } else {
                LogUtil.d(TAG, "mQueueCommands Service:null or service is not running or service queue is not empty");
            }
            if (mServiceBound) {
                // run again in period defined in preferences
                new Handler().postDelayed(mQueueCommands, ConfigActivity.getObdUpdatePeriod());
            }

        }
    };

    private boolean preRequisites = true;
    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            mServiceBound = true;
            LogUtil.i(TAG, "onServiceConnected");
            mService = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            if (mService.isAdapterConnected()) {
                mService.getVehicleData();
                LogUtil.d(TAG, "Starting live data");
                if (mProtocol.equals(Constants.OBD)) {
                    // start command execution
                    LogUtil.i(TAG, "OBD service found.....Start executing mQueueCommands");
                    new Handler().post(mQueueCommands);
                }
            }
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
            mServiceBound = false;
        }
    };

    private static String LookUpCommand(String txt) {
        for (AvailableCommandNames item : AvailableCommandNames.values()) {
            if (item.getValue().equals(txt)) return item.name();
        }
        return txt;
    }

    //recieved j1939 data
    @Override
    public void stateUpdateByAdapter(Map<String, String> commandResult) {
        for (Map.Entry<String, String> e : commandResult.entrySet()) {
            String key = e.getKey();
            String value = e.getValue();

            addTableRow(key, key, value);
        }
    }

    //recieved OBD 2 data
    public void stateUpdate(final ObdCommandJob job) {
        final String cmdName = job.getCommand().getName();
        String cmdResult = "";
        final String cmdID = LookUpCommand(cmdName);
        if (job.getState().equals(ObdCommandJob.ObdCommandJobState.EXECUTION_ERROR)) {
            cmdResult = job.getCommand().getResult();
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.BROKEN_PIPE)) {
            if (mServiceBound)
                stopLiveData();
        } else if (job.getState().equals(ObdCommandJob.ObdCommandJobState.NOT_SUPPORTED)) {
            cmdResult = getString(R.string.status_obd_no_support);
        } else {
            cmdResult = job.getCommand().getFormattedResult();
        }
        addTableRow(cmdID, cmdName, cmdResult);
        mCommandResult.put(cmdID, cmdResult);
    }


    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy called");
        unregisterReceiver(
                mParameterReceiver);
        mServiceBound = false;
        if (mService != null) {
            unbindService(mServiceConn);
        }
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

    private void updateConfig() {
        startActivity(new Intent(this, ConfigActivity.class));
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case START_LIVE_DATA:
                startLiveData();
                return true;
            case STOP_LIVE_DATA:
                stopLiveData();
                return true;
            case SETTINGS:
                updateConfig();
                return true;
        }
        return false;
    }


    private void startLiveData() {
        LogUtil.d(TAG, "Starting live data..");

        doBindService();
        if (mProtocol.equals(Constants.OBD)) {
            // start command execution
            new Handler().post(mQueueCommands);
        }
    }

    private void stopLiveData() {
        LogUtil.d(TAG, "Stopping live data..");
        doUnbindService();
        if (myCSVWriter != null) {
            myCSVWriter.closeLogCSVWriter();
        }
    }


    private void addTableRow(String id, String key, String val) {

        TableRow tr = new TableRow(this);
        MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(TABLE_ROW_MARGIN, TABLE_ROW_MARGIN, TABLE_ROW_MARGIN,
                TABLE_ROW_MARGIN);
        tr.setLayoutParams(params);

        TextView name = new TextView(this);
        name.setGravity(Gravity.RIGHT);
        name.setText(key + ": ");
        TextView value = new TextView(this);
        value.setGravity(Gravity.LEFT);
        value.setText(val);
        value.setTag(id);
        tr.addView(name);
        tr.addView(value);

        mParamsList.put(key, val);

        LogUtil.d(TAG, "key:" + key + " Value:" + val);
        mVehicleDataAdapter.addListItem(mParamsList);
    }

    /**
     *
     */
    private void queueCommands() {
        LogUtil.d(TAG, "queueCommands");
        if (mServiceBound) {
            for (ObdCommand Command : ObdConfig.getCommands()) {
                mService.queueJob(new ObdCommandJob(Command));
            }
        } else {
            LogUtil.d(TAG, "service is not bound");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    private void doBindService() {
        if (!mServiceBound) {
            if (mProtocol.equals(Constants.OBD)) {
                LogUtil.d(TAG, "Binding OBD service..");
                if (preRequisites) {
                    Intent serviceIntent = new Intent(this, ObdGatewayService.class);
                    bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
                } else {
                    Intent serviceIntent = new Intent(this, MockObdGatewayService.class);
                    bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
                }
            } else {
                Intent serviceIntent = new Intent(this, J1939DongleService.class);
                bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
            }

        } else if (mServiceBound && !mService.isAdapterConnected()) {

            if (!mProtocol.equalsIgnoreCase(Constants.OBD)) {
                mService.connectToAdapter();
            }
        }
    }


    private void doUnbindService() {
        if (mServiceBound) {
            if (mService.isRunning()) {
                mService.stopService();
            }
            LogUtil.d(TAG, "Unbinding OBD service..");
            unbindService(mServiceConn);
            mServiceBound = false;
        }
    }

    public void onLocationChanged(Location location) {
        mLastLocation = location;
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    @SuppressWarnings("unused")
    public class Params {
        String units;

        public Params(String u, String signalType) {
            this.units = u;
            this.signalType = signalType;
        }

        public String getSignalType() {
            return signalType;
        }

        public void setSignalType(String signalType) {
            this.signalType = signalType;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        String signalType;
    }

    private BroadcastReceiver mParameterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d(TAG, "onReceive() parameter received");
            HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra(Constants.LOCAL_RECEIVER_NAME);
            mParamsList.putAll(hashMap);
            assignValues(mParamsList);
        }
    };

    //assign values to adapter
    public void assignValues(Map<String, String> commandResult) {
        LogUtil.d(TAG, "assignValues");
        if (commandResult.size() != 0) {
            mErrorLayout.setVisibility(View.GONE);
            mRecyclerViewParams.setVisibility(View.VISIBLE);

            mVehicleDataAdapter.addListItem(commandResult);
        } else {
            mErrorLayout.setVisibility(View.VISIBLE);
            mRecyclerViewParams.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        finishCurrectActivity();
    }
}
