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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Utility wrapper class to log debug information.
 */
public class LogUtil {
    // Make this flag to false to disable logging.
    private static final boolean DEBUG = true;

    private static final String TAG = LogUtil.class.getSimpleName();

    public static void d(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(TAG, tag + " : " + msg);
            writeLog(tag+": "+msg);
        }
    }

    public static void i(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.INFO)) {
            Log.i(TAG, tag + " : " + msg);
            writeLog(tag+": "+msg);
        }
    }

    public static void e(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.ERROR)) {
            Log.e(TAG, tag + " : " + msg);
            writeLog(tag+": "+msg);
        }
    }

    public static void v(String tag, String msg) {
        if (DEBUG) { // && Log.isLoggable(tag, Log.VERBOSE)) {
            Log.v(TAG, tag + " : " + msg);
            writeLog(tag+": "+msg);
        }
    }

    public static void w(String tag, String msg) {
        if (DEBUG) {// && Log.isLoggable(tag, Log.WARN)) {
            Log.w(TAG, tag + " : " + msg);
            writeLog(tag+": "+ msg);
        }
    }

    private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    public static void writeLog(String text) {
        Date date = new Date();
        BufferedWriter bw = null;
        FileWriter fw = null;
        final String FILENAME = "fleet_logs1.txt";
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
            bw.write(dateFormat.format(date) + ": " + text + "\n");
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
    }
}
