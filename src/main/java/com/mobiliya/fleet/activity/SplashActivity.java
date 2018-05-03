package com.mobiliya.fleet.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.SharePref;

@SuppressWarnings({"ALL", "unused"})
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

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
}
