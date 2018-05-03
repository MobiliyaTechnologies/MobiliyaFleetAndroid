package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.models.LatLong;
import com.mobiliya.fleet.models.TripDetailModel;
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.services.GPSTracker;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.DateUtils;
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressWarnings({"ALL", "unused"})
public class TripDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private String TAG = TripDetailsActivity.class.getSimpleName();
    TextView mStartLocation, mEndLocation, mStartTime, mEndTime;
    TextView mMilesDriven, mTriptime, mFuelused, mAveragespeed, mTopspeed, mMilage;
    TextView mStops, mSpeeding, mHardnraking, mEnginefaults, mAccelerator, mPhoneUsage;
    private GoogleMap mMap;
    private List<Marker> mMarkerList = new ArrayList<>();
    String mTripId;
    private LinearLayout mBackButton;
    private TextView mTripName;
    LatLngBounds.Builder mBoundsBuilder = new LatLngBounds.Builder();
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_details);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mTripId = getIntent().getStringExtra(Constants.TRIPID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        bindViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    private void getTripsDetailsById(String tripId) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Loading trip details, please wait...");
        dialog.setCancelable(false);
        dialog.show();

        User user = SharePref.getInstance(getApplicationContext()).getUser();
        String tenantId = user.getTenantId();
        try {
            String trip_details_url = String.format(Constants.GET_TRIP_DETAIL_URL, tenantId, tripId);
            VolleyCommunicationManager.getInstance().SendRequest(trip_details_url, Request.Method.GET, null, getApplicationContext(), new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {
                            if (result.getString("message").equals("Success")) {

                                String data = result.get("data").toString();
                                if (data != null && data.trim().length() > 1) {
                                    Type type = new TypeToken<TripDetailModel>() {
                                    }.getType();
                                    Gson gson = new Gson();
                                    TripDetailModel tDetail = gson.fromJson(data, type);
                                    if (tDetail != null) {
                                        assignValues(tDetail);
                                    }
                                }
                            } else if (result.getString("message").equals("Unauthorized")) {
                                Log.d(TAG, "Get trip details request failed due to:" + result.getString("message"));
                                finish();

                            } else if (result.getString("message").equals("InternalServerError")) {
                                Log.d(TAG, "Get trip details request failed due to:" + result.getString("message"));
                                finish();
                            } else {
                                Log.d(TAG, "Get trip details request failed due to: " + result.getString("message"));
                                finish();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    dialog.dismiss();
                }

                @Override
                public void onError(VolleyError result) {
                    dialog.dismiss();
                    Log.d(TAG, "Get trip details request failed due to: " + result.getMessage());
                    return;
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
        }
    }

    @SuppressLint("SetTextI18n")
    private void assignValues(TripDetailModel tDetail) {
        if (tDetail != null) {
            String start_address = "NA", start_longitude = null, start_latitude = null;
            String end_address = "NA", end_longitude = null, end_latitude = null;

            if (TextUtils.isEmpty(tDetail.startTime) || "NA".equals(tDetail.startTime)) {
                mTripName.setText("");
            } else {
                mTripName.setText(DateUtils.tripDetailHeaderFormat(tDetail.startTime));
            }
            if (TextUtils.isEmpty(tDetail.startLocation) || "NA".equals(tDetail.startLocation)) {
            } else {
                start_address = tDetail.startLocation.split("#")[1];
                String latlong = tDetail.startLocation.split("#")[0];
                start_latitude = latlong.split(",")[0];
                start_longitude = latlong.split(",")[1];
            }
            if (TextUtils.isEmpty(tDetail.endLocation) || "NA".equals(tDetail.endLocation)) {
            } else {
                end_address = tDetail.endLocation.split("#")[1];
                String latlong = tDetail.endLocation.split("#")[0];
                end_latitude = latlong.split(",")[0];
                end_longitude = latlong.split(",")[1];
            }

            if (start_latitude != null && start_longitude != null) {
                LatLong startpoint = new LatLong(start_latitude, start_longitude);
                plotMarker("start", startpoint);
            }
            if (end_latitude != null && end_longitude != null) {
                LatLong endpoint = new LatLong(end_latitude, end_longitude);
                plotMarker("end", endpoint);
            }
            if (TextUtils.isEmpty(tDetail.startLocation) || "NA".equals(tDetail.startLocation)) {
                mStartLocation.setText("NA");
            } else {
                mStartLocation.setText(start_address);
            }
            if (TextUtils.isEmpty(tDetail.endLocation) || "NA".equals(tDetail.endLocation)) {
                mEndLocation.setText("NA");
            } else {
                mEndLocation.setText(end_address);
            }

            mStartTime.setText("NA");
            if (TextUtils.isEmpty(tDetail.startTime) || "NA".equals(tDetail.startTime)) {
                mStartTime.setText("NA");
            } else {
                mStartTime.setText(DateUtils.tripDetailFormat(tDetail.startTime));
            }

            if (TextUtils.isEmpty(tDetail.endTime) || "NA".equals(tDetail.endTime)) {
                mEndTime.setText("NA");
            } else {
                mEndTime.setText(DateUtils.tripDetailFormat(tDetail.endTime));
            }
            if (tDetail.stops == 0) {
                mStops.setText("0");
            } else {
                mStops.setText(tDetail.stops);
            }
            if (TextUtils.isEmpty(tDetail.milesDriven) || "-1".equals(tDetail.milesDriven)) {
                mMilesDriven.setText("NA");
            } else {
                mMilesDriven.setText(tDetail.milesDriven);
            }

            if (TextUtils.isEmpty(tDetail.tripDuration) || "NA".equals(tDetail.tripDuration)) {
                mTriptime.setText("NA");
            } else {
                mTriptime.setText(tDetail.tripDuration);
            }
            if (TextUtils.isEmpty(tDetail.fuelUsed) || "-1".equals(tDetail.fuelUsed)) {
                mFuelused.setText("NA");
            } else {
                mFuelused.setText(tDetail.fuelUsed);
            }
            if (TextUtils.isEmpty(tDetail.avgSpeed) || "-1".equals(tDetail.avgSpeed)) {
                mAveragespeed.setText("NA");
            } else {
                mAveragespeed.setText(tDetail.avgSpeed);
            }

            if (TextUtils.isEmpty(tDetail.topSpeed) || "-1".equals(tDetail.topSpeed)) {
                mTopspeed.setText("NA");
            } else {
                mTopspeed.setText(tDetail.topSpeed);
            }

            if (TextUtils.isEmpty(tDetail.mileage) || "-1".equals(tDetail.mileage)) {
                mMilage.setText("NA");
            } else {
                mMilage.setText(tDetail.mileage);
            }

            if (TextUtils.isEmpty(tDetail.speedings) || "00".equals(tDetail.speedings)) {
                mSpeeding.setText("NA");
            } else {
                mSpeeding.setText(tDetail.speedings);
            }

            mHardnraking.setText("NA");
            mEnginefaults.setText("NA");
            mAccelerator.setText("NA");
            mPhoneUsage.setText("NA");

            if (tDetail.locationDetails != null) {
                if (tDetail.locationDetails != null && !tDetail.locationDetails.isEmpty() && tDetail.locationDetails.size() > 0) {

                    mMap.clear();
                    LatLong firstPoint = tDetail.locationDetails.get(0);
                    plotMarker("start", firstPoint);

                    drawLine(tDetail.locationDetails);

                    LatLong lastPoint = tDetail.locationDetails.get(tDetail.locationDetails.size() - 1);
                    plotMarker("end", lastPoint);
                }
            }
        }
    }

    private void bindViews() {
        mTripName = (TextView) findViewById(R.id.tripname);

        mStartLocation = (TextView) findViewById(R.id.tv_startlocation);
        mEndLocation = (TextView) findViewById(R.id.tv_endlocation);
        mStartTime = (TextView) findViewById(R.id.tv_starttime);
        mEndTime = (TextView) findViewById(R.id.tv_endtime);
        mStops = (TextView) findViewById(R.id.tv_stops);

        mMilesDriven = (TextView) findViewById(R.id.tv_milesdriven);
        mTriptime = (TextView) findViewById(R.id.tv_triptime);
        mFuelused = (TextView) findViewById(R.id.tv_fuelused);
        mAveragespeed = (TextView) findViewById(R.id.tv_averagespeed);
        mTopspeed = (TextView) findViewById(R.id.tv_topspeed);
        mMilage = (TextView) findViewById(R.id.tv_milage);


        mSpeeding = (TextView) findViewById(R.id.tv_speeding);
        mHardnraking = (TextView) findViewById(R.id.tv_hardbraking);
        mEnginefaults = (TextView) findViewById(R.id.tv_enginefaults);
        mAccelerator = (TextView) findViewById(R.id.tv_aggresiveaccelator);
        mPhoneUsage = (TextView) findViewById(R.id.tv_phoneUsage);

        mBackButton = (LinearLayout) findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrectActivity();
            }
        });
    }

    private void finishCurrectActivity() {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
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
                    mBoundsBuilder.include(position);
                } else if ("start".equals(icon)) {
                    customIcon = R.drawable.startlocation;
                    mBoundsBuilder.include(position);
                }
                Marker marker = mMap.addMarker(new MarkerOptions().position(position).title(AddressStr).icon(BitmapDescriptorFactory.fromResource(customIcon)));
                mMarkerList.add(marker);
                CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngBounds(mBoundsBuilder.build(), 100);
                mMap.moveCamera(cameraPosition);
                mMap.animateCamera(cameraPosition);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void drawLine(List<LatLong> latLongs) {
        try {
            if (mMap == null) return;

            PolylineOptions lineOptions = new PolylineOptions();
            ArrayList<LatLng> points = new ArrayList<>();

            for (LatLong ltLng : latLongs) {
                LatLng position = new LatLng(Double.parseDouble(ltLng.latitude), Double.parseDouble(ltLng.longitude));
                points.add(position);

            }
            // Adding all the points in the route to LineOptions
            lineOptions.addAll(points);
            lineOptions.width(4);
            lineOptions.color(getColor(R.color.accent_black));

            CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngBounds(mBoundsBuilder.build(), 20);
            mMap.moveCamera(cameraPosition);
            // Drawing polyline in the Google Map for the i-th route
            mMap.addPolyline(lineOptions);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getTripsDetailsById(mTripId);
    }

    @Override
    public void onBackPressed() {
        finishCurrectActivity();
    }
}
