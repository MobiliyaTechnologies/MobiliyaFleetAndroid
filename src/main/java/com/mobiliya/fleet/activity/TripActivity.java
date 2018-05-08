package com.mobiliya.fleet.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.ApiCallBackListener;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.io.AbstractGatewayService;
import com.mobiliya.fleet.io.J1939DongleService;
import com.mobiliya.fleet.io.ObdGatewayService;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.services.GPSTracker;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.DateUtils;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.NotificationManagerUtil;
import com.mobiliya.fleet.utils.SharePref;
import com.mobiliya.fleet.utils.TripManagementUtils;
import com.mobiliya.fleet.utils.TripStatus;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.mobiliya.fleet.utils.CommonUtil.showToast;

@SuppressWarnings({"ALL", "unused"})
public class TripActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = TripActivity.class.getName();
    private MapboxMap mMap;
    private Trip mTrip;
    private TextView mTripDate;
    private TextView mMilesDriven;
    private TextView mTripTime;
    private TextView mFuelUsed;
    private TextView mStops;
    private TextView mPause_tv;
    private TextView mStop_tv;
    public TextView mSpeeding, mHardBraking;
    private LinearLayout mPause, mStop;
    private Trip mLasttrip;
    private SharePref mPref;
    private String mProtocol;
    private Intent mServiceIntent;
    private AbstractGatewayService mService = null;
    private Boolean mIsTripStop = false;
    public PolylineOptions mOptions;
    public Marker mMarkerStart;
    public Marker markerEnd;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();
    MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        bindViews();
        mPref = SharePref.getInstance(this);
        mProtocol = mPref.getItem(Constants.PREF_ADAPTER_PROTOCOL);
        connectToAdapetrService();
        mOptions = new PolylineOptions().width(5).color(getColor(R.color.accent_black));
        Mapbox.getInstance(getApplicationContext(), Constants.MAP_TOKEN);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.NOTIFICATION_PAUSE_BROADCAST);
        filter.addAction(Constants.NOTIFICATION_STOP_BROADCAST);
        registerReceiver(mNotificationReceiver, filter);

        try {
            registerReceiver(mParameterReceiver, new IntentFilter(Constants.LOCAL_RECEIVER_ACTION_NAME));
        } catch (Exception e) {
            LogUtil.i("", "broadcastReceiver is already unregistered");
        }

    }

    public void bindViews() {
        mTripDate = (TextView) findViewById(R.id.tripdate);
        mMilesDriven = (TextView) findViewById(R.id.tv_milesdriven);
        mTripTime = (TextView) findViewById(R.id.tv_triptime);
        mFuelUsed = (TextView) findViewById(R.id.tv_fuelused);
        mStops = (TextView) findViewById(R.id.tv_stops);
        mSpeeding = (TextView) findViewById(R.id.tv_speeding);
        mHardBraking = (TextView) findViewById(R.id.tv_hardbraking);
        LinearLayout mPause = (LinearLayout) findViewById(R.id.btn_pause);
        LinearLayout mStop = (LinearLayout) findViewById(R.id.btn_stop);
        mPause_tv = (TextView) findViewById(R.id.tv_pause);
        TextView mStop_tv = (TextView) findViewById(R.id.tv_stop);

        LinearLayout down_button = (LinearLayout) findViewById(R.id.down_button);
        down_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TripActivity.this, DashboardActivity.class);//The class you want to show
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.slide_in_down, R.anim.slide_out_down);
            }
        });
        mPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPauseCalled();
            }
        });
        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopCalled();
            }
        });
    }

    private void connectToAdapetrService() {
        LogUtil.i(TAG, "connectToAdapetrService");
        if (mProtocol.equals(Constants.J1939)) {
            mServiceIntent = new Intent(this, J1939DongleService.class);
        } else {
            mServiceIntent = new Intent(this, ObdGatewayService.class);
        }
        bindService(mServiceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        if (mService != null) {
            LogUtil.d(TAG, "onDestroy() service disconnected");
            unbindService(mServiceConn);
        }
        try {
            unregisterReceiver(mParameterReceiver);
        } catch (IllegalArgumentException iae) {
            iae.getMessage();
        }
        unregisterReceiver(mNotificationReceiver);
        if (mOptions != null) {
            mOptions = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            LogUtil.i(TAG, "onServiceConnected");
            mService = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        // This method is *only* called when the connection to the service is lost unexpectedly
        // and *not* when the client unbinds (http://developer.android.com/guide/components/bound-services.html)
        // So the isServiceBound attribute should also be set to false when we unbind from the service.
        @Override
        public void onServiceDisconnected(ComponentName className) {
            LogUtil.d(TAG, className.toString() + " service is unbound");
        }
    };

    public void onStopCalled() {
        Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        if (trip != null) {
            showStopDialog();
        } else {
            showToast(TripActivity.this, "No ongoing trip");
        }
    }

    public void onPauseCalled() {
        Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        if (trip != null) {
            if (trip.status == TripStatus.Pause.getValue()) {
                TripManagementUtils.resumeTrip(getBaseContext());
                mPause_tv.setText(getString(R.string.pause));
            } else {
                TripManagementUtils.pauseTrip(TripActivity.this);
                mPause_tv.setText(getString(R.string.paused));
                //calculations of stops
                int count = DatabaseProvider.getInstance(getBaseContext()).getStopsCount(mTrip.commonId);
                mStops.setText(Integer.toString(count));
            }
        } else {
            showToast(TripActivity.this, "No ongoing trip");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsTripStop = false;
        CommonUtil.checkLocationPermission(this);
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(TripActivity.this);
        mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        if (mTrip != null) {
            if (mTrip.status == TripStatus.Pause.getValue()) {
                mPause_tv.setText(getString(R.string.paused));
            }
            if (mTrip.status != TripStatus.Stop.getValue()) {
                mTripDate.setText(mTrip.tripName);
            }
        }
    }

    private void startTrip() {
        String tripId = TripManagementUtils.startTrip(this);
        NotificationManagerUtil.getInstance().createNotification(getBaseContext());
        if (tripId != null) {
            mMap.clear();
            Double lat = GPSTracker.getInstance(getBaseContext()).getLatitude();
            Double lon = GPSTracker.getInstance(getBaseContext()).getLongitude();
            LatLong latlong = new LatLong();
            latlong.latitude = String.valueOf(lat);
            latlong.longitude = String.valueOf(lon);
            mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
            mTripDate.setText(mTrip.tripName);
            DatabaseProvider.getInstance(getBaseContext()).addLatLong(mTrip.commonId, new LatLong(String.valueOf(lat), String.valueOf(lon)));
            plotMarker("start", new LatLong(String.valueOf(lat), String.valueOf(lon)));
            showToast(this, getString(R.string.trip_started));
        }
    }

    @Override
    public void onMapReady(MapboxMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setAttributionEnabled(false);
        mMap.getUiSettings().setLogoEnabled(false);
        Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        if (trip == null) {
            startTrip();
        } else {
            if (trip.status != TripStatus.Stop.getValue()) {
                String fuel_used = SharePref.getInstance(getApplicationContext()).getItem(Constants.FUEL_ONGOING, "NA");
                mFuelUsed.setText(fuel_used);
                int count = DatabaseProvider.getInstance(getBaseContext()).getStopsCount(mTrip.commonId);
                mStops.setText(Integer.toString(count));
                float milage = SharePref.getInstance(getApplicationContext()).getItem(Constants.MILES_ONGOING, 0.0f);
                mMilesDriven.setText("" + milage + " miles");
                String diff = SharePref.getInstance(getBaseContext()).getItem(Constants.TIME_ONGOING, "0");
                mTripTime.setText(diff);
                mMap.clear();
                LatLong latlong = DatabaseProvider.getInstance(getBaseContext()).getLatLongList(mTrip.commonId).get(0);
                plotMarker("start", latlong);
                List<LatLong> list = DatabaseProvider.getInstance(getBaseContext()).getLatLongList(mTrip.commonId);
                if (list != null) {
                    if (mOptions == null) return;
                    mOptions.getPoints().clear();
                    if (list.size() > 0) {
                        for (LatLong point : list) {
                            mOptions.add(new LatLng(Double.valueOf(point.latitude), Double.valueOf(point.longitude)));
                        }
                        redrawLine();
                    }
                }
            }
        }
    }

    private void redrawLine() {
        if (mOptions.getPoints().size() > 1) {
            List<LatLng> points_list = mOptions.getPoints();
            LatLng points = points_list.get(points_list.size() - 1);
            LatLong end_location = new LatLong(String.valueOf(points.getLatitude()), String.valueOf(points.getLongitude()));
            if (end_location != null) {
                plotMarker("end", end_location);
            }
        }
        Polyline mLine = mMap.addPolyline(mOptions);
    }

    private void plotMarker(String icon, LatLong latLong) {
        try {
            if (Double.parseDouble(latLong.longitude) == 0 && Double.parseDouble(latLong.longitude) == 0) {
                GPSTracker gpsTracker = GPSTracker.getInstance(getApplicationContext());
                if (gpsTracker.getLatitude() != 0 && gpsTracker.getLongitude() != 0) {
                    latLong.longitude = String.valueOf(gpsTracker.getLongitude());
                    latLong.latitude = String.valueOf(gpsTracker.getLatitude());
                } else {
                    gpsTracker.getLocation();
                    if (gpsTracker.getLatitude() != 0 && gpsTracker.getLongitude() != 0) {
                        latLong.longitude = String.valueOf(gpsTracker.getLongitude());
                        latLong.latitude = String.valueOf(gpsTracker.getLatitude());
                    } else
                        return;
                }
            }
            LatLng position = new LatLng(Double.parseDouble(latLong.latitude), Double.parseDouble(latLong.longitude));
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.ENGLISH);
            String AddressStr = null;
            List<Address> addresses = null;

            if (latLong != null && latLong.latitude != null && latLong.longitude != null) {

                addresses = geocoder.getFromLocation(Double.parseDouble(latLong.latitude), Double.parseDouble(latLong.longitude), 1);
                if (addresses != null && addresses.size() > 0) {
                    Address address = addresses.get(0);
                    AddressStr = address.getAddressLine(0);
                }

                int customIcon = 0;
                if ("end".equals(icon)) {
                    customIcon = R.drawable.stop_blue;
                    if (markerEnd != null) {
                        markerEnd.remove();
                    }
                    Icon icon_img = IconFactory.getInstance(TripActivity.this).fromResource(customIcon);
                    markerEnd = mMap.addMarker(new MarkerOptions().position(position).icon(icon_img).title(AddressStr));
                    markerEnd.setPosition(position);
                } else if ("start".equals(icon)) {
                    customIcon = R.drawable.startlocation;
                    Icon icon_img = IconFactory.getInstance(TripActivity.this).fromResource(customIcon);
                    mMarkerStart = mMap.addMarker(new MarkerOptions().position(position).icon(icon_img).title(AddressStr));
                    mMarkerStart.setPosition(position);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngZoom(position, 15);
                mMap.moveCamera(cameraPosition);
                mMap.animateCamera(cameraPosition);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateLoactionToList(LatLong latLong) {
        DatabaseProvider.getInstance(getBaseContext()).addLatLong(mTrip.commonId, latLong);
    }

    public void showStopDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(getString(R.string.stop_trip_dialog));
        alert.setMessage(getString(R.string.do_want_to_stop_trip));
        alert.setCancelable(false);
        alert.setNegativeButton(getString(R.string.no), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alert.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                mIsTripStop = true;
                dialog.dismiss();
                final ProgressDialog dialogProgress = new ProgressDialog(TripActivity.this);
                dialogProgress.setIndeterminate(true);
                dialogProgress.setCancelable(false);
                dialogProgress.setMessage("Loading trip details, please wait...");
                dialogProgress.show();

                Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
                long record_affected = TripManagementUtils.stopTrip(TripActivity.this);
                NotificationManagerUtil.getInstance().dismissNotification(getBaseContext());
                List<Trip> newTripList = DatabaseProvider.getInstance(getBaseContext()).getLastUnSyncedTrip();
                if (newTripList != null && newTripList.size() > 0) {
                    for (final Trip trips : newTripList) {
                        if (CommonUtil.isNetworkConnected(getBaseContext())) {
                            TripManagementUtils.addTripOnServer(getBaseContext(), trips, new ApiCallBackListener() {
                                        @Override
                                        public void onSuccess(JSONObject object) {
                                            if (object != null) {
                                                try {
                                                    if (object.getString("message").equals("Success")) {
                                                        LogUtil.d(TAG, "addTripOnServer() sucess");
                                                        String data = object.getString("data");
                                                        Type type = new TypeToken<Trip>() {
                                                        }.getType();
                                                        Gson gson = new Gson();
                                                        Trip trip_result = null;

                                                        try {
                                                            trip_result = gson.fromJson(data, type);
                                                        } catch (Exception ex) {
                                                            ex.getMessage();
                                                        }

                                                        String commonId = trip_result.commonId;
                                                        Intent intent = new Intent(TripActivity.this, TripDetailsActivity.class);
                                                        intent.putExtra(Constants.TRIPID, commonId);
                                                        startActivity(intent);
                                                        finish();
                                                    }
                                                } catch (Exception ex) {
                                                }
                                            }
                                            dialogProgress.dismiss();
                                        }

                                        @Override
                                        public void onError(VolleyError result) {
                                            if (result.networkResponse != null && result.networkResponse.statusCode == 401) {
                                                showToast(TripActivity.this, getString(R.string.aunthentication_error));
                                            } else {
                                                showToast(TripActivity.this, getString(R.string.error_occured));
                                            }
                                            finish();
                                            dialogProgress.dismiss();

                                        }
                                    }
                            );
                        } else {
                            finish();
                            dialogProgress.dismiss();
                        }
                    }
                } else {
                    dialogProgress.dismiss();
                    finish();
                }
            }
        });
        AlertDialog dialog = alert.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getColor(R.color.dashboard_header_start_color));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getColor(R.color.dashboard_header_start_color));
    }

    private BroadcastReceiver mParameterReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String TAG = "TripActivity";
            LogUtil.d(TAG, "onReceive() parameter received");
            if (mIsTripStop) {
                LogUtil.d(TAG, "onReceive() parameter received Return trip already stop dont update parameter");
                return;
            }
            @SuppressWarnings("unchecked") HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra(Constants.LOCAL_RECEIVER_NAME);

            String totalHours = hashMap.get("TotalHours");
            String fuelUsed = hashMap.get("FuelUsed");
            String distance = hashMap.get("Distance");
            String speed = hashMap.get("Speedcount");
            totalHours = "0".equalsIgnoreCase(totalHours) ? "NA" : totalHours;
            fuelUsed = "0".equalsIgnoreCase(fuelUsed) ? "NA" : fuelUsed;
            distance = "0".equalsIgnoreCase(distance) ? "NA" : distance;
            speed = "0".equalsIgnoreCase(speed) ? "0" : speed;
            setParameterData(totalHours, fuelUsed, distance, speed);
            if (hashMap.get("latitude") != null && hashMap.get("longitude") != null) {
                LatLong latLong = new LatLong(hashMap.get("latitude"), hashMap.get("longitude"));
                updateLoactionToList(latLong);
                LatLng point = new LatLng(Double.valueOf(latLong.latitude), Double.valueOf(latLong.longitude));
                mOptions.add(point);
                redrawLine();
            }
        }
    };

    private void setParameterData(String totalHours, String fuelUsed, String distance, String speeding) {
        //calculation for trip time
        if (mTrip != null) {
            Date start = DateUtils.getDateFromString(mTrip.startTime);
            String diff = DateUtils.getTimeDifference(start);

            SharePref.getInstance(getBaseContext()).addItem(Constants.TIME_ONGOING, diff);
            mTripTime.setText(diff);
            NotificationManagerUtil.getInstance().upDateNotification(getBaseContext(), diff);
        }

        if (!TextUtils.isEmpty(distance)) {
            LogUtil.d(TAG, "distance:" + distance);
            if (!"NA".equalsIgnoreCase(distance)) {
                float distanceValue = Float.parseFloat(distance);
                if (distanceValue <= 0) {
                    LogUtil.d(TAG, "distance value is less then 0");
                    return;
                }
            }
            //calculation for miles driven
            float firstmilage = SharePref.getInstance(getApplicationContext()).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
            LogUtil.d(TAG, "firstmilage:" + firstmilage);
            if (firstmilage == 0.0f) {
                if (!"NA".equalsIgnoreCase(distance)) {
                    firstmilage = Float.parseFloat(distance);
                    LogUtil.d(TAG, "First milage reading :" + firstmilage);
                    SharePref.getInstance(getApplicationContext()).addItem(Constants.TOTAL_MILES_ONGOING, firstmilage);
                } else {
                    if (mMilesDriven != null) {
                        mMilesDriven.setText("0 miles");
                    }
                }
            } else {
                if (!"NA".equalsIgnoreCase(distance)) {
                    float milesdriven = 0.0f;
                    milesdriven = Float.parseFloat(distance) - firstmilage;
                    if (milesdriven <= 0) {
                        LogUtil.d(TAG, "Miles driven less then 0");
                        if (mMilesDriven != null) {
                            mMilesDriven.setText("0 miles");
                        }
                        return;
                    }
                    int dist = (int) Math.round(milesdriven);
                    LogUtil.d(TAG, "Distance travel :" + dist);
                    SharePref.getInstance(getApplicationContext()).addItem(Constants.MILES_ONGOING, dist);
                    if (mMilesDriven != null) {
                        mMilesDriven.setText(dist + " miles");
                    }
                } else {
                    float miles = SharePref.getInstance(getApplicationContext()).getItem(Constants.MILES_ONGOING, 0.0f);
                    LogUtil.d(TAG, "Distance travel :" + miles);
                    if (mMilesDriven != null) {
                        mMilesDriven.setText(miles + " miles");
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(fuelUsed)) {
            if (mFuelUsed != null) {
                mFuelUsed.setText(fuelUsed);
            }
            SharePref.getInstance(getApplicationContext()).addItem(Constants.FUEL_ONGOING, fuelUsed);
        }
        if (!TextUtils.isEmpty(speeding)) {
            mSpeeding.setText(speeding + "");
        }
    }

    private BroadcastReceiver mNotificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.NOTIFICATION_PAUSE_BROADCAST)) {
                mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
                if (mTrip.status == TripStatus.Pause.getValue()) {
                    mPause_tv.setText(getString(R.string.paused));
                    int count = DatabaseProvider.getInstance(getBaseContext()).getStopsCount(mTrip.commonId);
                    mStops.setText(Integer.toString(count));
                }
            }
            if (intent.getAction().equals(Constants.NOTIFICATION_STOP_BROADCAST)) {
                unregisterReceiver(mParameterReceiver);
            }
        }
    };

}




