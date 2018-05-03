package com.mobiliya.fleet.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;

import com.mobiliya.fleet.utils.LogUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * This class contains all the location retrieval related functionality.
 */
@SuppressWarnings({"ALL", "unused"})
public class LocationTracker {

    private static final String TAG = LocationTracker.class.getSimpleName();

    private Location mCurrentBestLocation = null;

    private LocationManager mLocationManager = null;

    private LocationListener mLocationListener;

    private Context mContext = null;

    private Handler mHandler = null;

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private static final int TIMER_DELAY = 20 * 1000;

    /**
     * Maximum retry for location on failed case.
     */
    private static final int MAX_RETRY_FOR_LOCATION = 1;
    /**
     * Counter to do locate action upto maximum try on failed.
     */
    private int mRetryCount = MAX_RETRY_FOR_LOCATION;

    @SuppressLint("StaticFieldLeak")
    private static LocationTracker instance = null;

    private LocationTracker(Context context) {
        mContext = context;
        mLocationManager = (LocationManager) mContext.getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new MyLocationListener();

    }

    public static LocationTracker getInstance(Context context) {
        if (instance == null) {
            instance = new LocationTracker(context);
        }
        return instance;
    }

    private Handler nRetryHandler = new Handler();

    private Runnable runnable = new Runnable() {
        public void run() {
            try {
                LogUtil.d(TAG, "Timer fired with count : " + mRetryCount);
                if (mRetryCount > 0) {
                    nRetryHandler.postDelayed(runnable, TIMER_DELAY);
                    getLastBestLocation();
                } else
                    sendLocationInfo(new LocationInfo());

                mRetryCount--;

                // Remove the listener you previously added
                mLocationManager.removeUpdates(mLocationListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void getLocation(final Handler handler) {
        LogUtil.d(TAG, "Into getLocation() method");

        // Re initialise counter for retry location
        mRetryCount = MAX_RETRY_FOR_LOCATION;

        mHandler = handler;
        getLastBestLocation();

        nRetryHandler.postDelayed(runnable, TIMER_DELAY);
    }

    public void getLastBestLocation() {

        LogUtil.d(TAG, "Into getLastBestLocation() method");

        try {
            boolean gps_enabled = mLocationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean net_gps_enabled = mLocationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (net_gps_enabled) {
                LogUtil.d(TAG, "NETWORK GPS Enabled");
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                mLocationManager.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER, mLocationListener,
                        null);
            } else if (gps_enabled) {
                LogUtil.d(TAG, "GPS Enabled");
                mLocationManager.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER, mLocationListener, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private LocationInfo getLocationInfo(final Location currentBestLocation) {
        LogUtil.d(TAG, "Into getLocationInfo() method");

        LocationInfo lInfo = new LocationInfo();
        if (currentBestLocation != null) {

            Geocoder geocoder;
            List<Address> addresses = new ArrayList<>();
            String address = "";
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

            lInfo.setLatitude(currentBestLocation.getLatitude());
            lInfo.setLongitude(currentBestLocation.getLongitude());
            lInfo.setAccuracy(currentBestLocation.getAccuracy());
            lInfo.setAltitude(currentBestLocation.getAltitude());
            lInfo.setUtcTime(currentBestLocation.getTime());
            lInfo.setAddress(address);

            printLocation(lInfo);
        }

        return lInfo;
    }

    private void printLocation(LocationInfo location) {
        if (location != null) {
            String longitude = "\nlongitude: " + location.getLongitude();
            String latitude = "\nlatitude: " + location.getLatitude();
            String altitude = "\nAltitude: " + location.getAltitude();
            String accuracy = "\nAccuracy: " + location.getAccuracy();
            String time = "\nUTC Time: " + location.getUtcTime();
            String address = "\nAddress: " + location.getAddress();

            String info = "Current Location : \n" + latitude + longitude
                    + altitude + accuracy + time + address;
            LogUtil.d(TAG, info);
        }
    }

    private void sendLocationInfo(final LocationInfo location) {

        Message message = mHandler.obtainMessage();
        message.what = 0;
        message.obj = location;
        mHandler.dispatchMessage(message);

        nRetryHandler.removeCallbacks(runnable);
    }

    @SuppressWarnings("unused")
    class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            LogUtil.d(TAG, "OnlocationChanged with location : " + location);
            makeUseOfNewLocation(location);

            if (mCurrentBestLocation == null) {
                mCurrentBestLocation = location;
            }
            sendLocationInfo(getLocationInfo(mCurrentBestLocation));

            // Remove the listener you previously added
            mLocationManager.removeUpdates(this);
        }

        @Override
        public void onProviderDisabled(String arg0) {
        }

        @Override
        public void onProviderEnabled(String arg0) {
        }

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        }
    }

    /**
     * This method modify the last know good location according to the
     * arguments.
     *
     * @param location the possible new location
     */
    void makeUseOfNewLocation(Location location) {
        if (isBetterLocation(location, mCurrentBestLocation)) {
            mCurrentBestLocation = location;
        }
    }

    /**
     * Determines whether one Location reading is better than the current
     * Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to
     *                            compare the new one
     */
    public boolean isBetterLocation(Location location,
                                    Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location.
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location,
        // use the new location because the user has likely moved.
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be
            // worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation
                .getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and
        // accuracy.
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate
                && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * This API will return the current GPS Status.
     *
     * @return true if connected to GPS/Wifi, false otherwise.
     */
    public boolean getGpsStatus() {
        LogUtil.d(TAG, "Into getGpsStatus() method");

        boolean gpsStatus = true;

        boolean gps_enabled = mLocationManager
                .isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net_gps_enabled = mLocationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!net_gps_enabled && !gps_enabled) {
            gpsStatus = false;
        }

        return gpsStatus;
    }
}
