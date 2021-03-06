package com.mobiliya.fleet.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.activity.DashboardActivity;
import com.mobiliya.fleet.activity.TripActivity;
import com.mobiliya.fleet.activity.TripListActivity;
import com.mobiliya.fleet.adapters.ApiCallBackListener;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.Parameter;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.location.GPSTracker;

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
import static com.mobiliya.fleet.utils.DateUtils.tripOngoingFormat;

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
        String vehicle = SharePref.getInstance(activity).getVehicleID();

        String address = null;
        LatLong locations = null;
        Double latitude = 0.0;
        Double longitude = 0.0;
        SharePref pref = SharePref.getInstance(activity.getBaseContext());
        GPSTracker gps = GPSTracker.getInstance(activity.getBaseContext());
        gps.setDistance(0.0f);
        gps.setSpeed(0.0f);
        if (latlong != null) {
            latitude = latlong.latitude;
            longitude = latlong.longitude;
            try {
                locations = new LatLong(String.valueOf(latitude), String.valueOf(longitude));
                address = gps.getAddressFromLatLong(activity.getBaseContext(), locations);
            } catch (Exception ex) {
                Log.d(TAG, "Failed to get address from geocoder");
            }
        }

        if (latitude != 0.0 && longitude != 0.0 && !TextUtils.isEmpty(address) && locations != null) {
            pref.addItem(Constants.FIRST_MILES_ONGOING, -1.0f);
            pref.addItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
            pref.addItem(Constants.SPEEDING, 0);
            pref.addItem(Constants.FUEL_ONGOING, "NA");
            pref.addItem(Constants.TIME_ONGOING, "0");

            Trip newTrip = new Trip();
            UUID uuid = UUID.randomUUID();
            String randomUUIDString = uuid.toString();
            newTrip.commonId = randomUUIDString;
            //newTrip.tripName = "New Trip";
            newTrip.tripName = tripOngoingFormat(getLocalTimeString());
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
            updateLoactionToList(activity, newTrip, locations);
        }
        if (tripId == null) {
            Toast.makeText(activity, activity.getString(R.string.failed_to_start_trip_nogps), Toast.LENGTH_LONG).show();
        }

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

        LogUtil.d(TAG,"stopTrip");
        final ProgressDialog dialog = new ProgressDialog(activity);
        dialog.setIndeterminate(true);
        dialog.setMessage("Stopping Trip....");
        dialog.setCancelable(false);
        dialog.show();
        long recordAffected = -1;
        String address = null;
        LatLong locations = null;

        SharePref pref = SharePref.getInstance(activity.getApplication());
        GPSTracker gps = GPSTracker.getInstance(activity.getBaseContext());
        gps.setDistance(0.0f);
        gps.setSpeed(0.0f);
        Double latitude = 0.0;
        Double longitude = 0.0;
        try {
            GPSTracker gpsTracker = GPSTracker.getInstance(activity);
            latitude = gpsTracker.getLatitude();
            longitude = gpsTracker.getLongitude();
            locations = new LatLong(String.valueOf(latitude), String.valueOf(longitude));
            address = gpsTracker.getAddressFromLatLong(activity.getBaseContext(), locations);
            if(TextUtils.isEmpty(address)) {
                address = SharePref.getInstance(activity.getBaseContext()).getItem(Constants.LAST_ADDRESS, "");
            }
        } catch (Exception ex) {
            Log.d(TAG, "Failed to get address");
        }

        if (latitude != 0.0 && longitude != 0.0 && address != null && locations != null) {
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
                float miles = SharePref.getInstance(activity.getBaseContext()).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                newTrip.milesDriven = String.format("%.2f", miles);
                int mSpeedCount = SharePref.getInstance(activity.getBaseContext()).getItem(Constants.SPEEDING, 0);
                newTrip.IsSynced = false;
                newTrip.speedings = String.valueOf(mSpeedCount);

                pref.addItem(Constants.FIRST_MILES_ONGOING, -1.0f);
                pref.addItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                pref.addItem(Constants.FUEL_ONGOING, "NA");
                pref.addItem(Constants.TIME_ONGOING, "0");
                DatabaseProvider.getInstance(activity.getBaseContext()).deleteLatLong(newTrip.commonId);
                recordAffected = DatabaseProvider.getInstance(activity.getApplicationContext()).updateTrip(newTrip);
                if (recordAffected > 0) {
                    updateLoactionToList(activity, newTrip, locations);
                    showToast(activity, activity.getString(R.string.trip_stopped));
                    pref.addItem(Constants.FIRST_MILES_ONGOING, -1.0f);
                    pref.addItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
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
      /*  if (recordAffected == -1) {
            Toast.makeText(activity, activity.getString(R.string.failed_to_start_trip_nogps), Toast.LENGTH_LONG).show();
            dialog.dismiss();
        }*/
        return recordAffected;
    }

    public static void updateLoactionToList(Context cxt, Trip trp, LatLong latLong) {
        if (trp != null) {
            DatabaseProvider.getInstance(cxt).addLatLong(trp.commonId, latLong);
        }
    }

    public static void updateLocations(Context cxt, Parameter parameter) {
        try {
            Trip trip = DatabaseProvider.getInstance(cxt).getCurrentTrip();
            if (trip != null) {
                TripManagementUtils.updateLoactionToList(cxt, trip, new LatLong(parameter.Latitude, parameter.Longitude));
            }
        } catch (Exception ex) {
            ex.getMessage();
        }
    }

    public static long stopTripFromNotification(Context context) {
        LogUtil.d(TAG,"stopTripFromNotification");
        long recordAffected = -1;
        String address = null;
        LatLong locations = null;
        SharePref pref = SharePref.getInstance(context);

        Double latitude = Double.valueOf(SharePref.getInstance(context).getItem(LATITUDE,"0.0"));
        Double longitude = Double.valueOf(SharePref.getInstance(context).getItem(LONGITUDE,"0.0"));

        try {
            GPSTracker gpsTracker = GPSTracker.getInstance(context);
            locations = new LatLong(String.valueOf(latitude), String.valueOf(longitude));
            //address = gpsTracker.getAddressFromLatLong(context, locations);
            address = pref.getItem(Constants.LAST_ADDRESS,"");
        } catch (Exception ex) {
            Log.d(TAG, "Failed to get address");
        }

        if (latitude != 0.0 && longitude != 0.0 && address != null && locations != null) {

            try {
                Trip newTrip = DatabaseProvider.getInstance(context).getCurrentTrip();
                if (newTrip == null) {
                    LogUtil.d(TAG, "currect trip is null");
                    return -1;
                }
                LogUtil.d(TAG, "currect trip is saved in DB");
                newTrip.endLocation = latitude + "," + longitude + "#" + address;
                newTrip.endTime = getLocalTimeString();
                newTrip.EndDate = getLocalTimeString();
                newTrip.status = TripStatus.Stop.getValue();
                newTrip.stops = DatabaseProvider.getInstance(context).getStopsCount(newTrip.commonId);
                float miles = SharePref.getInstance(context).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                newTrip.milesDriven = String.format("%.2f", miles);
                LogUtil.d(TAG, "currect trip is saved in DB with Mile:"+newTrip.milesDriven);
                newTrip.IsSynced = false;

                newTrip.IsSynced = false;
                DatabaseProvider.getInstance(context).deleteLatLong(newTrip.commonId);
                recordAffected = DatabaseProvider.getInstance(context).updateTrip(newTrip);
                if (recordAffected > 0) {
                    updateLoactionToList(context, newTrip, locations);
                    Toast.makeText(context, context.getString(R.string.trip_stopped), Toast.LENGTH_LONG).show();
                    SharePref.getInstance(context).addItem(Constants.FIRST_MILES_ONGOING, -1.0f);
                    SharePref.getInstance(context).addItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                    SharePref.getInstance(context).addItem(Constants.FUEL_ONGOING, "NA");
                    SharePref.getInstance(context).addItem(Constants.TIME_ONGOING, "0");
                    SharePref.getInstance(context).addItem(Constants.SPEEDING, 0);
                }
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }

        if (recordAffected == -1) {
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
                        LogUtil.d(TAG, "SendLocalTripsToServer onSuccess");
                    }

                    @Override
                    public void onError(VolleyError result) {
                        LogUtil.d(TAG, "SendLocalTripsToServer onError");
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
            jsonBody.put("milesDriven", trip.milesDriven);
            jsonBody.put("speedings", trip.speedings);
            newTripRequestData = CommonUtil.getPostDataString(jsonBody);
            LogUtil.d(TAG, "addTripOnServer() with Json:" + newTripRequestData);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String tenantId = SharePref.getInstance(ctx).getUser().getTenantId();
        String url = String.format(Constants.getTripsURLs(ctx, Constants.ADD_TRIP_URL), tenantId);
        LogUtil.d(TAG,"addTripOnServer() URL : "+url);
        try {
            VolleyCommunicationManager.getInstance().SendRequest(url, Request.Method.POST, newTripRequestData, ctx, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {
                            if (result.getString("message").equals("Success")) {
                                LogUtil.d(TAG, "addTripOnServer() Success");
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
                                    LogUtil.d(TAG, "addTripOnServer() trip status STOP(0), so delete entry from DB");
                                    DatabaseProvider.getInstance(ctx).deleteTrip(trip.commonId);
                                } else if (trip.status == TripStatus.Pause.getValue()) {
                                    trip._id = trip_result._id;
                                    trip.commonId = trip_result.commonId;
                                    trip.IsSynced = true;
                                    LogUtil.d(TAG, "addTripOnServer() trip status PAUSE(2), so update entry from DB");
                                    DatabaseProvider.getInstance(ctx).updateTrip(trip);
                                } else {
                                    trip._id = trip_result._id;
                                    trip.commonId = trip_result.commonId;
                                    trip.IsSynced = true;
                                    LogUtil.d(TAG, "addTripOnServer() trip status START(1), so update entry from DB");
                                    DatabaseProvider.getInstance(ctx).updateTrip(trip);
                                }
                                LogUtil.d(TAG, "addTripOnServer() onSucess()");
                            }else {
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
                        LogUtil.d(TAG, "Error while updating server trip" + result.toString());
                    }
                    LogUtil.d(TAG, "Error while updating server trip");
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
                        final android.support.v7.app.AlertDialog.Builder mDialog = new android.support.v7.app.AlertDialog.Builder(activity);
                        LayoutInflater factory = LayoutInflater.from(activity);
                        View customTitleView = activity.getLayoutInflater().inflate(R.layout.alert_custom_tilte, null);
                        TextView header = (TextView) customTitleView.findViewById(R.id.title_header);
                        header.setText("Dongle disconnected");
                        View dialogView = factory.inflate(R.layout.dongle_disconnected_dialog, null);
                        mDialog.setCustomTitle(customTitleView);
                        mDialog.setView(dialogView);
                        mDialog.setCancelable(false);

                        final TextView pausetrip = (TextView) dialogView.findViewById(R.id.pause);
                        final android.support.v7.app.AlertDialog alertDialog = mDialog.show();

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
                                if (TripManagementUtils.stopTrip(activity) > 0) {

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
