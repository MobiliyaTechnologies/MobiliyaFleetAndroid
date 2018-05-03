package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;

import java.util.Map;

/**
 * Created by prashant on 23-02-2018.
 * This is an base activity of all activities
 */

@SuppressWarnings({"ALL", "unused"})
@SuppressLint("Registered")
public class BaseActivity extends Activity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    public void setBTStatus(String status) {
    }

    public void stateUpdateByAdapter(Map<String, String> commandResult) {
    }

    @Override
    protected void onResume() {
        super.onResume();

    }
}
