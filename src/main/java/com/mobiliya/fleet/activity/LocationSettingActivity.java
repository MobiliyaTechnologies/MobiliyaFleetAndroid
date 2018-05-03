package com.mobiliya.fleet.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.location.LocationInfo;
import com.mobiliya.fleet.location.LocationTracker;
import com.mobiliya.fleet.utils.LogUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"ALL", "unused"})
public class LocationSettingActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "LocationSettingActivity";
    private TextView mLocationTitle_tv;
    private TextView mExactLocation_tv;
    private LinearLayout mLocationView_ll;
    private Button mNext_btn;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private Context mContext;
    private Geocoder mGeocoder;
    private List<Address> mAddresses;
    private Location mylocation;
    private GoogleApiClient googleApiClient;
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_setting);
        setUpGClient();
        mLocationTitle_tv = (TextView) findViewById(R.id.locationTitle);
        mExactLocation_tv = (TextView) findViewById(R.id.exactLocation);
        TextView tvBack = (TextView) findViewById(R.id.btn_back);
        mLocationView_ll = (LinearLayout) findViewById(R.id.locationView);
        mNext_btn = (Button) findViewById(R.id.btn_turn_on);
        mNext_btn.setOnClickListener(this);
        tvBack.setOnClickListener(this);
        mContext = this;
        mGeocoder = new Geocoder(this, Locale.getDefault());
    }

    /**
     * This method is required for all devices running API23+
     * Android must programmatically check the permissions for bluetooth. Putting the proper permissions
     * in the manifest is not enough.
     * <p>
     * NOTE: This will only execute on versions > LOLLIPOP because it is not needed otherwise.
     */
    private void checkPermissions() {
        LogUtil.d(TAG, "checkPermissions()");
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        permissionCheck += this.checkSelfPermission("android.permission-group.CONTACTS");
        permissionCheck += this.checkSelfPermission("android.permission.WRITE_CONTACTS");
        permissionCheck += this.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
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
                        LogUtil.d(TAG, "location permission granted");
                        getMyLocation();
                    }

                } else {
                    LogUtil.d(TAG, "permission denied");
                }
            }
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_turn_on:
                checkPermissions();
                if (mNext_btn.getText().toString().equalsIgnoreCase("Next")) {
                    startNextActivity();
                }
                break;
            case R.id.btn_back:
                finish();
        }
    }

    @SuppressLint("SetTextI18n")
    private void setLocation() {
        mLocationView_ll.setVisibility(View.VISIBLE);
        mLocationTitle_tv.setText("Location");
        mNext_btn.setText("Next");
    }

    private void startNextActivity() {
        LogUtil.d(TAG, "startNextActivity");
        Intent intent;
        intent = new Intent(LocationSettingActivity.this, BluetoothConnectionActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * This API returns the current location requested.
     */
    public void getLocation() {
        LogUtil.d(TAG, "Into getLocation() method");
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Fetching location, please wait....");
        dialog.setCancelable(false);
        dialog.show();

        (new Thread() {
            public void run() {
                Looper.prepare();
                try {
                    final Handler mHandler = new Handler() {
                        @Override
                        public void handleMessage(final Message msg) {
                            LogUtil.d(TAG, "Into handleMessage()");
                            if (msg != null && msg.what == 0) {
                                final HashMap<String, Object> hMap = new HashMap<>();

                                final LocationInfo location = (LocationInfo) msg.obj;
                                if (location != null) {
                                    try {
                                        LogUtil.d(TAG, "location object ");
                                        if (TextUtils.isEmpty(location.getAddress())) {
                                            mAddresses = mGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                                            final String address = mAddresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                                            LogUtil.d(TAG, "location address :" + address);
                                            LocationSettingActivity.this.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    mExactLocation_tv.setText(address);
                                                    dialog.dismiss();
                                                }
                                            });
                                        } else {
                                            LocationSettingActivity.this.runOnUiThread(new Runnable() {
                                                public void run() {
                                                    dialog.dismiss();
                                                }
                                            });
                                            LogUtil.d(TAG, "got the location : " + location.getAddress());

                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {
                                        LocationSettingActivity.this.runOnUiThread(new Runnable() {
                                            public void run() {
                                                dialog.dismiss();
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    };

                    // Get the device location.
                    LocationTracker.getInstance(mContext).getLocation(mHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Looper.loop();
            }
        }).start();
    }

    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(LocationSettingActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    mylocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(3000);
                    locationRequest.setFastestInterval(3000);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi
                            .requestLocationUpdates(googleApiClient, locationRequest, this);
                    PendingResult<LocationSettingsResult> result =
                            LocationServices.SettingsApi
                                    .checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                        @Override
                        public void onResult(LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    // All location settings are satisfied.
                                    // You can initialize location requests here.
                                    setLocation();
                                    getLocation();
                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied.
                                    // But could be fixed by showing the user a dialog.
                                    try {
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        // Ask to turn on GPS automatically
                                        status.startResolutionForResult(LocationSettingActivity.this,
                                                REQUEST_CHECK_SETTINGS_GPS);
                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    break;
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS_GPS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        getMyLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                }
                break;
        }
    }

    private synchronized void setUpGClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LogUtil.d(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        LogUtil.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        LogUtil.d(TAG, "onConnectionFailed");
    }

    @Override
    public void onLocationChanged(Location location) {
        mylocation = location;
    }
}
