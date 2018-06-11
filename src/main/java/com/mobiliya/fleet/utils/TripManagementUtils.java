package com.mobiliya.fleet.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.location.Location;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.activity.DashboardActivity;
import com.mobiliya.fleet.activity.TripActivity;
import com.mobiliya.fleet.activity.TripListActivity;
import com.mobiliya.fleet.adapters.ApiCallBackListener;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.services.GPSTracker;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.mobiliya.fleet.activity.DashboardActivity.sPopupWindow;
import static com.mobiliya.fleet.activity.TripActivity.mPauseIcon;
import static com.mobiliya.fleet.activity.TripActivity.mPause_tv;
import static com.mobiliya.fleet.utils.CommonUtil.showToast;
import static com.mobiliya.fleet.utils.Constants.GET_TRIP_LIST_URL;
import static com.mobiliya.fleet.utils.Constants.LATITUDE;
import static com.mobiliya.fleet.utils.Constants.LONGITUDE;
import static com.mobiliya.fleet.utils.DateUtils.getLocalTimeString;

@SuppressWarnings({"ALL", "unused"})
public class TripManagementUtils {
    private static final String TAG = "TripManagementUtils";
    public static int count = 0;
    public static ApiCallBackListener apicallback;

    public static String startTrip(final Activity activity, LatLng latlong) {
        final ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setIndeterminate(true);
        dialog.setMessage("Starting Trip....");
        dialog.setCancelable(false);
        dialog.show();

        String tripId = null;

        @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("EEEE, MMMM dd, hh:mm a");
        String formattedDate = df.format(new Date());
        String vehicle = SharePref.getInstance(activity).getVehicleID();

        GPSTracker gpsTracker = GPSTracker.getInstance(activity);
        Double latitude = latlong.getLatitude();
        Double longitude = latlong.getLongitude();
        String address = gpsTracker.getAddressFromLatLong(activity.getBaseContext(), new LatLong(String.valueOf(latitude), String.valueOf(longitude)));


        Trip newTrip = new Trip();
        UUID uuid = UUID.randomUUID();
        String randomUUIDString = uuid.toString();
        newTrip.commonId = randomUUIDString;
        newTrip.tripName = "New Trip";
        newTrip.description = newTrip.tripName;
        newTrip.startTime = getLocalTimeString();
        newTrip.StartDate = getLocalTimeString();
        newTrip.startLocation = latitude + "," + longitude + "#" + address;
        newTrip.endLocation = "NA";
        newTrip.vehicleId = vehicle;
        newTrip.status = TripStatus.Start.getValue();
        newTrip.IsSynced = false;

        if (TextUtils.isEmpty(newTrip.vehicleId)) {
            dialog.dismiss();
            showToast(activity, "Please add your vehicle number");
            return null;
        }
        tripId = DatabaseProvider.getInstance(activity).addTrip(newTrip);
        dialog.dismiss();
        return tripId;
    }

    public static void pauseTrip(Activity activity) {
        Context cxt = activity.getApplicationContext();
        Trip newTrip = DatabaseProvider.getInstance(cxt).getCurrentTrip();

        newTrip.stops = newTrip.stops + 1;
        newTrip.status = TripStatus.Pause.getValue();
        newTrip.IsSynced = false;

        try {
            int recordAffected = DatabaseProvider.getInstance(cxt).updateTrip(newTrip);
            if (recordAffected > 0) {
                DatabaseProvider.getInstance(cxt).addNumberOfStops(newTrip.commonId);
                showToast(activity, cxt.getString(R.string.trip_paused));
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error while update trip " + ex);
        }
    }

    //This code is used by Notification Manager
    public static void pauseTripFromNotification(Context cxt) {
        Trip newTrip = DatabaseProvider.getInstance(cxt).getCurrentTrip();

        newTrip.stops = newTrip.stops + 1;
        newTrip.status = TripStatus.Pause.getValue();
        newTrip.IsSynced = false;

        try {
            int recordAffected = DatabaseProvider.getInstance(cxt).updateTrip(newTrip);
            if (recordAffected > 0) {
                DatabaseProvider.getInstance(cxt).addNumberOfStops(newTrip.commonId);
                Toast.makeText(cxt, cxt.getString(R.string.trip_paused), Toast.LENGTH_LONG).show();
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error while update trip " + ex);
        }
    }

    public static void resumeTrip(Context ctx) {
        Trip newTrip = DatabaseProvider.getInstance(ctx).getCurrentTrip();

        newTrip.status = TripStatus.Start.getValue();
        newTrip.IsSynced = false;

        try {
            int recordAffected = DatabaseProvider.getInstance(ctx).updateTrip(newTrip);
            if (recordAffected > 0) {
            }

        } catch (Exception ex) {
            Log.d(TAG, "Error while Resume trip " + ex);
        }
    }

    public static long stopTrip(Activity activity) {

        final ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setIndeterminate(true);
        dialog.setMessage("Stopping Trip....");
        dialog.setCancelable(false);
        dialog.show();
        long recordAffected = -1;

        GPSTracker gpsTracker = GPSTracker.getInstance(activity);
        Double latitude = Double.valueOf(SharePref.getInstance(activity.getBaseContext()).getItem(LATITUDE));
        Double longitude = Double.valueOf(SharePref.getInstance(activity.getBaseContext()).getItem(LONGITUDE));

        if(latitude!=0.0 &&longitude!=0.0 ) {
            String address = gpsTracker.getAddressFromLatLong(activity.getBaseContext(), new LatLong(String.valueOf(latitude), String.valueOf(longitude)));

            SharePref pref = SharePref.getInstance(activity.getApplication());

            try {
                Trip newTrip = DatabaseProvider.getInstance(activity).getCurrentTrip();
                if (newTrip == null) {
                    LogUtil.d(TAG, "currect trip is null");
                    return -1;
                }

                newTrip.endLocation = latitude + "," + longitude + "#" + address;
                newTrip.endTime = getLocalTimeString();
                newTrip.EndDate = getLocalTimeString();
                newTrip.status = TripStatus.Stop.getValue();
                newTrip.stops = DatabaseProvider.getInstance(activity).getStopsCount(newTrip.commonId);
                newTrip.IsSynced = false;

                pref.addItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                pref.addItem(Constants.MILES_ONGOING, 0.0f);
                pref.addItem(Constants.FUEL_ONGOING, "NA");
                pref.addItem(Constants.TIME_ONGOING, "0");

                recordAffected = DatabaseProvider.getInstance(activity.getApplicationContext()).updateTrip(newTrip);

                if (recordAffected > 0) {
                    DatabaseProvider.getInstance(activity.getBaseContext()).deleteLatLong(newTrip.commonId);
                    showToast(activity, activity.getString(R.string.trip_stopped));

                    pref.addItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                    pref.addItem(Constants.MILES_ONGOING, 0.0f);
                    pref.addItem(Constants.SPEEDING, 0);
                    pref.addItem(Constants.FUEL_ONGOING, "NA");
                    pref.addItem(Constants.TIME_ONGOING, "0");
                }
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            } catch (Exception ex) {
                ex.getMessage();
            } finally {
                dialog.dismiss();
            }
        }
        if(recordAffected==-1){
            Toast.makeText(activity, activity.getString(R.string.failed_to_start_trip_nogps), Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }
        return recordAffected;
    }


    public static long stopTripFromNotification(Context context) {
        long recordAffected = -1;
        GPSTracker tracer = GPSTracker.getInstance(context);
        Double latitude = Double.valueOf(SharePref.getInstance(context).getItem(LATITUDE));
        Double longitude = Double.valueOf(SharePref.getInstance(context).getItem(LONGITUDE));

        if(latitude!=0.0 &&longitude!=0.0 ) {
            String address = tracer.getAddressFromLatLong(context, new LatLong(String.valueOf(latitude), String.valueOf(longitude)));

            try {
                Trip newTrip = DatabaseProvider.getInstance(context).getCurrentTrip();
                if (newTrip == null) {
                    LogUtil.d(TAG, "currect trip is null");
                    return -1;
                }

                newTrip.endLocation = latitude + "," + longitude + "#" + address;
                newTrip.endTime = getLocalTimeString();
                newTrip.EndDate = getLocalTimeString();
                newTrip.status = TripStatus.Stop.getValue();
                newTrip.stops = DatabaseProvider.getInstance(context).getStopsCount(newTrip.commonId);
                newTrip.IsSynced = false;

                recordAffected = DatabaseProvider.getInstance(context).updateTrip(newTrip);

                if (recordAffected > 0) {
                    DatabaseProvider.getInstance(context).deleteLatLong(newTrip.commonId);
                    Toast.makeText(context, context.getString(R.string.trip_stopped), Toast.LENGTH_LONG).show();
                    SharePref.getInstance(context).addItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                    SharePref.getInstance(context).addItem(Constants.MILES_ONGOING, 0.0f);
                    SharePref.getInstance(context).addItem(Constants.FUEL_ONGOING, "NA");
                    SharePref.getInstance(context).addItem(Constants.TIME_ONGOING, "0");
                    SharePref.getInstance(context).addItem(Constants.SPEEDING, 0);
                }
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }

        if(recordAffected==-1){
            Toast.makeText(context, context.getString(R.string.failed_to_start_trip_nogps), Toast.LENGTH_LONG).show();
        }

        return recordAffected;
    }

    public static synchronized void SendLocalTripsToServer(Context ctx) {
        List<Trip> newTripList = DatabaseProvider.getInstance(ctx).getLastUnSyncedTrip();
        if (newTripList != null && newTripList.size() > 0) {
            for (Trip trip : newTripList) {

                addTripOnServer(ctx, trip, new ApiCallBackListener() {
                    @Override
                    public void onSuccess(JSONObject object) {
                        LogUtil.d(TAG, "onSuccess");
                    }

                    @Override
                    public void onError(VolleyError result) {
                        LogUtil.d(TAG, "onError");
                    }
                });
            }
        }
    }


    public static synchronized void addTripOnServer(final Context ctx, final Trip trip, final ApiCallBackListener listener) {
        apicallback = listener;
        JSONObject jsonBody = new JSONObject();
        String newTripRequestData = null;
        LogUtil.d(TAG, "addTripOnServer()");
        try {
            jsonBody.put("commonId", trip.commonId);
            jsonBody.put("tripName", trip.tripName);
            jsonBody.put("startTime", trip.startTime);
            jsonBody.put("endTime", trip.endTime);
            jsonBody.put("startLocation", trip.startLocation);
            jsonBody.put("endLocation", trip.endLocation);
            jsonBody.put("vehicleId", trip.vehicleId);
            jsonBody.put("description", trip.description);
            jsonBody.put("stops", trip.stops);
            jsonBody.put("status", trip.status);
            newTripRequestData = CommonUtil.getPostDataString(jsonBody);
            LogUtil.d(TAG, "addTripOnServer() with Json:" + newTripRequestData);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String tenantId = SharePref.getInstance(ctx).getUser().getTenantId();
        String url = String.format(Constants.getTripsURLs(ctx, Constants.ADD_TRIP_URL), tenantId);

        try {
            VolleyCommunicationManager.getInstance().SendRequest(url, Request.Method.POST, newTripRequestData, ctx, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {
                            if (result.getString("message").equals("Success")) {
                                String data = result.getString("data");
                                Type type = new TypeToken<Trip>() {
                                }.getType();
                                Gson gson = new Gson();
                                Trip trip_result = null;
                                try {
                                    trip_result = gson.fromJson(data, type);
                                } catch (Exception ex) {
                                    ex.getMessage();
                                }

                                if (trip.status == TripStatus.Stop.getValue()) {
                                    SharePref.getInstance(ctx).addItem("firstmilage", 0);
                                    DatabaseProvider.getInstance(ctx).deleteTrip(trip.commonId);
                                } else if (trip.status == TripStatus.Pause.getValue()) {
                                    trip._id = trip_result._id;
                                    trip.commonId = trip_result.commonId;
                                    trip.IsSynced = true;
                                    DatabaseProvider.getInstance(ctx).updateTrip(trip);
                                } else {
                                    trip._id = trip_result._id;
                                    trip.commonId = trip_result.commonId;
                                    trip.IsSynced = true;
                                    DatabaseProvider.getInstance(ctx).updateTrip(trip);
                                }
                                LogUtil.d(TAG, "addTripOnServer() onSucess()");
                            } /*else if (result.getString("message").equals("Unauthorized")) {
                                LogUtil.d(TAG, "Add Trip Request Failed due to:" + result.getString("message"));
                                if (trip.status == TripStatus.Stop.getValue()) {
                                    DatabaseProvider.getInstance(ctx).deleteTrip(trip.commonId);
                                }
                            } else if (result.getString("message").equals("InternalServerError")) {
                                LogUtil.d(TAG, "Add Trip Request Failed due to:" + result.getString("message"));
                                if (trip.status == TripStatus.Stop.getValue()) {
                                    DatabaseProvider.getInstance(ctx).deleteTrip(trip.commonId);
                                }
                            }*/ else {
                                LogUtil.d(TAG, "Add Trip Request Failed (Duplicate Trip): " + result.getString("message"));
                            }

                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }
                        listener.onSuccess(result);
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    if (result != null) {
                        Log.d(TAG, "Error while updating server trip" + result.toString());
                    }
                    listener.onError(result);
                }
            });
        } catch (Exception ex) {

        }
    }

    public static synchronized void getTripList(final Activity cxt, ApiCallBackListener listener) {
        apicallback = listener;
        final ProgressDialog dialog = new ProgressDialog(cxt);
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.setMessage("Loading trips");
        if (cxt instanceof TripListActivity) {
            dialog.show();
        }
        try {
            String tenantId = SharePref.getInstance(cxt.getBaseContext()).getUser().getTenantId();
            String gettripsUrl = String.format(Constants.getTripsURLs(cxt, GET_TRIP_LIST_URL), tenantId);
            String vehicleId = SharePref.getInstance(cxt.getBaseContext()).getVehicleID();
            gettripsUrl = gettripsUrl + "?order=desc&limit=10&vehicleId=" + vehicleId;
            VolleyCommunicationManager.getInstance().SendRequest(gettripsUrl, Request.Method.GET, null, cxt, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (cxt instanceof TripListActivity) {
                        dismissProgressDialog(dialog);
                    }
                    if (apicallback != null) {
                        apicallback.onSuccess(result);
                    }

                }

                @Override
                public void onError(VolleyError result) {
                    if (cxt instanceof TripListActivity) {
                        dismissProgressDialog(dialog);
                    }
                    if (apicallback != null) {
                        apicallback.onError(result);
                    }

                }
            });
        } catch (Exception e) {
            if (cxt instanceof TripListActivity) {
                dismissProgressDialog(dialog);
            }
        }

    }

    private static void dismissProgressDialog(ProgressDialog pDialog) {
        try {
            if (pDialog != null && pDialog.isShowing()) {
                pDialog.dismiss();
            }
        } catch (final IllegalArgumentException e) {
            LogUtil.d(TAG, "IllegalArgumentException: " + e.getMessage());
        } catch (final Exception e) {
            LogUtil.d(TAG, "Exception: " + e.getMessage());
        } finally {
            pDialog = null;
        }
    }


    public static synchronized void getLastTrip(final Activity cxt, ApiCallBackListener listener) {
        apicallback = listener;

        try {
            String tenantId = SharePref.getInstance(cxt.getBaseContext()).getUser().getTenantId();
            String getTripsUrl = String.format(Constants.getTripsURLs(cxt, GET_TRIP_LIST_URL), tenantId);
            String vehicleId = SharePref.getInstance(cxt.getBaseContext()).getVehicleID();
            getTripsUrl = getTripsUrl + "?limit=1&vehicleId=" + vehicleId;
            LogUtil.d(TAG, "URL for get last trip: " + getTripsUrl);
            VolleyCommunicationManager.getInstance().SendRequest(getTripsUrl, Request.Method.GET, null, cxt, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {

                    if (apicallback != null) {
                        apicallback.onSuccess(result);
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    if (apicallback != null) {
                        apicallback.onError(result);
                    }

                }
            });
        } catch (Exception e) {
        }

    }


    public static void showDongleDisconnectedDialog(final Activity activity) {
        if (count == 0) {
            count = 1;
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!activity.isFinishing()) {
                        final android.support.v7.app.AlertDialog.Builder loginDialog = new android.support.v7.app.AlertDialog.Builder(activity);
                        LayoutInflater factory = LayoutInflater.from(activity);
                        View customTitleView = activity.getLayoutInflater().inflate(R.layout.alert_custom_tilte, null);
                        TextView header = (TextView) customTitleView.findViewById(R.id.title_header);
                        header.setText("Ignition Off");
                        View dialogView = factory.inflate(R.layout.dongle_disconnected_dialog, null);
                        loginDialog.setCustomTitle(customTitleView);
                        loginDialog.setView(dialogView);

                        final TextView pausetrip = (TextView) dialogView.findViewById(R.id.pause);
                        final android.support.v7.app.AlertDialog alertDialog = loginDialog.show();

                        pausetrip.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                count = 0;
                                TripManagementUtils.pauseTrip(activity);

                                if ((activity instanceof TripActivity)) {
                                    mPause_tv.setText(activity.getString(R.string.paused));
                                    mPauseIcon.setImageDrawable(activity.getDrawable(R.drawable.play_icon));
                                }

                                if (activity instanceof DashboardActivity) {
                                    if (((DashboardActivity) activity).sPopupWindow != null) {
                                        if (sPopupWindow.isShowing()) {
                                            ((DashboardActivity) activity).sTv_btn_pause.setImageDrawable(activity.getDrawable(R.drawable.play_icon));
                                        }
                                    }
                                }

                                alertDialog.dismiss();
                            }
                        });
                        final TextView endTrip = (TextView) dialogView.findViewById(R.id.end_trip);
                        endTrip.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                count = 0;
                                alertDialog.dismiss();

                                if(TripManagementUtils.stopTrip(activity)>0) {

                                    NotificationManagerUtil.getInstance().dismissNotification(activity.getBaseContext());
                                    if ((activity instanceof TripActivity)) {
                                        activity.finish();
                                    }

                                    if (activity instanceof DashboardActivity) {
                                        if (((DashboardActivity) activity).sPopupWindow != null) {
                                            if (sPopupWindow.isShowing()) {
                                                sPopupWindow.dismiss();
                                            }

                                        }
                                    }
                                }
                            }
                        });
                    }
                }
            });
        }
    }
}
