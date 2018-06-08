package com.mobiliya.fleet.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.SharePref;

import static com.mobiliya.fleet.utils.Constants.FLEETURL;
import static com.mobiliya.fleet.utils.Constants.IDENTITYURL;
import static com.mobiliya.fleet.utils.Constants.TRIPURL;

@SuppressWarnings({"ALL", "unused"})
public class SplashActivity extends AppCompatActivity {

    private final String mIdentityurl="https://mobiliya-identity-service.azurewebsites.net/";
    private final String mFleeturl="https://mobiliya-fleet-service.azurewebsites.net/";
    private final String mTripurl="https://mobiliya-trip-service.azurewebsites.net/";

    private static SharedPreferences mPreference;
    private static final String SHARED_PREF_NAME = "configpref";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mPreference = getSharedInstance(getBaseContext());
        String identityurls = getIdentityUrl(getApplicationContext());
        if(TextUtils.isEmpty(identityurls)) {
            setURLs(mIdentityurl, mFleeturl, mTripurl);
        }
        if (SharePref.getInstance(getBaseContext()).getMemorySize() == 0) {
            SharePref.getInstance(getBaseContext()).setMemorySize(Constants.SET_DEFAULT_MEMORY_SIZE);
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, SignInActivity.class);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

    public void setURLs(String identity, String fleeturl, String tripUrl) {
        addItem(IDENTITYURL, identity);
        addItem(FLEETURL, fleeturl);
        addItem(TRIPURL, tripUrl);
    }

    /**
     * Add item to Shared Preferences.
     *
     * @param name  item name.
     * @param value item value.
     */
    public void addItem(String name, String value) {
        SharedPreferences.Editor mSharedPrefsEditor = getSharedInstance(getBaseContext()).edit();
        mSharedPrefsEditor.putString(name, value);
        mSharedPrefsEditor.apply();
    }
    public static SharedPreferences getSharedInstance(Context context) {
        if (mPreference == null) {
            mPreference = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
        }
        return mPreference;
    }

    public static String getIdentityUrl(Context cxt) {
        return getItem(cxt, IDENTITYURL, "");
    }
    public static String getItem(Context cxt, String name, @SuppressWarnings("SameParameterValue") String defaultValue) {
        return getSharedInstance(cxt).getString(name, defaultValue);
    }
}
