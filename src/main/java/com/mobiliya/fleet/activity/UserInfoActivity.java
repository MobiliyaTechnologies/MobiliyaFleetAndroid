package com.mobiliya.fleet.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.location.GpsLocationReceiver;
import com.mobiliya.fleet.models.ResponseModel;
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mobiliya.fleet.utils.Constants.GET_USER_URL;

public class UserInfoActivity extends AppCompatActivity {
    static final String TAG = UserInfoActivity.class.getName();
    @Bind(R.id.user_name)
    TextView mUserName_tv;

    @Bind(R.id.company_name)
    TextView mCompany_tv;

    @Bind(R.id.fname)
    TextView mFirstName_tv;

    @Bind(R.id.email)
    TextView mEmail_tv;

    @Bind(R.id.btn_change)
    Button mChangePassword;

    @Bind(R.id.back_button)
    LinearLayout mBack_btn;

    private SharePref mSharePref;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    ProgressDialog dialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        ButterKnife.bind(this);
        dialog = new ProgressDialog(this);
        mSharePref = SharePref.getInstance(getBaseContext());
        mBack_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrectActivity();
            }
        });

        mChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(UserInfoActivity.this, ChangePasswordActivity.class));
            }
        });
        if (CommonUtil.isNetworkConnected(getBaseContext())) {
            getUserDetails();
        } else {
            setValues(mSharePref.getUser());
        }
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

    private void getUserDetails() {
        dialog.setIndeterminate(true);
        dialog.setMessage("Please wait....");
        dialog.setCancelable(false);
        dialog.show();
        String email = mSharePref.getItem(Constants.PREF_EMAIL);
        String USER_URL = Constants.getIdentityURLs(getApplicationContext(), GET_USER_URL) + "?email=" + email;
        LogUtil.d(TAG, "Get User details for email:" + email);
        LogUtil.d(TAG, "Get User details for URL:" + USER_URL);
        try {
            VolleyCommunicationManager.getInstance().SendRequest(USER_URL, Request.Method.GET, "", this, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {

                            ResponseModel response = new ResponseModel();
                            response.setData(result.get("data"));
                            response.setMessage(result.getString("message"));

                            JSONArray responseArray = (JSONArray) response.getData();
                            JSONObject responseData = (JSONObject) responseArray.get(0);

                            User user = mSharePref.convertUserResponse(responseData);
                            mSharePref.setUserData(user);
                            setValues(user);
                            dismissDialog();
                        } catch (JSONException e) {
                            dismissDialog();
                            e.printStackTrace();
                        } catch (Exception e) {
                            dismissDialog();
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    dismissDialog();
                    Toast.makeText(UserInfoActivity.this, getString(R.string.try_again),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            dismissDialog();
            e.printStackTrace();
        }
    }

    public void setValues(User user) {
        mCompany_tv.setText(mSharePref.getItem(Constants.KEY_COMPANY_NAME, ""));
        mUserName_tv.setText("" + user.getFirstName() + " " + user.getLastName());
        mFirstName_tv.setText("" + user.getFirstName() + " " + user.getLastName());
        mEmail_tv.setText(user.getEmail());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissDialog();
    }

    private void dismissDialog(){
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
                dialog = null;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
