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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.ApiCallBackListener;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.io.AbstractGatewayService;
import com.mobiliya.fleet.io.J1939DongleService;
import com.mobiliya.fleet.io.ObdGatewayService;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.location.GPSTracker;
import com.mobiliya.fleet.location.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.NotificationManagerUtil;
import com.mobiliya.fleet.utils.SharePref;
import com.mobiliya.fleet.utils.TripManagementUtils;
import com.mobiliya.fleet.utils.TripStatus;

import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.mobiliya.fleet.utils.CommonUtil.getTimeDiff;
import static com.mobiliya.fleet.utils.CommonUtil.showToast;

public class TripActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final String TAG = TripActivity.class.getName();
    private GoogleMap mMap;
    private Trip mTrip;
    private TextView mTripDate;
    private TextView mMilesDriven;
    private TextView mTripTime;
    private TextView mFuelUsed;
    private TextView mStops;
    public static TextView mPause_tv;
    public TextView mSpeeding, mEngineRPM;
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
    public static ImageView mPauseIcon;
    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        gps = GPSTracker.getInstance(getBaseContext());
        bindViews();
        mPref = SharePref.getInstance(this);
        mProtocol = mPref.getItem(Constants.PREF_ADAPTER_PROTOCOL);
        connectToAdapetrService();

        mOptions = new PolylineOptions().width(5).color(getColor(R.color.accent_black)).geodesic(true);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.NOTIFICATION_PAUSE_BROADCAST);
        filter.addAction(Constants.NOTIFICATION_STOP_BROADCAST);
        registerReceiver(mNotificationReceiver, filter);
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(TripActivity.this);
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
        mEngineRPM = (TextView) findViewById(R.id.tv_engine_rpm);
        LinearLayout mPause = (LinearLayout) findViewById(R.id.btn_pause);
        LinearLayout mStop = (LinearLayout) findViewById(R.id.btn_stop);
        mPause_tv = (TextView) findViewById(R.id.tv_pause);
        mPauseIcon = (ImageView) findViewById(R.id.pause_icon);
        TextView mStop_tv = (TextView) findViewById(R.id.tv_stop);

        LinearLayout down_button = (LinearLayout) findViewById(R.id.down_button);
        down_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent(TripActivity.this, DashboardActivity.class);//The class you want to show
                //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                //startActivity(intent);
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
        if (mapView != null)
            mapView.onDestroy();
        if (mService != null) {
            LogUtil.d(TAG, "onDestroy() called");
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
        LogUtil.d(TAG,"onPause called");
        if (mapView != null) {
            mapView.onPause();
        }
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null)
            mapView.onLowMemory();


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null)
            mapView.onSaveInstanceState(outState);
    }

    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            LogUtil.d(TAG, "onServiceConnected");
            mService = ((AbstractGatewayService.AbstractGatewayServiceBinder) binder).getService();
            mService.startTimer();
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
        LogUtil.d(TAG,"onStopCalled");
        Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        if (trip != null) {
            showStopDialog();
        } else {
            showToast(TripActivity.this, "No ongoing trip");
        }
    }

    public void onPauseCalled() {
        LogUtil.d(TAG,"onPauseCalled");
        Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        if (trip != null) {
            if (trip.status == TripStatus.Pause.getValue()) {
                TripManagementUtils.resumeTrip(getBaseContext());
                mPause_tv.setText(getString(R.string.pause));
                mPauseIcon.setImageDrawable(getDrawable(R.drawable.pause));

            } else {
                TripManagementUtils.pauseTrip(TripActivity.this);
                mPause_tv.setText(getString(R.string.paused));
                mPauseIcon.setImageDrawable(getDrawable(R.drawable.play_icon));
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
        LogUtil.d(TAG,"onResume");
        if (mapView != null) {
            mapView.onResume();
        }
        mIsTripStop = false;
        CommonUtil.checkLocationPermission(this);
        mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        if (mTrip != null) {
            if (mTrip.status == TripStatus.Pause.getValue()) {
                mPause_tv.setText(getString(R.string.paused));
                mPauseIcon.setImageDrawable(getDrawable(R.drawable.play_icon));
            } else if (mTrip.status != TripStatus.Stop.getValue()) {
                mTripDate.setText(mTrip.tripName);
                mPauseIcon.setImageDrawable(getDrawable(R.drawable.pause));
            }
        }
    }

    private void startTrip() {
        gps.getLocation();
        if (gps.getLatitude() == 0.0d || gps.getLongitude() == 0.0d) {
            showToast(this, "Failed to get your location,Please try again");
            TripActivity.this.finish();
        } else {
            LatLng latlongitude = new LatLng(gps.getLatitude(), gps.getLongitude());
            saveStartTrip(latlongitude);
        }
    }


    public void saveStartTrip(LatLng latlongitude) {
        String tripId = TripManagementUtils.startTrip(this, latlongitude);
        if (tripId != null) {
            NotificationManagerUtil.getInstance().createNotification(getBaseContext());
            mMap.clear();
            mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
            mTripDate.setText("Trip Name :" + mTrip.tripName);
            if (latlongitude.latitude != 0.0 && latlongitude.longitude != 0.0) {
                DatabaseProvider.getInstance(getBaseContext()).addLatLong(mTrip.commonId, new LatLong(String.valueOf(latlongitude.latitude), String.valueOf(latlongitude.longitude)));
                plotMarker("start", new LatLong(String.valueOf(latlongitude.latitude), String.valueOf(latlongitude.longitude)));
            }
            showToast(this, getString(R.string.trip_started));
        } else {
            TripActivity.this.finish();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        if (trip == null) {
            startTrip();
        } else {
            if (trip.status != TripStatus.Stop.getValue()) {
                mTripDate.setText("Trip Name :" + trip.tripName);
                String fuel_used = SharePref.getInstance(getApplicationContext()).getItem(Constants.FUEL_ONGOING, "NA");
                mFuelUsed.setText(fuel_used);
                int count = DatabaseProvider.getInstance(getBaseContext()).getStopsCount(mTrip.commonId);
                mStops.setText(Integer.toString(count));
                float milage = SharePref.getInstance(getApplicationContext()).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
                mMilesDriven.setText("" + String.format("%.2f", milage) + " miles");
                String diff = SharePref.getInstance(getBaseContext()).getItem(Constants.TIME_ONGOING, "0");
                mTripTime.setText(diff);
                mMap.clear();
                LatLong latlong = DatabaseProvider.getInstance(getBaseContext()).getLatLongList(mTrip.commonId).get(0);
                plotMarker("start", latlong);
                List<LatLong> list = DatabaseProvider.getInstance(getBaseContext()).getLatLongList(mTrip.commonId);
                if (list != null && list.size() > 0) {
                    if (mOptions == null) return;
                    mOptions.getPoints().clear();

                    for (LatLong point : list) {
                        mOptions.add(new LatLng(Double.valueOf(point.latitude), Double.valueOf(point.longitude)));
                    }
                    redrawLine();

                }
            }
        }
    }

    private void redrawLine() {
        if (mMap == null || mOptions == null) {
            return;
        }
        if (mOptions.getPoints().size() > 1) {
            List<LatLng> points_list = mOptions.getPoints();
            LatLng points = points_list.get(points_list.size() - 1);
            LatLong end_location = new LatLong(String.valueOf(points.latitude), String.valueOf(points.longitude));
            if (end_location != null) {
                plotMarker("end", end_location);
            }
        }
        Polyline mLine = mMap.addPolyline(mOptions);
    }

    private void plotMarker(String icon, LatLong latLong) {
        SharePref pref = SharePref.getInstance(this);
        try {
            if (Double.parseDouble(latLong.longitude) == 0 && Double.parseDouble(latLong.longitude) == 0) {
                if (gps.getLatitude() != 0 && gps.getLongitude() != 0) {
                    latLong.longitude = String.valueOf(gps.getLongitude());
                    latLong.latitude = String.valueOf(gps.getLatitude());
                } else {
                    gps.getLocation();
                    if (gps.getLatitude() != 0 && gps.getLongitude() != 0) {
                        latLong.longitude = String.valueOf(gps.getLongitude());
                        latLong.latitude = String.valueOf(gps.getLatitude());
                    } else
                        return;
                }
            }
            LatLng position = new LatLng(Double.parseDouble(latLong.latitude), Double.parseDouble(latLong.longitude));
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.ENGLISH);
            String AddressStr = null;
            List<Address> addresses = null;

            if (latLong != null && latLong.latitude != null && latLong.longitude != null) {
                int customIcon = 0;
                if ("end".equals(icon)) {
                    customIcon = R.drawable.stop_blue;
                    if (markerEnd != null) {
                        markerEnd.remove();
                    }
                    markerEnd = mMap.addMarker(new MarkerOptions().position(position).icon(BitmapDescriptorFactory.fromResource(customIcon)).title(AddressStr));
                    markerEnd.setPosition(position);
                } else if ("start".equals(icon)) {
                    customIcon = R.drawable.startlocation;
                    mMarkerStart = mMap.addMarker(new MarkerOptions().position(position).icon(BitmapDescriptorFactory.fromResource(customIcon)).title(AddressStr));
                    mMarkerStart.setPosition(position);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngZoom(position, 15);
                mMap.moveCamera(cameraPosition);
                mMap.animateCamera(cameraPosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void showStopDialog() {
        LogUtil.d(TAG,"showStopDialog");
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
                try{
                //Trip trip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
                long record_affected = TripManagementUtils.stopTrip(TripActivity.this);
                if (record_affected > 0) {
                    NotificationManagerUtil.getInstance().dismissNotification(getBaseContext());
                    List<Trip> newTripList = DatabaseProvider.getInstance(getBaseContext()).getLastUnSyncedTrip();
                    if (newTripList != null && newTripList.size() > 0) {
                        for (final Trip trips : newTripList) {
                            if (CommonUtil.isNetworkConnected(getBaseContext())) {
                                TripManagementUtils.addTripOnServer(TripActivity.this, trips, new ApiCallBackListener() {
                                            @Override
                                            public void onSuccess(JSONObject object) {
                                                if (object != null) {
                                                    try {
                                                        if (object.getString("message").equals("Success")) {
                                                            String data = object.getString("data");
                                                            LogUtil.d(TAG, "addTripOnServer() sucess DATA:" + data);
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
                                                        LogUtil.d(TAG, "Exception addTripOnServer() ");
                                                        ex.printStackTrace();
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
                } else {
                    dialogProgress.dismiss();
                    finish();
                }
            }catch (Exception e){
                    e.printStackTrace();
                    LogUtil.d(TAG,"error in trips");
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
            LogUtil.d(TAG, "onReceive() parameter received");
            if (mIsTripStop) {
                LogUtil.d(TAG, "onReceive() parameter received Return trip already stop dont update parameter");
                return;
            }
            @SuppressWarnings("unchecked") HashMap<String, String> hashMap = (HashMap<String, String>) intent.getSerializableExtra(Constants.LOCAL_RECEIVER_NAME);

            String totalHours = hashMap.get("TotalHours");
            String fuelUsed = hashMap.get("FuelUsed");
            String speed = hashMap.get("Speedcount");
            String rpm = hashMap.get("RPM");
            totalHours = "0".equalsIgnoreCase(totalHours) ? "NA" : totalHours;
            fuelUsed = "0".equalsIgnoreCase(fuelUsed) ? "NA" : fuelUsed;
            if (mEngineRPM != null) {
                mEngineRPM.setText(rpm);
            }
            setParameterData(totalHours, fuelUsed, speed);
            if (hashMap.get("Latitude") != null && hashMap.get("Longitude") != null) {
                LatLong latLong = new LatLong(hashMap.get("Latitude"), hashMap.get("Longitude"));
                Double lat = Double.valueOf(latLong.latitude);
                Double longi = Double.valueOf(latLong.longitude);

                if (mTrip != null && lat != 0.0d && longi != 0.0d) {
                    LatLng point = new LatLng(Double.valueOf(latLong.latitude), Double.valueOf(latLong.longitude));
                    mOptions.add(point);
                }
                redrawLine();
            }
        }
    };

    private void setParameterData(String totalHours, String fuelUsed, String speeding) {
        //calculation for trip time
        if (mTrip != null) {
            String diff = getTimeDiff(getBaseContext(), mTrip);
            mTripTime.setText(diff);
        }

        if (mMilesDriven != null) {
            float milage = SharePref.getInstance(getApplicationContext()).getItem(Constants.TOTAL_MILES_ONGOING, 0.0f);
            mMilesDriven.setText("" + String.format("%.2f", milage) + " miles");
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

            LogUtil.d(TAG,"mNotificationReceiver called");
            if (intent.getAction().equals(Constants.NOTIFICATION_PAUSE_BROADCAST)) {
                mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
                if (mTrip.status == TripStatus.Pause.getValue()) {
                    mPause_tv.setText(getString(R.string.paused));
                    mPauseIcon.setImageDrawable(getDrawable(R.drawable.play_icon));
                    int count = DatabaseProvider.getInstance(getBaseContext()).getStopsCount(mTrip.commonId);
                    mStops.setText(Integer.toString(count));
                }
            }
            if (intent.getAction().equals(Constants.NOTIFICATION_STOP_BROADCAST)) {
                try {
                    LogUtil.d(TAG,"mNotificationReceiver called NOTIFICATION_STOP_BROADCAST");
                    unregisterReceiver(mParameterReceiver);
                    finish();
                } catch (IllegalArgumentException iae) {
                    iae.getMessage();
                }
            }
        }
    };
}




