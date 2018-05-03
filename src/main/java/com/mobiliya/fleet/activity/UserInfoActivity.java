package com.mobiliya.fleet.activity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.models.ResponseModel;
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.services.GpsLocationReceiver;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.ButterKnife;

@SuppressWarnings({"deprecation", "AccessStaticViaInstance", "WeakerAccess", "CanBeFinal", "unused", "TryWithIdenticalCatches"})
public class UserInfoActivity extends AppCompatActivity {
    static final String TAG = UserInfoActivity.class.getName();
    @Bind(R.id.user_name)
    TextView mUserName_tv;

    @Bind(R.id.company_name)
    TextView mCompany_tv;

    @Bind(R.id.fname)
    TextView mFirstName_tv;

    @Bind(R.id.lname)
    TextView mLastName_tv;

    @Bind(R.id.email)
    TextView mEmail_tv;

    @Bind(R.id.password)
    TextView mPassword_tv;

    @Bind(R.id.back_button)
    LinearLayout mBack_btn;

    private SharePref mSharePref;
    private GpsLocationReceiver gpsLocationReceiver = new GpsLocationReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        ButterKnife.bind(this);
        mSharePref = SharePref.getInstance(getBaseContext());
        mBack_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finishCurrectActivity();
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
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Logging in, please wait....");
        dialog.setCancelable(false);
        String email = mSharePref.getItem(Constants.PREF_EMAIL);
        String USER_URL = Constants.GET_USER_URL + "?email=" + email;
        LogUtil.d(TAG, "Get User details for email:" + email);
        LogUtil.d(TAG, "Get User details for URL:" + USER_URL);
        try {
            VolleyCommunicationManager.getInstance().SendRequest(USER_URL, Request.Method.GET, "", getApplicationContext(), new VolleyCallback() {
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
                            dialog.dismiss();
                        } catch (JSONException e) {
                            dialog.dismiss();
                            e.printStackTrace();
                        } catch (Exception e) {
                            dialog.dismiss();
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    dialog.dismiss();
                    Toast.makeText(UserInfoActivity.this, getString(R.string.try_again),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            dialog.dismiss();
            e.printStackTrace();
        }
    }

    public void setValues(User user) {
        mCompany_tv.setText(mSharePref.getItem(Constants.KEY_COMPANY_NAME, ""));
        mUserName_tv.setText("" + user.getFirstName() + " " + user.getLastName());
        mFirstName_tv.setText(user.getFirstName());
        mLastName_tv.setText(user.getLastName());
        mEmail_tv.setText(user.getEmail());
        mPassword_tv.setText(user.getPassword());
    }
}
