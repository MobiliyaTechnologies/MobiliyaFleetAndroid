package com.mobiliya.fleet.activity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.utils.LogUtil;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mobiliya.fleet.utils.Constants.FLEETURL;
import static com.mobiliya.fleet.utils.Constants.IDENTITYURL;
import static com.mobiliya.fleet.utils.Constants.TRIPURL;

public class ConfigureUrlActivity extends AppCompatActivity {

    private static final String TAG = ConfigureUrlActivity.class.getName();
    @Bind(R.id.input_identityurl)
    EditText mIdentityUrl_edt;

    @Bind(R.id.identity_error)
    TextView mIdentityUrlError_tv;

    @Bind(R.id.input_fleeturl)
    EditText mFleetUrl_edt;

    @Bind(R.id.fleet_error)
    TextView mFleetUrlError_tv;

    @Bind(R.id.input_tripurl)
    EditText mTripUrl_edt;

    @Bind(R.id.trip_error)
    TextView mTripUrlError_tv;

    @Bind(R.id.btn_save)
    Button mSave_btn;

    @Bind(R.id.btn_reset)
    TextView mReset_btn;

    String mIdentityurl;
    String mFleeturl;
    String mTripurl;

    private static SharedPreferences mPreference;
    private static final String SHARED_PREF_NAME = "configpref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_url);
        ButterKnife.bind(this);

        mPreference = getSharedInstance(getBaseContext());
        String identityurls = getIdentityUrl(getApplicationContext());
        mIdentityUrl_edt.setText(identityurls);
        String fleeturls = getFleetUrl(getApplicationContext());
        mFleetUrl_edt.setText(fleeturls);
        String tripurls = getTripServiceUrl(getApplicationContext());
        mTripUrl_edt.setText(tripurls);

        mSave_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validate()) {
                    saveToSharedPreference();
                }
            }
        });

        mReset_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIdentityUrl_edt.setText("");
                mFleetUrl_edt.setText("");
                mTripUrl_edt.setText("");

                mIdentityUrlError_tv.setVisibility(View.GONE);
                mFleetUrlError_tv.setVisibility(View.GONE);
                mTripUrlError_tv.setVisibility(View.GONE);

            }
        });
    }

    private void saveToSharedPreference() {
        setURLs(mIdentityurl, mFleeturl, mTripurl);
        finish();
    }

    private boolean validate() {
        mIdentityurl = mIdentityUrl_edt.getText().toString()+"/";
        mFleeturl = mFleetUrl_edt.getText().toString()+"/";
        mTripurl = mTripUrl_edt.getText().toString()+"/";

        if (TextUtils.isEmpty(mIdentityurl)) {
            mIdentityUrlError_tv.setVisibility(View.VISIBLE);
            return true;
        } else {
            mIdentityUrlError_tv.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(mFleeturl)) {
            mFleetUrlError_tv.setVisibility(View.VISIBLE);
            return true;
        } else {
            mFleetUrlError_tv.setVisibility(View.GONE);
        }

        if (TextUtils.isEmpty(mTripurl)) {
            mTripUrlError_tv.setVisibility(View.VISIBLE);
            return true;
        } else {
            mTripUrlError_tv.setVisibility(View.GONE);
        }


        return false;
    }

    public void setURLs(String identity, String fleeturl, String tripUrl) {
        addItem(IDENTITYURL, identity);
        addItem(FLEETURL, fleeturl);
        addItem(TRIPURL, tripUrl);
    }


    public static SharedPreferences getSharedInstance(Context context) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        }
        return mPreference;
    }

    /**
     * Add item to Shared Preferences.
     *
     * @param name  item name.
     * @param value item value.
     */
    public void addItem(String name, String value) {
        LogUtil.d(TAG, "act_changes addItem name " + name
                + " value{" + value + "}");
        SharedPreferences.Editor mSharedPrefsEditor = getSharedInstance(getBaseContext()).edit();
        mSharedPrefsEditor.putString(name, value);
        mSharedPrefsEditor.apply();
    }


    public static String getIdentityUrl(Context cxt) {
        return getItem(cxt, IDENTITYURL, "");
    }

    public static String getFleetUrl(Context cxt) {
        return getItem(cxt, FLEETURL, "");
    }

    public static String getTripServiceUrl(Context cxt) {
        return getItem(cxt, TRIPURL, "");
    }
    public static String getItem(Context cxt, String name, @SuppressWarnings("SameParameterValue") String defaultValue) {
        return getSharedInstance(cxt).getString(name, defaultValue);
    }
}
