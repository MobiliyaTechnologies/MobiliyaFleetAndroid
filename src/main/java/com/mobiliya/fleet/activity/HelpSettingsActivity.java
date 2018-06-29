package com.mobiliya.fleet.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.mobiliya.fleet.R;

public class HelpSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_settings);
    }

    public void back(View view ){
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }
}
