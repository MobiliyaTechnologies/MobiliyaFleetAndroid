package com.mobiliya.fleet.location;

import android.location.Location;

/**
 * This class contains all the details about a particular location.
 */
@SuppressWarnings({"ALL", "unused"})
public class LocationInfo {

    private double mLongitude = 0;
    private double mLatitude = 0;
    private double mAltitude = 0;
    private double mAccuracy = 0;
    private long mUtcTime = 0;
    private String mAddress = null;

    public LocationInfo(Location location) {
        if (location != null) {
            this.mAccuracy = location.getAccuracy();
            this.mAltitude = location.getAltitude();
            this.mLatitude = location.getLatitude();
            this.mLongitude = location.getLongitude();
            this.mUtcTime = location.getTime();
        }
    }

    public LocationInfo() {

    }

    /**
     * Returns the location longitude.
     *
     * @return the location longitude.
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Set the location longitude.
     *
     * @param longitude the location longitude.
     */
    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    /**
     * Returns the location latitude.
     *
     * @return the location latitude.
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Sets the location latitude.
     *
     * @param latitude the location latitude.
     */
    public void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    /**
     * Returns the location Altitude.
     *
     * @return the location Altitude.
     */
    public double getAltitude() {
        return mAltitude;
    }

    /**
     * Sets the location Altitude.
     *
     * @param altitude the location altitude.
     */
    public void setAltitude(double altitude) {
        this.mAltitude = altitude;
    }

    /**
     * Returns the location Accuracy.
     *
     * @return the location Accuracy.
     */
    public double getAccuracy() {
        return mAccuracy;
    }

    /**
     * Sets the the location Accuracy.
     *
     * @param accuracy the location accuracy.
     */
    public void setAccuracy(double accuracy) {
        this.mAccuracy = accuracy;
    }

    /**
     * Returns the time in UTC when location is recorded.
     *
     * @return the time in UTC when location is recorded.
     */
    public long getUtcTime() {
        return mUtcTime;
    }

    /**
     * Sets the time in UTC.
     *
     * @param utcTime the time in UTC.
     */
    public void setUtcTime(long utcTime) {
        this.mUtcTime = utcTime;
    }

    /**
     * Returns the address based on the coordinates.
     *
     * @return the Address string.
     */
    public String getAddress() {
        return mAddress;
    }

    /**
     * Sets the address string.
     *
     * @param address the address to set.
     */
    public void setAddress(String address) {
        this.mAddress = address;
    }

}
