package com.mobiliya.fleet.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.utils.Constants;

@SuppressWarnings({"ALL", "unused"})
public class ResetSucessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_sucess);
        Button _btnDone = (Button) findViewById(R.id.btn_done);
        _btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(Constants.SETTINGS_RESULT_CODE);
                finish();
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Constants.SETTINGS_RESULT_CODE);
        finish();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

}
