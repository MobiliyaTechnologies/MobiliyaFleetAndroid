package com.mobiliya.fleet.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.models.ResponseModel;
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.models.Vehicle;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mobiliya.fleet.activity.ConfigureUrlActivity.getFleetUrl;
import static com.mobiliya.fleet.activity.ConfigureUrlActivity.getIdentityUrl;
import static com.mobiliya.fleet.activity.ConfigureUrlActivity.getTripServiceUrl;
import static com.mobiliya.fleet.utils.Constants.GET_USER_URL;
import static com.mobiliya.fleet.utils.Constants.REGISTRATION_NUMBER;
import static com.mobiliya.fleet.utils.Constants.VEHICLES;

@SuppressWarnings({"ALL", "unused"})
public class SignInActivity extends AppCompatActivity implements View.OnFocusChangeListener {
    private static final String TAG = "SignInActivity";

    @Bind(R.id.input_email)
    EditText mEmail_edt;
    @Bind(R.id.username_error)
    TextView mEmailError_tv;

    @Bind(R.id.input_password)
    EditText mPassword_edt;
    @Bind(R.id.password_error)
    TextView mPasswordError_tv;

    @Bind(R.id.input_company)
    EditText mCompany_edt;
    @Bind(R.id.company_error)
    TextView mCompanyError_tv;

    @Bind(R.id.input_vehical_registration)
    EditText mVehicleReg_edt;
    @Bind(R.id.vehicle_error)
    TextView mVehicleError_tv;

    @Bind(R.id.link_tern_condition)
    TextView mLinkTermAndConditions_tv;
    @Bind(R.id.forgot_password)
    TextView mForgotPassword_tv;
    @Bind(R.id.btn_login)
    Button mSignIn_btn;

    @Bind(R.id.url_error)
    TextView mConfigureError;

    @Bind(R.id.configure)
    ImageButton mConfigute_imbtn;

    private SharePref mSharePref;
    private ProgressDialog mDialog = null;

    String email;
    private String password;
    private String companyName;
    private String vehicleRegistrationNumber;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharePref = SharePref.getInstance(this);
        mDialog = new ProgressDialog(this);
        mDialog.setIndeterminate(true);
        mDialog.setMessage("Logging in, please wait....");
        mDialog.setCancelable(false);

        boolean isUSerLoggedIn = mSharePref.getBooleanItem(Constants.PREF_USER_LOGED_IN, false);
        boolean isMovedToDashBoard = mSharePref.getBooleanItem(Constants.PREF_MOVED_TO_DASHBOARD, false);
        boolean isMovedToBtScreen = mSharePref.getBooleanItem(Constants.KEY_MOVE_TO_BT_SCREEN, false);
        if (isUSerLoggedIn && isMovedToDashBoard) {
            DashboardActivity mActivity = new DashboardActivity();
            startNextActivity(mActivity);
            return;
        } else if (isUSerLoggedIn && isMovedToBtScreen) {
            BluetoothConnectionActivity mActivity = new BluetoothConnectionActivity();
            startNextActivity(mActivity);
            return;
        } else if (isUSerLoggedIn) {
            startNextActivity(null);
            return;
        }
        setContentView(R.layout.activity_sign_in);
        ButterKnife.bind(this);
        setTermAndConditionText();
        mSignIn_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
        mForgotPassword_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startForgotActivity();
            }
        });
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null && mBluetoothAdapter.disable()) {
            mBluetoothAdapter.enable();
        }


        mEmail_edt.setOnFocusChangeListener(this);
        mPassword_edt.setOnFocusChangeListener(this);
        mCompany_edt.setOnFocusChangeListener(this);
        mVehicleReg_edt.setOnFocusChangeListener(this);
        mConfigute_imbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mConfigureError.setVisibility(View.GONE);
                startActivity(new Intent(SignInActivity.this,ConfigureUrlActivity.class));
            }
        });
    }

    private void startForgotActivity() {
        LogUtil.d(TAG, "startNextActivity");
        Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.enter, R.anim.leave);
    }

    private void setTermAndConditionText() {
        SpannableString ss = new SpannableString(getResources().getString(R.string.term_conditions));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                LogUtil.d(TAG, "clicked on term & condition");
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };
        ss.setSpan(clickableSpan, ss.length() - 18, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mLinkTermAndConditions_tv.setText(ss);
        mLinkTermAndConditions_tv.setMovementMethod(LinkMovementMethod.getInstance());
        mLinkTermAndConditions_tv.setHighlightColor(getResources().getColor(R.color.term_and_condition));
    }

    public void signIn() {
        LogUtil.d(TAG, "signIn");
        if (CommonUtil.isNetworkConnected(getBaseContext())) {
            if (!validate()) {
                mSignIn_btn.setEnabled(false);
                mSignIn_btn.setBackgroundResource(R.drawable.login_button_grey);
                return;
            }
            mSignIn_btn.setEnabled(true);
            mSignIn_btn.setBackgroundResource(R.drawable.login_button_gradient);
            attempLogin();
        } else {
            CommonUtil.showToast(this, getString(R.string.no_internet_connection));
        }
    }

    public boolean validate() {
        boolean valid = true;

        email = mEmail_edt.getText().toString().trim();
        password = mPassword_edt.getText().toString().trim();
        companyName = mCompany_edt.getText().toString().trim();
        vehicleRegistrationNumber = mVehicleReg_edt.getText().toString().trim();


        String identityurl=getIdentityUrl(getApplicationContext());
        String fleeturl=getFleetUrl(getApplicationContext());
        String tripurls=getTripServiceUrl(getApplicationContext());

        if(!TextUtils.isEmpty(identityurl) && !TextUtils.isEmpty(fleeturl) &&   !TextUtils.isEmpty(tripurls)){
            mConfigureError.setVisibility(View.INVISIBLE);
        }else{
            mConfigureError.setVisibility(View.VISIBLE);
            return valid=false;
        }

        if (companyName.isEmpty()) {
            mCompanyError_tv.setVisibility(View.VISIBLE);
            mCompany_edt.setBackgroundResource(R.drawable.shadow_10_error);
            mCompany_edt.setTextColor(R.color.deep_carpen_pink);
            valid = false;
        } else {
            mCompanyError_tv.setVisibility(View.INVISIBLE);
        }

        if (vehicleRegistrationNumber.isEmpty()) {
            mVehicleError_tv.setVisibility(View.VISIBLE);
            mVehicleReg_edt.setBackgroundResource(R.drawable.shadow_10_error);
            mVehicleReg_edt.setTextColor(R.color.deep_carpen_pink);
            valid = false;
        } else {
            mVehicleError_tv.setVisibility(View.INVISIBLE);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mEmailError_tv.setVisibility(View.VISIBLE);
            mEmail_edt.setBackgroundResource(R.drawable.shadow_10_error);
            mEmail_edt.setTextColor(R.color.deep_carpen_pink);
            mEmailError_tv.setText("Enter a valid email address");
            valid = false;
        } else {
            mEmailError_tv.setVisibility(View.INVISIBLE);
        }

        if (password.isEmpty()) {
            mPasswordError_tv.setVisibility(View.VISIBLE);
            mPassword_edt.setBackgroundResource(R.drawable.shadow_10_error);
            mPassword_edt.setTextColor(R.color.deep_carpen_pink);
            valid = false;
        } else {
            mPasswordError_tv.setVisibility(View.INVISIBLE);
        }

        return valid;
    }

    private void attempLogin() {
        mDialog.show();
        String LOGIN_URL = Constants.getIdentityURLs(getApplicationContext(),Constants.LOGIN_URL);
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("email", email);
            jsonBody.put("password", password);
            final String requestBody = CommonUtil.getPostDataString(jsonBody);
            VolleyCommunicationManager.getInstance().SendRequest(LOGIN_URL, Request.Method.POST, requestBody, this, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {

                            ResponseModel response = new ResponseModel();
                            response.setData(result.get("data"));
                            response.setMessage(result.getString("message"));
                            JSONObject responseData = (JSONObject) response.getData();
                            if (responseData != null) {
                                String tokenStr = responseData.getString("access_token");
                                LogUtil.d(TAG, "Access Token:" + tokenStr);
                                String expires = responseData.getString("expires");
                                LogUtil.d(TAG, "expires:" + expires);
                                String userData = responseData.
                                        getJSONObject("userDetails").getString("tenantCompany");
                                if (!companyName.equalsIgnoreCase(userData)) {
                                    mDialog.dismiss();
                                    CommonUtil.showToast(SignInActivity.this, getString(R.string.incorrect_company_name));
                                    return;
                                }
                                mSharePref.addItem(Constants.PREF_ACCESS_TOKEN, tokenStr);
                                mSharePref.addItem(Constants.PREF_EXPIRES, expires);
                                mSharePref.addItem(Constants.PREF_EMAIL, email);
                                mSharePref.addItem(Constants.PREF_PASSWORD, password);
                                mSharePref.addItem(Constants.KEY_COMPANY_NAME, userData);
                                mSharePref.addItem(Constants.KEY_VEHICLE_REGISTRATION_NO, vehicleRegistrationNumber);
                                getUserDetails();
                            } else {
                                mDialog.dismiss();
                                CommonUtil.showToast(SignInActivity.this, getString(R.string.incorrect_credentials));
                            }
                        } catch (JSONException e) {
                            mDialog.dismiss();
                            e.printStackTrace();
                        } catch (Exception e) {
                            mDialog.dismiss();
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    mDialog.dismiss();
                    Toast.makeText(SignInActivity.this, getString(R.string.incorrect_credentials),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (JSONException e) {
            mDialog.dismiss();
            e.printStackTrace();
        } catch (Exception e) {
            mDialog.dismiss();
            e.printStackTrace();
        }

    }

    private void startNextActivity(Activity activity) {
        LogUtil.d(TAG, "startNextActivity");
        Intent intent;
        if (activity != null) {
            intent = new Intent(SignInActivity.this, activity.getClass());
        } else {
            intent = new Intent(SignInActivity.this, LocationSettingActivity.class);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.enter, R.anim.leave);
        finish();
    }

    private void getUserDetails() {
        mDialog.setMessage("Please wait....");
        String email = mSharePref.getItem(Constants.PREF_EMAIL);
        String USER_URL = Constants.getIdentityURLs(getApplicationContext(),GET_USER_URL) + "?email=" + email;
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
                            getVehicleDetails();
                        } catch (JSONException e) {
                            mDialog.dismiss();
                            e.printStackTrace();
                        } catch (Exception e) {
                            mDialog.dismiss();
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    mDialog.dismiss();
                    Toast.makeText(SignInActivity.this, getString(R.string.try_again),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            mDialog.dismiss();
            e.printStackTrace();
        }
    }

    private void getVehicleDetails() {
        String mTenantId = mSharePref.getUser().getTenantId();
        String mVehicleRegistrationNo = mSharePref.getItem(Constants.KEY_VEHICLE_REGISTRATION_NO, "");
        LogUtil.d(TAG, "Vehicle registration no is:" + mVehicleRegistrationNo);
        String VEHICLE_URL =getFleetUrl(getApplicationContext())+
                mTenantId + VEHICLES + REGISTRATION_NUMBER + mVehicleRegistrationNo;
        LogUtil.d(TAG, "Get vehicle details:" + VEHICLE_URL);
        try {
            VolleyCommunicationManager.getInstance().SendRequest(VEHICLE_URL, Request.Method.GET, "", this, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {
                            ResponseModel response = new ResponseModel();
                            response.setData(result.get("data"));
                            response.setMessage(result.getString("message"));
                            JSONArray responseArray = (JSONArray) response.getData();
                            if (responseArray.length() > 0) {
                                JSONObject responseData = (JSONObject) responseArray.get(0);
                                Vehicle vehicle = mSharePref.convertVehicleResponse(responseData);
                                if(TextUtils.isEmpty(vehicle.getDeviceId())){
                                    mDialog.dismiss();
                                    Toast.makeText(SignInActivity.this, getString(R.string.no_device),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                if(vehicle.getDeviceId().equalsIgnoreCase("null")){
                                    mDialog.dismiss();
                                    Toast.makeText(SignInActivity.this, getString(R.string.no_device),
                                            Toast.LENGTH_LONG).show();
                                    return;
                                }
                                JSONObject deviceData = responseData.getJSONObject("Device");
                                String iotConnectionString=deviceData.getString("connectionString");
                                mSharePref.addItem(Constants.IOTURL, iotConnectionString);
                                String mUserId = mSharePref.getItem(Constants.KEY_ID, "");
                                LogUtil.d(TAG, "mUserId Id is:" + mUserId);
                                if (mUserId.equalsIgnoreCase(vehicle.getUserId())) {
                                    mSharePref.setVehicleID(vehicle.getId());
                                    mSharePref.setVehicleData(vehicle);
                                    mSharePref.addItem(Constants.PREF_USER_LOGED_IN, true);
                                    LocationSettingActivity mActivity = new LocationSettingActivity();
                                    mDialog.dismiss();
                                    startNextActivity(mActivity);
                                } else {
                                    mDialog.dismiss();
                                    Toast.makeText(SignInActivity.this, getString(R.string.incorrect_vehicle),
                                            Toast.LENGTH_LONG).show();
                                }
                            } else {
                                mDialog.dismiss();
                                Toast.makeText(SignInActivity.this, getString(R.string.incorrect_vehicle),
                                        Toast.LENGTH_LONG).show();
                            }
                        } catch (JSONException e) {
                            mDialog.dismiss();
                            e.printStackTrace();
                        } catch (Exception e) {
                            mDialog.dismiss();
                            e.printStackTrace();
                        }
                    } else {
                        Toast.makeText(SignInActivity.this, getString(R.string.incorrect_vehicle),
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    mDialog.dismiss();
                    Toast.makeText(SignInActivity.this, getString(R.string.try_again),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            mDialog.dismiss();
            e.printStackTrace();
        }
    }

    @Override
    public void onFocusChange(View view, boolean iffocused) {
        mSignIn_btn.setEnabled(true);
        mSignIn_btn.setBackgroundResource(R.drawable.login_button_gradient);

        switch (view.getId()) {
            case R.id.input_email:
                mEmail_edt.setBackgroundResource(R.drawable.edittext_selector);
                mEmailError_tv.setVisibility(View.INVISIBLE);
                break;
            case R.id.input_password:
                mPassword_edt.setBackgroundResource(R.drawable.edittext_selector);
                mPasswordError_tv.setVisibility(View.INVISIBLE);
                break;
            case R.id.input_company:
                mCompany_edt.setBackgroundResource(R.drawable.edittext_selector);
                mCompanyError_tv.setVisibility(View.INVISIBLE);
                break;
            case R.id.input_vehical_registration:
                mVehicleReg_edt.setBackgroundResource(R.drawable.edittext_selector);
                mVehicleError_tv.setVisibility(View.INVISIBLE);
                break;
        }

    }
}