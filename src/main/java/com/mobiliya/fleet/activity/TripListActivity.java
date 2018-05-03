package com.mobiliya.fleet.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.adapters.ApiCallBackListener;
import com.mobiliya.fleet.adapters.CustomIgnitionListenerTracker;
import com.mobiliya.fleet.adapters.TripListAdapter;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.TripManagementUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static com.mobiliya.fleet.utils.CommonUtil.showToast;

@SuppressWarnings({"ALL", "unused"})
public class TripListActivity extends AppCompatActivity implements ApiCallBackListener {

    private TripListAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private ImageButton mBackButton;
    final List<Trip> mTriplist = new ArrayList<Trip>();
    private LinearLayout mNotrips_ll;
    private RecyclerView mRecyclerView;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_list);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mNotrips_ll = (LinearLayout) findViewById(R.id.notrips);
        mAdapter = new TripListAdapter(getBaseContext(), mTriplist, null);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CustomIgnitionListenerTracker.showDialogOnIgnitionChange(this);
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
        mBackButton = (ImageButton) findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrectActivity();
            }
        });
        if (CommonUtil.isNetworkConnected(getBaseContext())) {
            TripManagementUtils.getTripList(this, this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    private void finishCurrectActivity() {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }


    @Override
    public void onBackPressed() {
        finishCurrectActivity();
    }

    boolean isStatus = false;

    @Override
    public void onSuccess(JSONObject result) {
        if (result != null) {
            try {
                if (result.getString("message").equals("Success")) {
                    String data = result.get("data").toString();
                    JSONArray dataarr = result.getJSONArray("data");

                    if (dataarr != null && dataarr.length() > 0) {
                        Type type = new TypeToken<List<Trip>>() {
                        }.getType();
                        Gson gson = new Gson();
                        List<Trip> tList = gson.fromJson(data, type);

                        if (tList != null) {
                            mNotrips_ll.setVisibility(View.GONE);
                            mRecyclerView.setVisibility(View.VISIBLE);
                            for (int i = 0; i < tList.size(); i++) {
                                Trip trip = tList.get(i);
                                mTriplist.add(trip);

                            }
                            mAdapter.notifyDataChanged(tList);
                        } else {
                            isStatus = true;
                        }
                    } else {
                        isStatus = true;
                    }

                } else {
                    isStatus = true;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            isStatus = true;
        }

        if (isStatus) {
            mRecyclerView.setVisibility(View.GONE);
            showToast(TripListActivity.this, getString(R.string.no_data_available));
        }

    }

    @Override
    public void onError(VolleyError result) {
        if (result != null) {
            if (result.networkResponse != null && result.networkResponse.statusCode == 401) {
                showToast(TripListActivity.this, getString(R.string.aunthentication_error));
            } else {
                showToast(TripListActivity.this, getString(R.string.error_occured));
            }
        }
        finish();
    }
}
