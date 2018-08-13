package com.mobiliya.fleet.location;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import java.util.List;
import java.util.Locale;

import static com.mobiliya.fleet.utils.Constants.GPS_DISTANCE;


@SuppressWarnings({"ALL", "unused"})
public class GPSTracker extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = "GPSTracker";
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    private Context mContext;
    private GoogleApiClient googleApiClient;
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;
    @SuppressLint("StaticFieldLeak")
    private static GPSTracker mGPSTrackerInstance;
    private Location location;
    private double lat_new, lon_new, lat_old = 0, lon_old = 0;
    private Long time_old = System.currentTimeMillis();
    private Long time_new = System.currentTimeMillis();
    private float speed = 0;
    private float distance = 0;
    private double latitude = 0.0;
    private double longitude = 0.0;
    private Location prevLocation = null;

    private GPSTracker(Context context) {
        this.mContext = context;
        setUpGClient();
        getLocation();
    }

    public GPSTracker() {
    }

    public static synchronized GPSTracker getInstance(Context context) {
        if (mGPSTrackerInstance == null) {
            mGPSTrackerInstance = new GPSTracker(context);
        }
        return mGPSTrackerInstance;
    }

    public static synchronized void resetGPSInstance() {
        LogUtil.d(TAG, "resetGPSInstance");
        mGPSTrackerInstance = null;
    }

    public void getLocation() {
        LogUtil.d(TAG, "getLocation called");
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                this.location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                if (this.location != null) {
                    LogUtil.d(TAG, "getLastLocation: " + this.location.getLatitude());
//                    LogUtil.d(TAG, "getLastLocation: Accuracy: " + this.location.getAccuracy());
                } else {
                    retrieveLastLocation();
                }
                updateGPSCoordinates();
                LocationRequest locationRequest = new LocationRequest();
                locationRequest.setInterval(5000);
                locationRequest.setFastestInterval(5000);
                locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                        .addLocationRequest(locationRequest);
                builder.setAlwaysShow(true);
                LocationServices.FusedLocationApi
                        .requestLocationUpdates(googleApiClient, locationRequest, this);
            } else {
                LogUtil.d(TAG, "googleApiClient.isConnected(): FALSE");
            }
        } else {
            setUpGClient();
            LogUtil.d(TAG, "getLocation googleApiClient is NULL");
        }
    }


    private synchronized void setUpGClient() {
        googleApiClient = new GoogleApiClient.Builder(this.mContext)
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
        getLocation();
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
    public void onLocationChanged(Location CurrectLocation) {
        LogUtil.d(TAG, "onLocationChanged lat:" + CurrectLocation.getLatitude() + " long:" +
                CurrectLocation.getLongitude() + " Accuracy " + CurrectLocation.getAccuracy());
        double distanceLocal = 0.0;
        double speedLocal = 0.0f;
        try {
            if (CurrectLocation != null && CurrectLocation.getLatitude() != 0.0 && CurrectLocation.getLongitude() != 0.0) {
                if (this.location == null) {
                    LogUtil.d(TAG, "Location object is null, so set now");
                    this.location = CurrectLocation;
                }
                if (CurrectLocation.getAccuracy() > 95) {
                    LogUtil.d(TAG, "we are ignoring GPS location since accuaracy is greater then 100");
                    return;
                }
                if (this.prevLocation == null) {
                    this.prevLocation = new Location("");
                    this.prevLocation.set(CurrectLocation);
                    this.location.set(CurrectLocation);
                    updateGPSCoordinates();
                } else if (this.location != null && prevLocation != null) {
                    //check for 100 meters distance, accept only when its greater then 100 meters
                    if (prevLocation.distanceTo(CurrectLocation) > 100) {
                        distanceLocal = calculateDistance(CurrectLocation.getLatitude(), CurrectLocation.getLongitude(),
                                prevLocation.getLatitude(), prevLocation.getLongitude());
                        //check for distance with prev distance it should be less then 2 miles, else we are ignoring
                        if (distanceLocal > 2) {
                            LogUtil.d(TAG, "Distance is greater then 2 miles so, return distance:" + distanceLocal);
                            return;
                        }
                        prevLocation.set(CurrectLocation);
                        this.location.set(CurrectLocation);
                        updateGPSCoordinates();
                    } else {
                        LogUtil.d(TAG, "return since distance is under 100 meters");
                        return;
                    }
                }
                try {
                    LogUtil.d(TAG, "Distance last and current Miles:" + this.distance);
                    time_new = System.currentTimeMillis();
                    long time = time_new - time_old;
                    double time_s = time / 1000.0;
                    double time_h = time_s / 3600;
                    speedLocal = Math.round((distanceLocal / time_h) * 100.0) / 100.0;

                    if (speedLocal > 130) {
                        LogUtil.d(TAG, "Speed is greater then 130 miles so do not consider speed:" + speedLocal);
                    } else {
                        this.speed = (float) speedLocal;
                    }
                    double dist = Math.round(distanceLocal * 100.0) / 100.0;
                    if (SharePref.getInstance(this.mContext).getBooleanItem(GPS_DISTANCE, true)) {
                        float ongoingDistance = SharePref.getInstance(this.mContext)
                                .getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                        this.distance = ongoingDistance;
                        this.distance += (float) dist;
                        LogUtil.d(TAG, "Distance last and current Miles after addition:" + this.distance);
                        SharePref.getInstance(this.mContext)
                                .addItem(Constants.TOTAL_MILES_ONGOING, this.distance);
                    } else {
                        this.distance += (float) dist;
                    }
                    LogUtil.d(TAG, "Speed Miles/Hrs:" + this.speed);
                    LogUtil.d(TAG, "Total miles On going trip:" + this.distance);
                    time_old = time_new;
                } catch (Exception e) {
                    LogUtil.d(TAG, "exception while calculation speed or distance");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            LogUtil.d(TAG, "Exception onLocationChanged");
        }
    }

    /**
     * Update GPSTracker latitude and longitude
     */
    public void updateGPSCoordinates() {
        if (location != null && (location.getLatitude() != 0.0 && location.getLatitude() != 0.0)) {
            LogUtil.d(TAG, "updateGPSCoordinates");
            if (location != null && prevLocation != null) {
                double distanceLocal = calculateDistance(location.getLatitude(), location.getLongitude(),
                        prevLocation.getLatitude(), prevLocation.getLongitude());
                if (distanceLocal > 2) {
                    LogUtil.d(TAG, "updateGPSCoordinates Distance is greater then 2 miles so, return distance:" + distanceLocal);
                    return;
                }
            }
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
            SharePref.getInstance(mContext).addItem(Constants.LATITUDE, String.valueOf(this.latitude));
            SharePref.getInstance(mContext).addItem(Constants.LONGITUDE, String.valueOf(this.longitude));
            LatLong locations = new LatLong(String.valueOf(latitude), String.valueOf(this.longitude));
            String address = getAddressFromLatLong(this, locations);
            if(!TextUtils.isEmpty(address)) {
                SharePref.getInstance(mContext).addItem(Constants.LAST_ADDRESS, address);
            }else{
                LogUtil.d(TAG,"address is null");
            }
        }
    }

    private void retrieveLastLocation() {
        try {
            LogUtil.d(TAG, "retrieveLastLocation");
            Double lat = Double.parseDouble(SharePref.getInstance(mContext).getItem(Constants.LATITUDE));
            Double lng = Double.parseDouble(SharePref.getInstance(mContext).getItem(Constants.LONGITUDE));
            this.latitude = lat;
            this.longitude = lng;
            LogUtil.d(TAG, "Last Lat:" + lat + " Long:" + lng);
        } catch (Exception e) {
            LogUtil.d(TAG, "Parsing error for last lat long");
            e.printStackTrace();
        }
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        Location oldLocation = new Location("oldLocation");
        Location newLocation = new Location("newLocation");
        oldLocation.setLatitude(lat2);
        oldLocation.setLongitude(lon2);
        newLocation.setLatitude(lat1);
        newLocation.setLongitude(lon1);
        float distance = oldLocation.distanceTo(newLocation);//  in meters
        distance = distance / 1000; // in km
        double miles = distance * 0.621; //in miles
        return miles;
    }


    /**
     * Get list of address by latitude and longitude
     *
     * @return null or List<Address>
     */

    public String getAddressFromLatLong(Context context, LatLong latLong) {
        String latlongString = "";
        if (latLong != null) {
            double latitude = Double.parseDouble(latLong.latitude);
            double longitude = Double.parseDouble(latLong.longitude);
            latlongString = "" + latitude + ", " + longitude;
            try {
                if(CommonUtil.isNetworkConnected(context)){
                    LogUtil.d(TAG,"Geocoder network is connected");
                }else {
                    LogUtil.d(TAG,"Geocoder network is not connected");
                }
                Geocoder mGeocoder = new Geocoder(context, Locale.getDefault());
                List<Address> mAddresses = mGeocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
                String addressLine = mAddresses.get(0).getAddressLine(0);
                LogUtil.d(TAG, "Geocoder address:"+addressLine);
                return addressLine;
            } catch (Exception e) {
                e.getMessage();
                LogUtil.d(TAG, "Impossible to connect to Geocoder");
            }
        }
        return latlongString;
    }

    /**
     * GPSTracker latitude getter and setter
     *
     * @return latitude
     */
    public double getLatitude() {
        /*if (location != null) {
            latitude = round(location.getLatitude(), 4);
        }*/
        return this.latitude;
    }

    /**
     * GPSTracker longitude getter and setter
     *
     * @return
     */
    public double getLongitude() {
       /* if (location != null) {
            longitude = round(location.getLongitude(), 4);

        }*/
        return this.longitude;
    }

    /**
     * GPSTracker isGPSTrackingEnabled getter.
     * Check GPS/wifi is enabled
     */
    public boolean getIsGPSTrackingEnabled() {

        return true;//this.isGPSTrackingEnabled;
    }

    public long getTime() {
        if (location != null) {
            return location.getTime();
        }
        return 0;
    }

    public float getSpeed() {
        if (location != null) {
            return speed;
        }
        return 0;
    }

    public void setSpeed(float s) {
        if (location != null) {
            this.speed = s;
        }
    }

    public float getDistance() {
        if (location != null) {
            if (SharePref.getInstance(this.mContext).getBooleanItem(GPS_DISTANCE, true)) {
                float ongoingDistance = SharePref.getInstance(this.mContext)
                        .getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                this.distance = ongoingDistance;
            }
            return this.distance;
        }
        return 0;
    }

    public float setDistance(float distance) {
        if (location != null) {
            this.distance = distance;
            return this.distance;
        }
        return 0;
    }

    public float getAccuracy() {
        if (location != null) {
            return location.getAccuracy();
        }
        return 0.0f;
    }
}
