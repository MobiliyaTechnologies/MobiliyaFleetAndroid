package com.mobiliya.fleet.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.utils.LogUtil;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"ALL", "unused"})
public class GPSTracker extends android.app.Service implements LocationListener {

    //private FusedLocationProviderClient mFusedLocationClient;
    // Get Class Name
    private static String TAG = GPSTracker.class.getName();

    private final Context mContext;

    // flag for GPS Status
    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for GPS Tracking is enabled
    boolean isGPSTrackingEnabled = false;

    Location location;
    private double lat_new, lon_new, lat_old = 0, lon_old = 0;
    private Long time_old = System.currentTimeMillis() / 1000;
    private Long time_new = System.currentTimeMillis() / 1000;
    private double speed = 0;
    private double distance = 0;
    private double latitude;
    private double longitude;

    // How many Geocoder should return our GPSTracker
    int geocoderMaxResults = 1;

    // The minimum distance to change updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 30; // 30 second

    // Declaring a Location Manager
    protected LocationManager locationManager;

    // Store LocationManager.GPS_PROVIDER or LocationManager.NETWORK_PROVIDER information
    private String provider_info;

    @SuppressLint("StaticFieldLeak")
    private static GPSTracker mGPSTrackerInstance;

    public GPSTracker(Context context) {
        this.mContext = context;
        getLocation();
    }

    public GPSTracker() {
        this.mContext = this;
        getLocation();
    }

    public static synchronized GPSTracker getInstance(Context context) {
        if (mGPSTrackerInstance == null) {
            mGPSTrackerInstance = new GPSTracker(context);
        }
        return mGPSTrackerInstance;
    }


    /**
     * Try to get my current location by GPS or Network Provider
     */
    public void getLocation() {

        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            //getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

            //getting network status
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);


            // Try to get location if you GPS DongleDemoService is enabled
            if (isNetworkEnabled) { // Try to get location if you Network DongleDemoService is enabled
                this.isGPSTrackingEnabled = true;

                Log.d(TAG, "Application use Network State to get GPS coordinates");

                /*
                 * This provider determines location based on
                 * availability of cell tower and WiFi access points. Results are retrieved
                 * by means of a network lookup.
                 */
                provider_info = LocationManager.NETWORK_PROVIDER;

            } else if (isGPSEnabled) {
                this.isGPSTrackingEnabled = true;

                Log.d(TAG, "Application use GPS DongleDemoService");

                /*
                 * This provider determines location using
                 * satellites. Depending on conditions, this provider may take a while to return
                 * a location fix.
                 */

                provider_info = LocationManager.GPS_PROVIDER;

            }

            // Application can use GPS or Network Provider
            if (provider_info != null && !provider_info.isEmpty()) {
                if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(
                        provider_info,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this
                );

                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(provider_info);
                    updateGPSCoordinates();
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "Impossible to connect to LocationManager");
        }
    }

    /**
     * Update GPSTracker latitude and longitude
     */
    public void updateGPSCoordinates() {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }

    /**
     * GPSTracker latitude getter and setter
     *
     * @return latitude
     */
    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }

        return latitude;
    }

    /**
     * GPSTracker longitude getter and setter
     *
     * @return
     */
    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }

        return longitude;
    }

    /**
     * GPSTracker isGPSTrackingEnabled getter.
     * Check GPS/wifi is enabled
     */
    public boolean getIsGPSTrackingEnabled() {

        return this.isGPSTrackingEnabled;
    }

    /**
     * Stop using GPS listener
     * Calling this method will stop using GPS in your app
     */
    public void stopUsingGPS() {
        if (locationManager != null) {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    /**
     * Get list of address by latitude and longitude
     *
     * @return null or List<Address>
     */
    public List<Address> getGeocoderAddress(Context context) {
        if (location != null) {

            Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);

            try {
                /**
                 * Geocoder.getFromLocation - Returns an array of Addresses
                 * that are known to describe the area immediately surrounding the given latitude and longitude.
                 */
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, this.geocoderMaxResults);

                return addresses;
            } catch (IOException e) {
                //e.printStackTrace();
                Log.e(TAG, "Impossible to connect to Geocoder", e);
            }
        }

        return null;
    }


    /**
     * Get list of address by latitude and longitude
     *
     * @return null or List<Address>
     */

    public String getAddressFromLatLong(Context context, LatLong latLong) {
        if (latLong != null) {
            double latitude = Double.parseDouble(latLong.latitude);
            double longitude = Double.parseDouble(latLong.longitude);

            Geocoder geocoder = new Geocoder(context, Locale.ENGLISH);

            try {
                /**
                 * Geocoder.getFromLocation - Returns an array of Addresses
                 * that are known to describe the area immediately surrounding the given latitude and longitude.
                 */
                Address addresses = geocoder.getFromLocation(latitude, longitude, this.geocoderMaxResults).get(0);
                String addressLine = addresses.getAddressLine(0);
                return addressLine;
            } catch (IOException e) {
                //e.printStackTrace();
                LogUtil.e(TAG, "Impossible to connect to Geocoder");
            }
        }

        return null;
    }


    /**
     * Try to get AddressLine
     *
     * @return null or addressLine
     */
    public String getAddressLine(Context context) {
        List<Address> addresses = getGeocoderAddress(context);

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String addressLine = address.getAddressLine(0);

            return addressLine;
        } else {
            return null;
        }
    }

    /**
     * Try to get Locality
     *
     * @return null or locality
     */
    public String getLocality(Context context) {
        List<Address> addresses = getGeocoderAddress(context);

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String locality = address.getLocality();

            return locality;
        } else {
            return null;
        }
    }

    /**
     * Try to get Postal Code
     *
     * @return null or postalCode
     */
    public String getPostalCode(Context context) {
        List<Address> addresses = getGeocoderAddress(context);

        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String postalCode = address.getPostalCode();

            return postalCode;
        } else {
            return null;
        }
    }

    /**
     * Try to get CountryName
     *
     * @return null or postalCode
     */
    public String getCountryName(Context context) {
        List<Address> addresses = getGeocoderAddress(context);
        if (addresses != null && addresses.size() > 0) {
            Address address = addresses.get(0);
            String countryName = address.getCountryName();

            return countryName;
        } else {
            return null;
        }
    }

    public long getTime() {
        if (location != null) {
            return location.getTime();
        }
        return 0;
    }

    public double getSpeed() {
        if (location != null) {
            return speed;
        }
        return 0;
    }

    public double getDistance() {
        if (location != null) {
            return this.distance;
        }
        return 0;
    }
    public double setDistance(double distance) {
        if (location != null) {
            this.distance = distance;
            return this.distance;
        }
        return 0;
    }

    @Override
    public void onLocationChanged(Location location) {
        LogUtil.d(TAG, "lat:" + location.getLatitude() + " long:" + location.getLongitude());
        if (this.location != null) {
            this.location.set(location);
        }
        if (location != null) {
            //String Speed = "Device Speed: " +location.getSpeed();
            lat_new = location.getLongitude();
            lon_new = location.getLatitude();
            if (lat_old == 0 && lon_old == 0) {
                lat_old = lat_new;
                lon_old = lon_new;
            }
            String longitude = "Longitude: " + location.getLongitude();
            String latitude = "Latitude: " + location.getLatitude();
            try {
                double distance = calculateSpeedByDistance(lat_new, lon_new, lat_old, lon_old);
                LogUtil.d(TAG, "distance:" + distance);
                time_new = System.currentTimeMillis() / 1000;
                long time = time_new - time_old;
                double time_s = time / 1000.0;
                double speed_mps = distance / time_s;
                speed = (speed_mps * 3600.0) / 1000.0;
                LogUtil.d(TAG, "Speed:" + speed);
                this.distance += calculateDistance(lat_new, lon_new, lat_old, lon_old);
                LogUtil.d(TAG, "Distance:" + this.distance);
                lat_old = lat_new;
                lon_old = lon_new;
                time_old = time_new;
            } catch (Exception e) {
                LogUtil.d(TAG, "exception while calculation speed or distance");
                e.printStackTrace();
            }
        }
        updateGPSCoordinates();
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static final Double EARTH_RADIUS = 6371.00;

    public double calculateSpeedByDistance(double lat1, double lon1, double lat2, double lon2) {
        double Radius = EARTH_RADIUS;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        return Radius * c;
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
        return round(miles,2);
    }
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
    /*public void writeLog(String text){
        BufferedWriter bw = null;
        FileWriter fw = null;
        final String FILENAME = "fleet_logs.txt";
        String path = Environment.getExternalStorageDirectory().getPath() + "/" + FILENAME;

        try {

            File file = new File(path);

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            // true = append file
            fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);

            bw.write(text+"\n");

        } catch (IOException e) {

            e.printStackTrace();

        } finally {

            try {

                if (bw != null)
                    bw.close();

                if (fw != null)
                    fw.close();

            } catch (IOException ex) {

                ex.printStackTrace();

            }
        }
    }*/
}
