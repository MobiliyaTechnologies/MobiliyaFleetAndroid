package com.mobiliya.fleet.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.comm.VolleyCallback;
import com.mobiliya.fleet.comm.VolleyCommunicationManager;
import com.mobiliya.fleet.models.ResponseModel;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;

import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings({"ALL", "unused"})
public class ForgotPasswordActivity extends AppCompatActivity implements View.OnClickListener, View.OnFocusChangeListener {

    private static final String TAG = "ForgotPasswordActivity";
    private EditText mInputEmail_edt;
    private TextView mEmailError_tv;
    private String mEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        mInputEmail_edt = (EditText) findViewById(R.id.input_email);
        mEmailError_tv = (TextView) findViewById(R.id.forgot_error);
        Button _btDone = (Button) findViewById(R.id.btn_done);
        _btDone.setOnClickListener(this);
        TextView _btBack = (TextView) findViewById(R.id.btn_back);
        _btBack.setOnClickListener(this);
        mInputEmail_edt.setOnFocusChangeListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_done:
                if (validate()) {
                    resetPassword();
                }
                break;
            case R.id.btn_back:
                finish();
                overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
                break;
        }
    }

    public boolean validate() {
        boolean valid = true;

        mEmail = mInputEmail_edt.getText().toString().trim();

        if (mEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(mEmail).matches()) {
            mInputEmail_edt.setBackgroundResource(R.drawable.shadow_10_error);
            mEmailError_tv.setVisibility(View.VISIBLE);
            mEmailError_tv.setText("Enter a valid mEmail address");
            valid = false;
        } else {
            mInputEmail_edt.setBackgroundResource(R.drawable.shadow_10);
            mEmailError_tv.setVisibility(View.GONE);
        }
        return valid;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.left_to_right, R.anim.right_to_left);
    }

    private void resetPassword() {
        {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setIndeterminate(true);
            dialog.setMessage("Please wait....");
            dialog.setCancelable(false);
            dialog.show();

            String FORGOT_PW_URL = Constants.getIdentityURLs(getApplicationContext(),Constants.FORGOT_PASSWORD_URL);
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("email", mEmail);
                final String requestBody = CommonUtil.getPostDataString(jsonBody);

                VolleyCommunicationManager.getInstance().SendRequest(FORGOT_PW_URL, Request.Method.POST, requestBody, this, new VolleyCallback() {
                    @Override
                    public void onSuccess(JSONObject result) {
                        if (result != null) {
                            try {

                                ResponseModel response = new ResponseModel();
                                response.setMessage(result.getString("message"));
                                if (Constants.SUCCESS.equalsIgnoreCase(response.getMessage())) {
                                    dialog.dismiss();
                                    startNewActivity();
                                } else {
                                    dialog.dismiss();
                                    Toast.makeText(ForgotPasswordActivity.this, getString(R.string.try_again),
                                            Toast.LENGTH_LONG).show();
                                }

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
                        Toast.makeText(ForgotPasswordActivity.this, getString(R.string.error_reset),
                                Toast.LENGTH_LONG).show();
                    }
                });
            } catch (JSONException e) {
                dialog.dismiss();
                e.printStackTrace();
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();
            }
        }
    }

    private void startNewActivity() {
        LogUtil.d(TAG, "startNextActivity");
        Intent intent;
        intent = new Intent(ForgotPasswordActivity.this, ResetSucessActivity.class);
        startActivityForResult(intent, Constants.FORGOT_REQUEST_CODE);
        finish();
        overridePendingTransition(R.anim.enter, R.anim.leave);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // check if the request code is same as what is passed  here it is 2
        if (requestCode == Constants.FORGOT_REQUEST_CODE && resultCode == Constants.SETTINGS_RESULT_CODE) {
            finish();
        }
    }

    @Override
    public void onFocusChange(View view, boolean isfocused) {
        mInputEmail_edt.setBackgroundResource(R.drawable.shadow_10);
        mEmailError_tv.setVisibility(View.GONE);

    }
}
