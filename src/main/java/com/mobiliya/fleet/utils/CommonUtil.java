package com.mobiliya.fleet.utils;


import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.db.tables.FaultTable;
import com.mobiliya.fleet.db.tables.LatLongTable;
import com.mobiliya.fleet.db.tables.ParameterTable;
import com.mobiliya.fleet.db.tables.TripTable;
import com.mobiliya.fleet.db.tables.UserTable;
import com.mobiliya.fleet.models.BaseParameters;
import com.mobiliya.fleet.models.FaultModel;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.OBDFaultCode;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.location.GPSTracker;
import com.mobiliya.fleet.location.GpsLocationReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.LOCATION_SERVICE;
import static com.mobiliya.fleet.utils.Constants.ACCESS_LOCATION;

public class CommonUtil {
    private static final String TAG = "CommonUtil";

    public static String getPostDataString(JSONObject params) throws Exception {
        try {
            StringBuilder result = new StringBuilder();
            boolean first = true;
            Iterator<String> itr = params.keys();

            while (itr.hasNext()) {

                String key = itr.next();
                Object value = params.get(key);

                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(key, "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(value.toString(), "UTF-8"));

            }
            LogUtil.d(TAG, "Post request String:" + result.toString());
            return result.toString();
        } catch (Exception e) {
            Log.d("AKIL", "Error while parsing: " + e.getMessage());
            return "";
        }
    }


    /**
     * Turns On Bluetooth
     */
    public static boolean isBluetoothOn() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return !mBluetoothAdapter.isEnabled() && mBluetoothAdapter.enable();
    }



    public static Boolean isOnline(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Checks Internet connection
     */
    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting() && netinfo.isAvailable() && netinfo.isConnected()) {
            android.net.NetworkInfo wifi = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            return (mobile != null && mobile.isConnected()) || (wifi != null && wifi.isConnected());
        } else {
            return false;
        }
    }


    public static void showToast(Activity activity, String message) {
        Context context = activity.getBaseContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_toast, (ViewGroup) activity.findViewById(R.id.toast_layout_root));
        TextView text = (TextView) layout.findViewById(R.id.text);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    public static String getVehicleAndParameterJson(BaseParameters param,/* User user, */Context context) {

        String paramString = new Gson().toJson(param);
        JSONObject messageJSON = new JSONObject();
        try {
            messageJSON.put("Parameters", paramString);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return messageJSON.toString();
    }


    /**
     * Handling negative values
     * Returns No data if value is negative
     *
     * @param text
     * @return
     */
    public static String getPositiveData(String text) {
        String updated_text = text;
        try {
            // checking valid integer using parseInt() method
            int int_val = Integer.parseInt(text);
            if (int_val < 0) {
                return "No Data found";
            }
        } catch (NumberFormatException e) {
        }

        try {
            // checking valid integer using parseInt() method
            Double long_val = Double.parseDouble(updated_text.trim());
            if (long_val < 0.0) {
                return "No Data found";
            }
        } catch (NumberFormatException e) {
        }
        return text;

    }


    public static boolean checkLocationPermission(final Activity activity) {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(activity.getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission necessary");
                    alertBuilder.setMessage("Access location permission is necessary to get current location!!!");
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions(activity, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }


    /**
     * Clear App Cache
     *
     * @return
     */

    public static void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else
            return dir != null && dir.isFile() && dir.delete();
    }

    private static final long MEGABYTE = 1024L * 1024L;

    /**
     * Convert Bytes to Megabytes
     *
     * @param bytes
     * @return
     */
    public static long bytesToMeg(long bytes) {
        return bytes / MEGABYTE;
    }

    /**
     * Convert Megabytes to Bytes
     *
     * @param megabytes
     * @return
     */
    public static long megToBytes(long megabytes) {
        return megabytes * MEGABYTE;
    }

    public static AlertDialog.Builder showNetworkDialog(Context c) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("No Internet connection.");
        builder.setMessage("You have no internet connection");

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        return builder;
    }

    public static boolean isPasswordLengthValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private static final String PASSWORD_PATTERN =
            "((?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])|(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[^a-zA-Z0-9])|(?=.*?[A-Z])(?=.*?[0-9])(?=.*?[^a-zA-Z0-9])|(?=.*?[a-z])(?=.*?[0-9])(?=.*?[^a-zA-Z0-9])).{8,}";

    /**
     * Validate password with regular expression
     *
     * @param password password for validation
     * @return true valid password, false invalid password
     */
    public static boolean isValidPassword(final String password) {
        Pattern pattern = Pattern.compile(PASSWORD_PATTERN);
        Matcher matcher = pattern.matcher(password);
        return matcher.matches();

    }

    public static boolean isValidPhone(CharSequence phone) {
        return !TextUtils.isEmpty(phone) && android.util.Patterns.PHONE.matcher(phone).matches();
    }


    @SuppressLint("SimpleDateFormat")
    public static String convertDateTo_EEEE_MMMM_d_hh_mm_a_Format(String title) {
        String currentdate = title;
        if (title != null) {
            try {
                @SuppressLint("SimpleDateFormat") DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
                Date date = formatter1.parse(title);

                currentdate = new SimpleDateFormat("EEEE, MMMM d, hh:mm a").format(date);
                return currentdate;
            } catch (Exception ex) {
                ex.getMessage();
            }

            try {
                @SuppressLint("SimpleDateFormat") DateFormat formatter2 = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                Date date = formatter2.parse(title);

                currentdate = new SimpleDateFormat("EEEE, MMMM d, hh:mm a").format(date);
                return currentdate;
            } catch (Exception ex) {
                ex.getMessage();
            }

            try {
                @SuppressLint("SimpleDateFormat") DateFormat formatter3 = new SimpleDateFormat("EEE, MMM dd, hh:mm a");
                Date date = formatter3.parse(title);

                currentdate = new SimpleDateFormat("EEEE, MMMM d'th', hh:mm a").format(date);
                return currentdate;
            } catch (Exception ex) {
                ex.getMessage();
            }
        }
        return currentdate;
    }

    @SuppressLint("ObsoleteSdkInt")
    public static boolean isAppInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    public static boolean isEnddateLessThanStartDate(String startTime, String endTime) {
        boolean check = false;
        try {
            @SuppressLint("SimpleDateFormat") DateFormat formatter1 = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
            Date start = formatter1.parse(startTime);
            Date end = formatter1.parse(endTime);
            if (start.compareTo(end) > 0) {
                check = true;
            }
        } catch (Exception ex) {
            ex.getMessage();
        }
        return check;
    }

    /**
     * Calulate date difference from two dates
     *
     * @param todaysDate
     * @param oldDate
     * @return
     */
    public static boolean isDateDifferenceMoreThan12Hrs(String todaysDate, String oldDate) {
        boolean isDifference = false;

        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Date d1 = null;
        Date d2 = null;
        try {
            d1 = format.parse(oldDate);
            d2 = format.parse(todaysDate);

            //in milliseconds
            long diff = d2.getTime() - d1.getTime();
            long diffDays = diff / (12 * 60 * 60 * 1000);
            if (diffDays > 1) {
                isDifference = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isDifference;
    }

    public static String decodeString(String data) {
        byte[] bytes = Base64.decode(data, Base64.DEFAULT);
        String string = null;
        try {
            string = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LogUtil.d(TAG, "Error in converting decoded string");
            e.printStackTrace();
        }

        return string;
    }

    public static int getIntegerDelay(String delay) {
        String numerictime = delay.split("\\s+")[0];
        int synctime = Integer.parseInt(numerictime);
        return synctime;
    }

    public static int getValueFromTimeArray(int synctime, String[] dataSyncTime) {
        //  String[] dataSyncTime = { "30 sec", "1 min", "2 min", "3 min", "5 min"};
        int position = 0;
        for (int i = 0; i < dataSyncTime.length; i++) {
            String time = dataSyncTime[i];


            int numerictime = getIntegerDelay(time);
            if (synctime == numerictime) {
                position = i;
                break;
            }
        }
        return position;
    }

    public static void closeKeyPad(Activity activity, Button savebtn) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(savebtn.getWindowToken(), 0);
    }

    /**
     * Send Notification From service
     *
     * @param activity
     */
    public static void sendNotification(Context activity) {

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(activity);

        //Create the intent that’ll fire when the user taps the notification//

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.androidauthority.com/"));
        PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);

        mBuilder.setContentIntent(pendingIntent);

        //mBuilder.setSmallIcon(R.drawable.logo);
        mBuilder.setContentTitle(activity.getString(R.string.app_name));
        mBuilder.setContentText("Fault Raised");

        NotificationManager mNotificationManager =

                (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(001, mBuilder.build());
    }


    /**
     * Setting current loaction in prefs required for in
     * onCreate method
     */
    public static LatLong getCurrentLocation(Context cxt) {
        GPSTracker gpsTracker = GPSTracker.getInstance(cxt);
        Double lat = gpsTracker.getLatitude();
        Double longi = gpsTracker.getLongitude();
        LatLong latlog = new LatLong(String.valueOf(lat), String.valueOf(longi));
        return latlog;
    }

    /**
     * Checks id database size exceeds the db size set from settings
     *
     * @param serviceContext
     * @return
     */
    public static boolean checkDbSizeExceeds(Context serviceContext) {
        DatabaseProvider provider = DatabaseProvider.getInstance(serviceContext);
        Long dblength = new File(provider.getDB().getReadableDatabase().getPath()).length();
        long megabyte = CommonUtil.bytesToMeg(dblength);
        long dbsize = SharePref.getInstance(serviceContext).getMemorySize();
        return megabyte == dbsize || megabyte > dbsize;
    }

    public static String readFile(String path)
            throws IOException {
        InputStream is = new FileInputStream(path);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));
        String line = buf.readLine();
        StringBuilder sb = new StringBuilder();
        while (line != null) {
            sb.append(line).append("\n");
            line = buf.readLine();
        }
        String fileAsString = sb.toString();
        LogUtil.d(TAG, "file DATA fileAsString:" + fileAsString);
        return fileAsString;
    }

    public static String getOBDFaultProperty(String key, Context context) throws IOException {
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open("faultOBDII.properties");
        properties.load(inputStream);
        return properties.getProperty(key);
    }

    public static FaultModel getOBDError(String spn, Context ctx) {
        String message = "";
        OBDFaultCode spnFound = null;
        try {
            message = getOBDFaultProperty(String.valueOf(spn), ctx);
            spnFound = new OBDFaultCode(spn, message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String spnDesc = "";
        if (spnFound != null)
            spnDesc = spnFound.Description;

        String desc = spnDesc;
        if (TextUtils.isEmpty(message)) {
            desc = spn;
            spn = "";
        }
        FaultModel fault = new FaultModel("", spn, "", desc, DateUtils.getLocalTimeString());
        return fault;
    }

    public static String decoded(String JWTEncoded) throws Exception {
        String decodedToken = "";
        try {
            String[] split = JWTEncoded.split("\\.");
            LogUtil.d("JWT_DECODED", "Header: " + getJson(split[0]));
            decodedToken = getJson(split[1]);
            LogUtil.d("JWT_DECODED", "Body: " + decodedToken);
        } catch (UnsupportedEncodingException e) {
            LogUtil.d(TAG, "error in parsing JWTToken");
        }
        return decodedToken;
    }

    private static String getJson(String strEncoded) throws UnsupportedEncodingException {
        byte[] decodedBytes = Base64.decode(strEncoded, Base64.URL_SAFE);
        return new String(decodedBytes, "UTF-8");
    }

    public static void registerGpsReceiver(Context baseContext, GpsLocationReceiver gpsLocationReceiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.location.PROVIDERS_CHANGED");
        filter.addAction(Constants.LOCATION_NOT_CHANGED);
        baseContext.registerReceiver(gpsLocationReceiver, filter);

        LocationManager locationManager = (LocationManager) baseContext.getSystemService(LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGPSEnabled) {
            baseContext.sendBroadcast(new Intent(Constants.LOCATION_NOT_CHANGED));
        }
    }

    public static void unRegisterGpsReceiver(Context baseContext, GpsLocationReceiver gpsLocationReceiver) {
        if (gpsLocationReceiver != null) {
            baseContext.unregisterReceiver(gpsLocationReceiver);
        }
    }

    public static void trimCache(Context cxt) {
        try {
            File dir = cxt.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDirectory(dir);
            }
        } catch (Exception e) {
            LogUtil.d(TAG, "Exception in trimCache");
        }
    }


    public static String getTimeDiff(Context cxt, Trip trip) {
        String diff = "0.0";

        Date start = DateUtils.getDateFromString(trip.startTime);
        diff = DateUtils.getTimeDifference(start);
        SharePref.getInstance(cxt).addItem(Constants.TIME_ONGOING, diff);
        return diff;
    }

    public static boolean deleteDirectory(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }

    public static float milesDriven(Context context, float distance) {
        LogUtil.d(TAG, "distance:" + distance);

        float milesdriven = 0.0f;
        if (distance < 0) {
            LogUtil.d(TAG, "distance value is less then 0");
            return 0.0f;
        }
        //calculation for miles driven
        float firstMilage = SharePref.getInstance(context).getItem(Constants.FIRST_MILES_ONGOING, -1.0f);
        LogUtil.d(TAG, "firstmilage:" + firstMilage);
        if (firstMilage == -1.0) {
            SharePref.getInstance(context).addItem(Constants.FIRST_MILES_ONGOING, distance);
            LogUtil.d(TAG, "First milage reading set:" + distance);
            return milesdriven;
        } else {
            milesdriven = distance - firstMilage;
            if (milesdriven <= 0) {
                LogUtil.d(TAG, "Miles driven less then previous cached miles");
                return 0.0f;
            } else if (milesdriven > 100) {
                LogUtil.d(TAG, "Miles driven great then 100 miles so we are ignoring");
                return 0.0f;
            }
            LogUtil.d(TAG, "Distance travel :" + milesdriven);
            SharePref.getInstance(context).addItem(Constants.TOTAL_MILES_ONGOING, milesdriven);
            return milesdriven;
        }
    }

    public static void deletAllDatabaseTables(Context cxt) {
        try {
            DatabaseProvider.getInstance(cxt).deleteAllTableData(new UserTable());
            DatabaseProvider.getInstance(cxt).deleteAllTableData(new ParameterTable());
            DatabaseProvider.getInstance(cxt).deleteAllTableData(new TripTable());
            DatabaseProvider.getInstance(cxt).deleteAllTableData(new FaultTable());
            DatabaseProvider.getInstance(cxt).deleteAllTableData(new LatLongTable());
        } catch (Exception ex) {
            LogUtil.d(TAG, "Failed while deleting db");
        }
    }

    public static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
