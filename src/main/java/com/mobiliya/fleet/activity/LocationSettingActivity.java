package com.mobiliya.fleet.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
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
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.location.LocationInfo;
import com.mobiliya.fleet.services.GPSTracker;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.LogUtil;

import java.io.IOException;
import java.util.ArrayList;
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
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

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

    @Override
    protected void onResume() {
        super.onResume();
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
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
    private void setLocation(String text) {
        mLocationView_ll.setVisibility(View.VISIBLE);
        mLocationTitle_tv.setText("Location");
        mNext_btn.setText(text);
    }

    private void startNextActivity() {
        LogUtil.d(TAG, "startNextActivity");
        GPSTracker.getInstance(this);
        Intent intent;
        intent = new Intent(LocationSettingActivity.this, BluetoothConnectionActivity.class);
        startActivity(intent);
        finish();
    }

    private ProgressDialog dialog = null;

    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(LocationSettingActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    dialog = new ProgressDialog(this);
                    dialog.setIndeterminate(true);
                    dialog.setMessage("Fetching location, please wait....");
                    dialog.setCancelable(false);
                    dialog.show();
                    mylocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(1000);
                    locationRequest.setFastestInterval(1000);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi
                            .requestLocationUpdates(googleApiClient, locationRequest, this);
                }
            }
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
        try {
            if (TextUtils.isEmpty(getAddress(mylocation))) {
                mAddresses = mGeocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                final String address = mAddresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
                LogUtil.d(TAG, "location address :" + address);
                mExactLocation_tv.setText(address);
                dialog.dismiss();
                setLocation("Next");
            } else {
                String address = getAddress(mylocation);
                mExactLocation_tv.setText(address);
                dialog.dismiss();
                setLocation("Next");
                LogUtil.d(TAG, "got the location : " + address);
            }
        } catch (Exception e) {
            dialog.dismiss();
            LogUtil.d(TAG, "Exception occurred");
        }
    }

    private String getAddress(final Location currentBestLocation) {
        LogUtil.d(TAG, "Into getAddress() method");
        String address = "";
        LocationInfo lInfo = new LocationInfo();
        if (currentBestLocation != null) {
            Geocoder geocoder;
            List<Address> addresses = new ArrayList<>();
            geocoder = new Geocoder(mContext.getApplicationContext(),
                    Locale.getDefault());
            try {
                addresses = geocoder.getFromLocation(
                        currentBestLocation.getLatitude(),
                        currentBestLocation.getLongitude(), 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address returnedAddress = addresses.get(0);
                    for (int i = 0; i < addresses.get(0)
                            .getMaxAddressLineIndex(); i++) {
                        address += returnedAddress.getAddressLine(i) + ", ";
                    }
                    // Remove the trailing characters.
                    if (address.length() > 0) {
                        address = address.substring(0, address.length() - 2);
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return address;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       /* LocationServices.FusedLocationApi
                .removeLocationUpdates(googleApiClient, LocationSettingActivity.this);*/
    }
}
