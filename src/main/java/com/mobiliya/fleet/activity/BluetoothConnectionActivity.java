package com.mobiliya.fleet.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.pires.obd.commands.protocol.EchoOffCommand;
import com.j1939.api.enums.ConnectionStates;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.adapters.DeviceListAdapter;
import com.mobiliya.fleet.io.AbstractGatewayService;
import com.mobiliya.fleet.io.J1939DongleService;
import com.mobiliya.fleet.io.ObdGatewayService;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Set;


@SuppressWarnings({"ALL", "unused"})
public class BluetoothConnectionActivity extends BaseActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private static final String TAG = "BluetoothConnectionActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothAdapter mBluetoothAdapterBLE;
    private ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    private ArrayList<BluetoothDevice> mBLEDevices = new ArrayList<>();
    private DeviceListAdapter mDeviceListAdapter;
    private ListView mListViewNewDevices;
    private Button mBtnDone;
    private boolean mScanning;
    private Handler mHandler;
    private Boolean mBLEdevice = false;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;
    private TextView mDeviceStatus_tv;
    private Boolean mDevicePaired = false;
    private BluetoothDevice mPairedBTdevice = null;
    private SharePref mPref = null;
    private ProgressBar mProgressBar;
    private ImageView mReScanButton_imv;
    private AbstractGatewayService mService;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    public static final int MAX_ITEM_CLICK_COUNT = 3;
    private TextView mBtnSkip;
    private TextView mConnectDevice_tv;
    private int mItemClickCount = 0;
    private LinearLayout mRetryLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connection);
        mHandler = new Handler();
        mListViewNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();
        mBtnDone = (Button) findViewById(R.id.btn_done);
        mBtnSkip = (TextView) findViewById(R.id.btn_skip);
        mRetryLayout = (LinearLayout) findViewById(R.id.retryImg);
        mConnectDevice_tv = (TextView) findViewById(R.id.connect_device_text);
        mConnectDevice_tv.setOnClickListener(this);
        mBtnSkip.setOnClickListener(this);
        mPref = SharePref.getInstance(this);
        mPref.addItem(Constants.KEY_MOVE_TO_BT_SCREEN, true);
        mProgressBar = (ProgressBar) findViewById(R.id.device_search_progress_bar);
        mReScanButton_imv = (ImageView) findViewById(R.id.rescan_device_button);
        checkBTPermissions();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // For BLE, Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapterBLE = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
        } else {
            unPairDevice();
            btnDiscover();
        }
        mListViewNewDevices.setOnItemClickListener(this);
        mBtnDone.setOnClickListener(this);
        mReScanButton_imv.setOnClickListener(this);
        mPref.addItem(Constants.SEND_IOT_DATA_FORCEFULLY, false);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_done:
                //move to next screen if TRUE
                String doneText = getResources().getText(R.string.done).toString();
                if (mBtnDone.getText().toString().equalsIgnoreCase(doneText)) {
                    if (mDevicePaired) {
                        if (!mBLEdevice) {
                            mPref.addItem(Constants.PREF_ADAPTER_PROTOCOL, Constants.OBD);
                            gotoDahsboard(Constants.OBD);
                        } else {
                            mPref.addItem(Constants.PREF_ADAPTER_PROTOCOL, Constants.J1939);
                            gotoDahsboard(Constants.J1939);
                        }
                    }
                } else {//else retry is click scan again
                    reScanDevices();
                }
                break;
            case R.id.btn_back:
                mBluetoothAdapter.cancelDiscovery();
                finish();
                break;
            case R.id.rescan_device_button:
                reScanDevices();
                break;
            case R.id.btn_skip:
                stopScan();
                mPref.addItem(Constants.PREF_ADAPTER_PROTOCOL, Constants.J1939);
                mPref.addItem(Constants.PREF_BT_DEVICE_NAME, "BlueFire");
                mPref.addItem(Constants.SEND_IOT_DATA_FORCEFULLY, true);
                gotoDahsboard(Constants.J1939);
                break;
        }
    }

    private void stopScan() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (mBluetoothAdapter != null && mLeScanCallback != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private void reScanDevices() {
        setmRetryLayout(false);
        mBTDevices.clear();
        mBLEDevices.clear();
        if (mDeviceListAdapter != null) {
            mDeviceListAdapter.notifyDataSetChanged();
        }
        btnDiscover();
    }

    private void setmRetryLayout(boolean flag) {

        if (mBTDevices.size() > 0) {
            //Found device in the mFaultslist
            LogUtil.d(TAG, "Found device in the list");
        } else {
            if (flag) {
                mListViewNewDevices.setVisibility(View.GONE);
                mRetryLayout.setVisibility(View.VISIBLE);
                mBtnDone.setText(getResources().getText(R.string.retry_scan));
            } else {
                mListViewNewDevices.setVisibility(View.VISIBLE);
                mRetryLayout.setVisibility(View.GONE);
                mBtnDone.setText(getResources().getText(R.string.done));
            }
        }
    }

    private final BroadcastReceiver mPairingRequestReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_PAIRING_REQUEST)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    int pin = intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234);
                    //the pin in case you need to accept for an specific pin
                    LogUtil.d(TAG, "Start Auto Pairing. PIN = " + intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 1234));
                    byte[] pinBytes;
                    pinBytes = ("" + pin).getBytes("UTF-8");
                    device.setPin(pinBytes);
                } catch (Exception e) {
                    LogUtil.e(TAG, "Error occurs when trying to auto pair");
                    e.printStackTrace();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        //Broadcasts when bond state changes (ie:pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiverPairingState, filter);
        IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiverListingDevice, discoverDevicesIntent);

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LogUtil.d(TAG, "onDestroy: called.");
        /*if (mService != null && mDevicePaired) {
            LogUtil.d(TAG, "onDestroy() service disconnected");
            unbindService(serviceConn);
        }*/
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.cancelDiscovery();
        }
        if (mScanning) {
            scanLeDevice(false);
        }
        unregisterReceiver(mBroadcastReceiverPairingState);
        unregisterReceiver(mBroadcastReceiverListingDevice);
        super.onDestroy();
    }

    private void btnDiscover() {
        LogUtil.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            LogUtil.d(TAG, "Bluetooth is not enable, trying to enable..");
            mBluetoothAdapter.enable();
        }
        mPairedBTdevice = null;

        //check if BT scan is in progress
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
            LogUtil.d(TAG, "btnDiscover: Canceling discovery.");
            startBTDiscovery();
        } else {
            startBTDiscovery();
        }
        //check if BLE scan is in progress
        if (mScanning) {
            scanLeDevice(false);
            LogUtil.d(TAG, "btnDiscover: Canceling BLE discovery.");
            scanLeDevice(true);
        } else {
            scanLeDevice(true);
        }
    }

    /**
     * This method is called when user need to search for bluetooth devices
     */
    private void startBTDiscovery() {
        mBluetoothAdapter.startDiscovery();
        setBTScanRescanButtonVisiblility(false);
        setBTScanProgressBarVisibility(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

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
                        btnDiscover();
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Indicates the local Bluetooth adapter is turning off. Local clients should immediately attempt graceful disconnection of any remote links.
                        LogUtil.d(TAG, "Bluetooth STATE_TURNING_OFF");
                        break;
                }
            }
        }
    };


    /**
     * Broadcast Receiver for listing devices that are not yet paired
     * -Executed by btnDiscover() method.
     */
    private final BroadcastReceiver mBroadcastReceiverListingDevice = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            LogUtil.d(TAG, "onReceive: ACTION FOUND.");

            try {
                if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (!TextUtils.isEmpty(device.getName()) && !checkDuplicate(device)) {
                        BluetoothClass bluetoothClass = device.getBluetoothClass();
                        LogUtil.d(TAG, "Device type: " + bluetoothClass.getDeviceClass());
                        if (!isSkipDevices(bluetoothClass)) {
                            mBTDevices.add(device);
                        }
                    } else {
                        LogUtil.d(TAG, "onReceive: Device Name is null, so returning");
                        return;
                    }
                    LogUtil.d(TAG, "onReceive: " + device.getName() + ": " + device.getAddress());
                    mDeviceListAdapter = new DeviceListAdapter(getBaseContext(), mBTDevices);
                    mListViewNewDevices.setAdapter(mDeviceListAdapter);
                }
            }catch (Exception e){
                LogUtil.d(TAG,"error mBroadcastReceiverListingDevice");
                e.printStackTrace();
            }
        }
    };

    /**
     * Check duplicate items in the listview for BT devices
     */
    private boolean checkDuplicate(BluetoothDevice device) {
        for (int i = 0; i < mBTDevices.size(); i++) {
            if (mBTDevices.get(i).getAddress().equalsIgnoreCase(device.getAddress())) {
                LogUtil.d(TAG, "checkDuplicate: duplicate device no need to add in mFaultslist");
                return true;
            }
        }
        return false;
    }

    private boolean checkBLEDeviceInList(BluetoothDevice device) {
        for (int i = 0; i < mBLEDevices.size(); i++) {
            if (mBLEDevices.get(i).getAddress().equalsIgnoreCase(device.getAddress())) {
                LogUtil.d(TAG, "checkBLEDeviceInList: BLE device present into mFaultslist");
                return true;
            }
        }
        return false;
    }

    /**
     * Broadcast Receiver that detects bond state changes (Pairing status changes)
     */
    private final BroadcastReceiver mBroadcastReceiverPairingState = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            try {
                if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                    BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                        mBtnDone.setText(getResources().getText(R.string.done));
                        LogUtil.d(TAG, "BroadcastReceiver: BOND_BONDED. " + mDevice.getName());
                        mListViewNewDevices.setEnabled(true);
                        mBtnDone.setEnabled(true);
                        mDeviceStatus_tv.setTextColor(Color.parseColor("#577cfc"));
                        mDevicePaired = true;
                        mPairedBTdevice = mDevice;
                        mListViewNewDevices.setAdapter(mDeviceListAdapter);
                        mPref.addItem(Constants.PREF_BT_DEVICE_ADDRESS, mDevice.getAddress());
                        mPref.addItem(Constants.PREF_BT_DEVICE_NAME, mDevice.getName());
                        //connectToOBD();
                    }
                    //case2: creating a bone
                    if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                        mListViewNewDevices.setEnabled(false);
                        mBtnDone.setEnabled(false);
                        LogUtil.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                    }
                    //case3: breaking a bond
                    if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                        LogUtil.d(TAG, "BroadcastReceiver: BOND_NONE.");
                        mListViewNewDevices.setEnabled(true);
                        mBtnDone.setEnabled(true);
                        mListViewNewDevices.setAdapter(mDeviceListAdapter);
                        btnDiscover();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();
        scanLeDevice(false);
        setBTScanProgressBarVisibility(false);
        setBTScanRescanButtonVisiblility(true);
        mBtnDone.setText(getResources().getText(R.string.done));

        LogUtil.d(TAG, "onItemClick: You Clicked on a device.");
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();
        mPairedBTdevice = mBTDevices.get(i);


        mPref.addItem(Constants.PREF_BT_DEVICE_ADDRESS, mPairedBTdevice.getAddress());
        mPref.addItem(Constants.PREF_BT_DEVICE_NAME, mPairedBTdevice.getName());
        mDeviceStatus_tv = (TextView) view.findViewById(R.id.tvDeviceAddress);
        mDeviceStatus_tv.setText(getResources().getString(R.string.pairing));
        mDeviceStatus_tv.setTextColor(Color.parseColor("#577cfc"));

        LogUtil.d(TAG, "onItemClick: deviceName = " + deviceName);
        LogUtil.d(TAG, "onItemClick: deviceAddress = " + deviceAddress);

        //Check for the device item click whether BLE or BT device
        for (BluetoothDevice device : mBLEDevices) {
            if (device.getAddress().equalsIgnoreCase(deviceAddress)) {
                LogUtil.d(TAG, "onItemClick: is BLE device = " + true);
                mBLEdevice = true;
            }
        }

        //check if device is already bounded
        boolean flag = false;
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (device.getAddress().equalsIgnoreCase(deviceAddress)) {
                mListViewNewDevices.setEnabled(true);
                mBtnDone.setEnabled(true);
                mDeviceStatus_tv.setTextColor(Color.parseColor("#577cfc"));
                mDevicePaired = true;
                mPairedBTdevice = mBTDevices.get(i);
                mListViewNewDevices.setAdapter(mDeviceListAdapter);
                //connectToOBD();
                flag = true;
                break;
            }
        }
        if (flag) {
            LogUtil.d(TAG, "Device was already bounded, return");
            return;
        }
        //create the bond.
        try {
            LogUtil.d(TAG, "Trying to pair with " + deviceName);
            if (!mBLEdevice) {
                mBTDevices.get(i).createBond();
            } else {
                if (mService != null) {
                    LogUtil.d(TAG, "connecting to adapter");
                    mService.connectToAdapter();
                } else
                    connectTOJ1939();
            }
        } catch (Exception ex) {
            ex.getMessage();
        }
    }

    /**
     * This method is used to unpair devices which are paired.
     */
    private void unPairDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                try {
                    Method m = device.getClass()
                            .getMethod("removeBond", (Class[]) null);
                    m.invoke(device, (Object[]) null);
                    LogUtil.d(TAG, "unPairDevice = ");
                } catch (Exception e) {
                    LogUtil.e(TAG, e.getMessage());
                }
            }
        } else {
            LogUtil.d(TAG, "unPairDevice = 0");
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    setBTScanProgressBarVisibility(false);
                    setBTScanRescanButtonVisiblility(true);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            setBTScanRescanButtonVisiblility(false);
            setBTScanProgressBarVisibility(true);
            LogUtil.d(TAG, "Scannig BLE devices");
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            setBTScanProgressBarVisibility(false);
            setBTScanRescanButtonVisiblility(true);
        }
        invalidateOptionsMenu();
    }

    private void setBTScanProgressBarVisibility(boolean visibile) {
        if (mDevicePaired) {
            LogUtil.d(TAG, "setBTScanProgressBarVisibility Device is already paired return.");
            return;
        }
        if (visibile) {
            setmRetryLayout(false);
            mBtnDone.setText(getResources().getText(R.string.done));
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            mBtnDone.setText(getResources().getText(R.string.retry_scan));
            setmRetryLayout(true);
            mProgressBar.setVisibility(View.GONE);
        }

    }

    private void setBTScanRescanButtonVisiblility(boolean visibile) {
        if (visibile) {
            setmRetryLayout(true);
        } else {
            setmRetryLayout(false);
        }
    }

    // BLE Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!TextUtils.isEmpty(device.getName())) {
                                BluetoothClass bluetoothClass = device.getBluetoothClass();
                                if (!isSkipDevices(bluetoothClass)) {
                                    mBLEDevices.add(device);
                                }
                                if (!checkDuplicate(device)) {
                                    LogUtil.d(TAG, "Device type: " + bluetoothClass.getDeviceClass());
                                    mBTDevices.add(device);
                                }
                            }
                            LogUtil.d(TAG, "on BLE Device Receive: " + device.getName() + ": " + device.getAddress());
                            mDeviceListAdapter = new DeviceListAdapter(getBaseContext(), mBTDevices);
                            mListViewNewDevices.setAdapter(mDeviceListAdapter);
                        }
                    });
                }
            };

    public void gotoDahsboard(String protocol) {
        LogUtil.d(TAG, "Protocol Selected: " + protocol);
        startIOTService(protocol);
        Intent intent = new Intent(BluetoothConnectionActivity.this, DashboardActivity.class);
        mPref.addItem(Constants.PREF_MOVED_TO_DASHBOARD, true);
        startActivity(intent);
        overridePendingTransition(R.anim.enter, R.anim.leave);
        finish();
    }

    private void startIOTService(String mAdapterProtocol) {
        SharePref mPref = SharePref.getInstance(this);
        Intent mServiceIntent;
        try {
            if (mAdapterProtocol.equalsIgnoreCase(Constants.OBD)) {
                LogUtil.d(TAG, "startIOTService: OBD");
                mServiceIntent = new Intent(this, ObdGatewayService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mServiceIntent);
                } else {
                    startService(mServiceIntent);
                }
            } else {
                LogUtil.d(TAG, "startIOTService: J1939");
                mServiceIntent = new Intent(this, J1939DongleService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mServiceIntent);
                } else {
                    startService(mServiceIntent);
                }
            }
        } catch (IllegalStateException e) {
            LogUtil.d(TAG, "IllegalStateException while create service from Bluetooth connection class");
            e.printStackTrace();
        } catch (Exception e) {
            LogUtil.d(TAG, "Exception while create service from Bluetooth connection class");
            e.printStackTrace();
        }
    }

    /*method will try to connect to J1939 device*/
    private void connectTOJ1939() {
        Intent serviceIntent = new Intent(this, J1939DongleService.class);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    /* method to bind to ObdGatewayservice */
    private void connectToOBD() {
        LogUtil.i(TAG, "connectToOBD -> bind to serice");
        Intent serviceIntent = new Intent(this, ObdGatewayService.class);
        bindService(serviceIntent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            LogUtil.i(TAG, "ServiceConnection -> onServiceConnected");
            mService = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            mService.setContext(BluetoothConnectionActivity.this);
            try {
                if (mService.isAdapterConnected()) {
                    LogUtil.i(TAG, "service.isAdapterConnected() -> true");
                    mDeviceStatus_tv.setText(getResources().getString(R.string.paired));
                    mDevicePaired = true;
                } else {
                    LogUtil.i(TAG, "service.isAdapterConnected() -> false");
                    mService.startService();
                    mService.connectToAdapter();
                }
            } catch (IOException ioe) {
                LogUtil.e(TAG, "Failure Starting live data");
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
        }
    };


    /*method will update adapter connection status on UI*/
    @Override
    public void setBTStatus(String status) {
        super.setBTStatus(status);
        LogUtil.i(TAG, "Adapter Status received -> " + status);
        if (status.contains(getResources().getString(R.string.adapter_connected))) {
            mDeviceStatus_tv.setText(getResources().getString(R.string.paired));
            mDevicePaired = true;
            mBtnDone.setText(getResources().getText(R.string.done));
        } else if (status.equals(ConnectionStates.Connecting)) {
            mDeviceStatus_tv.setText(getResources().getString(R.string.pairing));
            mBtnDone.setText(getResources().getText(R.string.done));
        } else if (status.equals(ConnectionStates.DataTimeout) || status.equals(ConnectionStates.Disconnected)
                || status.equals(ConnectionStates.DataError) || status.contains(getResources().getString(R.string.adapter_not_connected))) {
            mDeviceStatus_tv.setText(getResources().getString(R.string.not_connected));
        }
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * <p>
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkBTPermissions() {
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        permissionCheck += this.checkSelfPermission("android.permission-group.CONTACTS");
        permissionCheck += this.checkSelfPermission("android.permission.WRITE_CONTACTS");
        permissionCheck += this.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
        permissionCheck += this.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission_group.CONTACTS, Manifest.permission.BLUETOOTH_PRIVILEGED}, MY_PERMISSIONS_REQUEST_LOCATION); //Any number
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        btnDiscover();
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    LogUtil.d(TAG, "permission denied");
                }
                return;
            }

        }
    }


    private boolean isSkipDevices(BluetoothClass aClass) {
        boolean flag = false;
        switch (aClass.getDeviceClass()) {
            case BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER:
            case BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO:
            case BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE:
            case BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES:
            case BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO:
            case BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER:
            case BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE:
            case BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO:
            case BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX:
            case BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED:
            case BluetoothClass.Device.AUDIO_VIDEO_VCR:
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CAMERA:
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_CONFERENCING:
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_DISPLAY_AND_LOUDSPEAKER:
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_GAMING_TOY:
            case BluetoothClass.Device.AUDIO_VIDEO_VIDEO_MONITOR:
            case BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET:
            case BluetoothClass.Device.COMPUTER_DESKTOP:
            case BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA:
            case BluetoothClass.Device.COMPUTER_LAPTOP:
            case BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA:
            case BluetoothClass.Device.COMPUTER_SERVER:
            case BluetoothClass.Device.COMPUTER_UNCATEGORIZED:
            case BluetoothClass.Device.PHONE_CELLULAR:
            case BluetoothClass.Device.PHONE_CORDLESS:
            case BluetoothClass.Device.PHONE_ISDN:
            case BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY:
            case BluetoothClass.Device.PHONE_SMART:
            case BluetoothClass.Device.PHONE_UNCATEGORIZED:
            case BluetoothClass.Device.COMPUTER_WEARABLE:
                flag = true;
                break;
        }
        return flag;
    }
}
