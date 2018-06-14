package com.mobiliya.fleet.utils;

import android.app.Application;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
            appendToFile(tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.INFO)) {
            Log.i(TAG, tag + " : " + msg);
            appendToFile(tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.ERROR)) {
            Log.e(TAG, tag + " : " + msg);
            appendToFile(tag, msg);
        }
    }

    public static void v(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(TAG, tag + " : " + msg);
            appendToFile(tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (DEBUG) {// && Log.isLoggable(tag, Log.WARN)) {
            Log.w(TAG, tag + " : " + msg);
            appendToFile(tag, msg);
        }
    }

    private static void appendToFile(String logMessageTag, String logMessage) {
        /*try
        {
            // Gets the log file from the root of the primary storage. If it does
            // not exist, the file is created.
            File logFile = new File(Environment.getExternalStorageDirectory(),
                    "TestApplicationLog.txt");
            if (!logFile.exists())
                logFile.createNewFile();
            // Write the message to the log with a timestamp
            BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true));
            writer.write(String.format("%1s [%2s]:%3s\r\n",
                    getDateTimeStamp(), logMessageTag, logMessage));
            writer.close();

        }
        catch (IOException e)
        {
            LogUtil.e(TAG, "Unable to log exception to file.");
        }*/
    }

    private static String getDateTimeStamp() {
        Date dateNow = Calendar.getInstance().getTime();
        // My locale, so all the log files have the same date and time format
        return (DateFormat.getDateTimeInstance
                (DateFormat.SHORT, DateFormat.SHORT, Locale.ENGLISH).format(dateNow));
    }
}
