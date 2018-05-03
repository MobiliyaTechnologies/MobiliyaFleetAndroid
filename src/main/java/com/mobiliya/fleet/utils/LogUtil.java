package com.mobiliya.fleet.utils;

import android.util.Log;

/**
 * Utility wrapper class to log debug information.
 */
@SuppressWarnings({"ALL", "unused"})
public class LogUtil {
    // Make this flag to false to disable logging.
    private static final boolean DEBUG = true;

    private static final String TAG = LogUtil.class.getSimpleName();

    public static void d(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(TAG, tag + " : " + msg);
        }
    }

    public static void i(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.INFO)) {
            Log.i(TAG, tag + " : " + msg);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.ERROR)) {
            Log.e(TAG, tag + " : " + msg);
        }
    }

    public static void v(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(TAG, tag + " : " + msg);
        }
    }

    public static void w(String tag, String msg) {
        if (DEBUG) {// && Log.isLoggable(tag, Log.WARN)) {
            Log.w(TAG, tag + " : " + msg);
        }
    }
}
