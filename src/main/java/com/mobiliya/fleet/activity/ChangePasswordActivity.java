package com.mobiliya.fleet.activity;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.Bind;
import butterknife.ButterKnife;

import static com.mobiliya.fleet.utils.CommonUtil.isValidPassword;

public class ChangePasswordActivity extends AppCompatActivity implements View.OnFocusChangeListener {


    @Bind(R.id.input_password)
    EditText mPassword_edt;

    @Bind(R.id.password_error)
    TextView mPassword_error;

    @Bind(R.id.reenter_password)
    EditText mReEnterPassword_edt;

    @Bind(R.id.reenter_password_error)
    TextView mReEnterPassword_error;

    @Bind(R.id.btn_change)
    Button mChangePassword_btn;

    @Bind(R.id.btn_back)
    TextView mBack_btn;
    private String mPassword;
    private String mRe_Password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        ButterKnife.bind(this);


        mPassword_edt.setOnFocusChangeListener(this);
        mReEnterPassword_edt.setOnFocusChangeListener(this);

        mBack_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mChangePassword_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validated()){
                    resetPassword();
                }
            }
        });
    }


    @Override
    public void onFocusChange(View view, boolean iffocused) {

        switch (view.getId()) {
            case R.id.input_password:
                mPassword_edt.setBackgroundResource(R.drawable.shadow_10);
                mPassword_error.setVisibility(View.INVISIBLE);
                break;
            case R.id.reenter_password:
                mReEnterPassword_edt.setBackgroundResource(R.drawable.shadow_10);
                mReEnterPassword_error.setVisibility(View.INVISIBLE);
                break;
        }

    }

    private boolean validated() {

        mPassword = mPassword_edt.getText().toString().trim();
        mRe_Password = mReEnterPassword_edt.getText().toString().trim();

        if (TextUtils.isEmpty(mPassword)) {
            mPassword_error.setVisibility(View.VISIBLE);
            return false;
        }else if(!isValidPassword(mPassword))
        {
            mPassword_error.setVisibility(View.VISIBLE);
            mPassword_error.setText(getText(R.string.password_invalid));
            return false;
        }
        else {
            mPassword_error.setVisibility(View.INVISIBLE);
        }

        if (TextUtils.isEmpty(mRe_Password)) {

            mReEnterPassword_error.setVisibility(View.VISIBLE);
            return false;
        }else if(!isValidPassword(mRe_Password))
        {
            mReEnterPassword_error.setVisibility(View.VISIBLE);
            mReEnterPassword_error.setText(getText(R.string.password_invalid));
            return false;
        }
        else {
            mReEnterPassword_edt.setBackgroundResource(R.drawable.shadow_10);
            mReEnterPassword_error.setVisibility(View.INVISIBLE);
        }

        if(!mPassword.equals(mRe_Password)){

            mReEnterPassword_error.setVisibility(View.VISIBLE);
            mReEnterPassword_error.setText(getText(R.string.mimatch));
            return false;
        }

        return true;
    }


    private void resetPassword() {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setMessage("Please wait....");
        dialog.setCancelable(false);
        dialog.show();

        String RESET_URL = Constants.getIdentityURLs(getApplicationContext(),Constants.RESET_PASSWORD_URL);
        JSONObject jsonBody = new JSONObject();
        try {
            final SharePref pref=SharePref.getInstance(getBaseContext());
            String password=pref.getItem(Constants.PREF_PASSWORD);
            String email=pref.getItem(Constants.PREF_EMAIL);

            jsonBody.put("email", email);
            jsonBody.put("oldPassword", password);
            jsonBody.put("newPassword", mRe_Password);
            final String requestBody = CommonUtil.getPostDataString(jsonBody);
            VolleyCommunicationManager.getInstance().SendRequest(RESET_URL, Request.Method.PUT, requestBody, this, new VolleyCallback() {
                @Override
                public void onSuccess(JSONObject result) {
                    if (result != null) {
                        try {
                            String res = result.getString("message");
                            if("Success".equalsIgnoreCase(res)){
                                pref.addItem(Constants.PREF_PASSWORD, mRe_Password);

                                Toast.makeText(ChangePasswordActivity.this, getString(R.string.pass_change_succes),
                                        Toast.LENGTH_LONG).show();
                                finish();

                            }else{
                                Toast.makeText(ChangePasswordActivity.this, getString(R.string.failed_to_change_password),
                                        Toast.LENGTH_LONG).show();
                            }
                            dialog.dismiss();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(VolleyError result) {
                    dialog.dismiss();
                    Toast.makeText(ChangePasswordActivity.this, getString(R.string.failed_to_change_password),
                            Toast.LENGTH_LONG).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            dialog.dismiss();
        }

    }
}
