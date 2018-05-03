package com.mobiliya.fleet.activity;

import android.annotation.SuppressLint;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mobiliya.fleet.BuildConfig;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;

import butterknife.Bind;
import butterknife.ButterKnife;

@SuppressWarnings({"WeakerAccess", "CanBeFinal", "unused"})
public class AboutUsActivity extends BaseActivity {
    @Bind(R.id.back_button)
    ImageButton mBack_btn;

    @Bind(R.id.version)
    TextView version;
    GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();
    private LocationManager locationManager;
    private boolean isGPSEnabled;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);
        ButterKnife.bind(this);
        version.setText("Version :" + BuildConfig.VERSION_NAME);

        mBack_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrectActivity();
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

    private void finishCurrectActivity() {
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    @Override
    public void onBackPressed() {
        finishCurrectActivity();
    }
}
