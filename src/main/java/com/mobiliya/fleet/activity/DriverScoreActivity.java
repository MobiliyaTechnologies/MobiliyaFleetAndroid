package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.models.DriverScore;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

@SuppressWarnings({"CanBeFinal", "RedundantCast"})
public class DriverScoreActivity extends Activity {
    private static final String TAG = "DriverScoreActivity";
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();
    private TextView tvVehicleStops, tvOverSpeeding, tvHardBreaking, tvAgressiveAccel;
    private ProgressBar mProgress;
    private TextView mTextScore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_score);
        initView();
        setValues();
    }

    @SuppressLint("SetTextI18n")
    private void setValues() {
        DriverScore Score = SharePref.getInstance(this).getDriverScore();
        int score = Score.driverBehaviour.driverScore;
        int vehicleStop = Score.driverBehaviour.vehicleStops;
        int overSpeeding = Score.driverBehaviour.overSpeeding;
        int hardBraking = Score.driverBehaviour.hardBraking;
        int aggresiveAcc = Score.driverBehaviour.aggressiveAccelerator;
        if (score == 0 && vehicleStop == 0 && overSpeeding == 0 && hardBraking == 0 && aggresiveAcc == 0) {
            LogUtil.d(TAG, "All values are 0, so return");
            return;
        }
        tvVehicleStops.setText(Integer.toString(vehicleStop));
        tvOverSpeeding.setText(Integer.toString(overSpeeding));
        tvHardBreaking.setText(Integer.toString(hardBraking));
        tvAgressiveAccel.setText(Integer.toString(aggresiveAcc));
        mProgress.setProgress(score);
        mTextScore.setText(Integer.toString(score));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    private void initView() {
        tvVehicleStops = (TextView) findViewById(R.id.vehicleStops);
        tvOverSpeeding = (TextView) findViewById(R.id.overSpeeding);
        tvHardBreaking = (TextView) findViewById(R.id.hardBreaking);
        tvAgressiveAccel = (TextView) findViewById(R.id.agressiveAccel);
        mProgress = (ProgressBar) findViewById(R.id.scoreDriver);
        mTextScore = (TextView) findViewById(R.id.scoreText);
        LinearLayout mBackButton = (LinearLayout) findViewById(R.id.backButton);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CommonUtil.registerGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CommonUtil.unRegisterGpsReceiver(getBaseContext(), gpsLocationReceiver);
    }
}
