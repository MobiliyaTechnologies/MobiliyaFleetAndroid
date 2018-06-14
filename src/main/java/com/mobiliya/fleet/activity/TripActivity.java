package com.mobiliya.fleet.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
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
import com.mobiliya.fleet.location.LocationInfo;
import com.mobiliya.fleet.location.LocationTracker;
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
import java.util.Timer;
import java.util.TimerTask;

import static com.mobiliya.fleet.utils.CommonUtil.getTimeDiff;
import static com.mobiliya.fleet.utils.CommonUtil.showToast;

@SuppressWarnings({"ALL", "unused"})
public class TripActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = TripActivity.class.getName();
    private MapboxMap mMap;
    private Trip mTrip;
    private TextView mTripDate;
    private TextView mMilesDriven;
    private TextView mTripTime;
    private TextView mFuelUsed;
    private TextView mStops;
    public static TextView mPause_tv;
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
    public static ImageView mPauseIcon;
    private Geocoder mGeocoder;
    private List<Address> mAddresses;
    private Location mylocation;
    private GoogleApiClient googleApiClient;
    private final static int REQUEST_CHECK_SETTINGS_GPS = 0x1;
    private final static int REQUEST_ID_MULTIPLE_PERMISSIONS = 0x2;
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 1001;
    LatLng mLatLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);
        bindViews();
        mPref = SharePref.getInstance(this);
        setUpGClient();
        mGeocoder = new Geocoder(this, Locale.getDefault());
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
        mHardBraking = (TextView) findViewById(R.id.tv_hardbraking);
        LinearLayout mPause = (LinearLayout) findViewById(R.id.btn_pause);
        LinearLayout mStop = (LinearLayout) findViewById(R.id.btn_stop);
        mPause_tv = (TextView) findViewById(R.id.tv_pause);
        mPauseIcon = (ImageView) findViewById(R.id.pause_icon);
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
        /*if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }*/
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
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
        mIsTripStop = false;
        CommonUtil.checkLocationPermission(this);
        mTrip = DatabaseProvider.getInstance(getBaseContext()).getCurrentTrip();
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        if (mTrip != null) {
            //initDataSyncTimer();
            if (mTrip.status == TripStatus.Pause.getValue()) {
                mPause_tv.setText(getString(R.string.paused));
                mPauseIcon.setImageDrawable(getDrawable(R.drawable.play_icon));
            } else if (mTrip.status != TripStatus.Stop.getValue()) {
                mTripDate.setText(mTrip.tripName);
                mPauseIcon.setImageDrawable(getDrawable(R.drawable.pause));
            }
        }
    }

    private Timer mTimer;

    /*method to initialize timer task*/
    private void initDataSyncTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
            mTimer = new Timer();
        } else {
            mTimer = new Timer();
        }

        int delayTime = 1;

        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LogUtil.i(TAG, "10 sec timer callback callled ->" + 10);
                performAction();
            }
        }, 0, (10 * 1000));
    }

    private void performAction() {
        String TAG = "TripActivity";
        LogUtil.d(TAG, "performAction parameter received");
        if (mIsTripStop) {
            LogUtil.d(TAG, "performAction() parameter received Return trip already stop dont update parameter");
            return;
        }
        SharePref pref = SharePref.getInstance(this);

        String totalHours = pref.getItem("TotalHours", "NA");
        String fuelUsed = pref.getItem("FuelUsed", "NA");
        String distance = pref.getItem(Constants.TIMER_DISTANCE, "NA");
        String speed = pref.getItem(Constants.SPEED_COUNT, "0");
        String latitude = pref.getItem(Constants.TIMER_LATITUDE);
        String longitude = pref.getItem(Constants.TIMER_LONGITUDE);

        final String totalH = "0".equalsIgnoreCase(totalHours) ? "NA" : totalHours;
        final String fuelU = "0".equalsIgnoreCase(fuelUsed) ? "NA" : fuelUsed;
        final String dist = "0".equalsIgnoreCase(distance) ? "NA" : distance;
        final String vehicleSpeed = "0".equalsIgnoreCase(speed) ? "0" : speed;
        final String lat = latitude;
        final String lon = longitude;
        runOnUiThread(new Thread(new Runnable() {
            public void run() {
                setParameterData(totalH, fuelU, dist, vehicleSpeed);
                if (lat != null && lon != null) {
                    LatLong latLong = new LatLong(lat, lon);
                    Double lat = Double.valueOf(latLong.latitude);
                    Double longi = Double.valueOf(latLong.longitude);

                    if (mTrip != null && lat != 0.0d && longi != 0.0d) {
                        LatLng point = new LatLng(Double.valueOf(latLong.latitude), Double.valueOf(latLong.longitude));
                        mOptions.add(point);
                    }
                    redrawLine();
                }
            }
        }));
    }

    private void startTrip() {
        GPSTracker gps = GPSTracker.getInstance(getBaseContext());
        if (gps.getLatitude() == 0.0d || gps.getLongitude() == 0.0d) {
            checkPermissions();
            getMyLocation();
        } else {
            LatLng latlongitude = new LatLng(gps.getLatitude(), gps.getLongitude());
            saveStartTrip(latlongitude);
        }
        //initDataSyncTimer();
        //LatLng latlongitude = new LatLng(gps.getLatitude(), gps.getLongitude());
        //saveStartTrip(latlongitude);
    }


    public void saveStartTrip(LatLng latlongitude) {
        String tripId = TripManagementUtils.startTrip(this, latlongitude);
        if (tripId != null) {
            NotificationManagerUtil.getInstance().createNotification(getBaseContext());
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
        } else {
            TripActivity.this.finish();
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
                mMilesDriven.setText("" + String.format("%.1f", milage) + " miles");
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
            LatLong end_location = new LatLong(String.valueOf(points.getLatitude()), String.valueOf(points.getLongitude()));
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

                /*addresses = geocoder.getFromLocation(Double.parseDouble(latLong.latitude), Double.parseDouble(latLong.longitude), 1);
                if (addresses != null && addresses.size() > 0) {
                    Address address = addresses.get(0);
                    AddressStr = address.getAddressLine(0);
                }*/

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
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                    Double lat=Double.valueOf(latLong.latitude);
                    Double longi=Double.valueOf(latLong.longitude);

                    if (mTrip != null&&lat!=0.0d&&longi!=0.0d) {
                        LatLng point = new LatLng(Double.valueOf(latLong.latitude), Double.valueOf(latLong.longitude));
                        mOptions.add(point);
                    }
                    redrawLine();
                }
            }
        };

    private void setParameterData(String totalHours, String fuelUsed, String distance, String speeding) {
        //calculation for trip time
        if (mTrip != null) {
            String diff = getTimeDiff(getBaseContext(), mTrip);
            mTripTime.setText(diff);
        }

        if (mMilesDriven != null && !TextUtils.isEmpty(distance)) {
            float miles = CommonUtil.milesDriven(this, distance);
            if (miles < 0) {
                mMilesDriven.setText("0.0 miles");
            } else {
                mMilesDriven.setText(String.format("%.1f", miles) + " miles");
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
                    mPauseIcon.setImageDrawable(getDrawable(R.drawable.play_icon));
                    int count = DatabaseProvider.getInstance(getBaseContext()).getStopsCount(mTrip.commonId);
                    mStops.setText(Integer.toString(count));
                }
            }
            if (intent.getAction().equals(Constants.NOTIFICATION_STOP_BROADCAST)) {
                try {
                    unregisterReceiver(mParameterReceiver);
                } catch (IllegalArgumentException iae) {
                    iae.getMessage();
                }
            }
        }
    };


    private synchronized void setUpGClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, 0, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }


    private void getMyLocation() {
        if (googleApiClient != null) {
            if (googleApiClient.isConnected()) {
                int permissionLocation = ContextCompat.checkSelfPermission(TripActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION);
                if (permissionLocation == PackageManager.PERMISSION_GRANTED) {
                    mylocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                    LocationRequest locationRequest = new LocationRequest();
                    locationRequest.setInterval(3000);
                    locationRequest.setFastestInterval(3000);
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest);
                    builder.setAlwaysShow(true);
                    LocationServices.FusedLocationApi
                            .requestLocationUpdates(googleApiClient, locationRequest, this);
                    PendingResult<LocationSettingsResult> result =
                            LocationServices.SettingsApi
                                    .checkLocationSettings(googleApiClient, builder.build());
                    result.setResultCallback(new ResultCallback<LocationSettingsResult>() {

                        @Override
                        public void onResult(LocationSettingsResult result) {
                            final Status status = result.getStatus();
                            switch (status.getStatusCode()) {
                                case LocationSettingsStatusCodes.SUCCESS:
                                    // All location settings are satisfied.
                                    // You can initialize location requests here.
                                    getLocation();
                                    break;
                                case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                    // Location settings are not satisfied.
                                    // But could be fixed by showing the user a dialog.
                                    try {
                                        // Show the dialog by calling startResolutionForResult(),
                                        // and check the result in onActivityResult().
                                        // Ask to turn on GPS automatically
                                        status.startResolutionForResult(TripActivity.this,
                                                REQUEST_CHECK_SETTINGS_GPS);
                                    } catch (IntentSender.SendIntentException e) {
                                        // Ignore the error.
                                    }
                                    break;
                                case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                    break;
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * This API returns the current location requested.
     */
    public void getLocation() {
        LogUtil.d(TAG, "Into getLocation() method");
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Starting trip, please wait....");
        dialog.setCancelable(false);
        dialog.show();

        (new Thread() {
            public void run() {
                Looper.prepare();
                try {
                    final Handler mHandler = new Handler() {
                        @Override
                        public void handleMessage(final Message msg) {
                            LogUtil.d(TAG, "Into handleMessage()");
                            if (msg != null && msg.what == 0) {
                                final HashMap<String, Object> hMap = new HashMap<>();

                                final LocationInfo location = (LocationInfo) msg.obj;
                                if (location != null) {
                                    try {
                                        LogUtil.d(TAG, "location object ");

                                        if (location.getLatitude() != 0.0d && location.getLongitude() != 0.0d) {
                                            mLatLong = new LatLng(location.getLatitude(), location.getLongitude());
                                        }

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    } finally {

                                    }
                                }
                                TripActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        saveStartTrip(mLatLong);
                                        dialog.dismiss();
                                    }
                                });

                            }
                        }
                    };

                    // Get the device location.
                    LocationTracker.getInstance(TripActivity.this).getLocation(mHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Looper.loop();
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        LogUtil.d(TAG, "location permission granted");
                        //getMyLocation();
                    }

                } else {
                    LogUtil.d(TAG, "permission denied");
                }
            }
        }
    }

    private void checkPermissions() {
        LogUtil.d(TAG, "checkPermissions()");
        int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
        permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
        permissionCheck += this.checkSelfPermission("android.permission-group.CONTACTS");
        permissionCheck += this.checkSelfPermission("android.permission.WRITE_CONTACTS");
        permissionCheck += this.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
        if (permissionCheck != 0) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission_group.CONTACTS, Manifest.permission.BLUETOOTH_PRIVILEGED}, MY_PERMISSIONS_REQUEST_LOCATION); //Any number
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}




