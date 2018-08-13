package com.mobiliya.fleet.comm;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.mobiliya.fleet.R;
import com.mobiliya.fleet.activity.SettingsActivity;
import com.mobiliya.fleet.activity.SignInActivity;
import com.mobiliya.fleet.models.ResponseModel;
import com.mobiliya.fleet.models.User;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import static com.android.volley.VolleyLog.TAG;
import static com.mobiliya.fleet.utils.CommonUtil.trimCache;


public class VolleyCommunicationManager {

    private static RequestQueue sRequestQueue;
    private static VolleyCommunicationManager sSoleInstance;
    private static boolean isSessionExpired = false;
    public static JSONObject mJsonobj = null;

    private VolleyCommunicationManager() {
    }  //private constructor.

    public static VolleyCommunicationManager getInstance() {
        if (sSoleInstance == null) { //if there is no instance available... create new one
            sSoleInstance = new VolleyCommunicationManager();

        }

        return sSoleInstance;
    }

    public static JSONObject SendRequest(final String URL, final int method, final String requestBody, final Context context, final VolleyCallback callback) {
        try {
            if (sRequestQueue == null) {
                sRequestQueue = Volley.newRequestQueue(context);
            }
            LogUtil.d(TAG, "Post request string:" + requestBody);
            StringRequest stringRequest = new StringRequest(method, URL, new Response.Listener<String>() {

                @Override
                public void onResponse(String response) {
                    try {
                        if (response != null) {
                            mJsonobj = new JSONObject(response);
                            callback.onSuccess(mJsonobj);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.i("VOLLEY", "Response");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error != null) {
                        if (error.networkResponse != null && error.networkResponse.statusCode == 401) {
                            final SharePref pref = SharePref.getInstance(context);
                            User user = pref.getUser();
                            if(TextUtils.isEmpty(user.getEmail()) ||
                                    TextUtils.isEmpty(pref.getItem(Constants.PREF_PASSWORD, ""))) {
                                isSessionExpired = false;
                                callback.onError(error);
                                return;
                            }
                            isSessionExpired = true;
                            VolleyCommunicationManager.refreshToken(context, URL, method, requestBody, callback);
                        } else {
                            isSessionExpired = false;
                            callback.onError(error);
                        }
                    }
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/x-www-form-urlencoded; charset=UTF-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }

                // set headers
                @Override
                public Map<String, String> getHeaders() throws com.android.volley.AuthFailureError {
                    Map<String, String> params = new HashMap<>();
                    String access_token = SharePref.getInstance(context).getInstance(context).getItem(Constants.PREF_ACCESS_TOKEN, "");
                    if (!TextUtils.isEmpty(access_token)) {
                        LogUtil.d(TAG, "Authorization token:"+access_token);
                        params.put("Authorization", access_token);
                    } else {
                        LogUtil.d(TAG, "No headers set");
                    }
                    return params;
                }

            };

            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    60000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            sRequestQueue.add(stringRequest);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mJsonobj;
    }

    public static void refreshToken(final Context cxt, final String URL, final int method, final String requestBody, final VolleyCallback callback) {
        String LOGIN_URL = Constants.getIdentityURLs(cxt, Constants.LOGIN_URL);
        JSONObject jsonBody = new JSONObject();
        try {
            final SharePref pref = SharePref.getInstance(cxt);
            User user = pref.getUser();
            jsonBody.put("email", user.getEmail());
            jsonBody.put("password", pref.getItem(Constants.PREF_PASSWORD, ""));
            LogUtil.d(TAG, "Refreshing token  email: " + user.getEmail() + " Password: " + pref.getItem(Constants.PREF_PASSWORD));
            final String request = CommonUtil.getPostDataString(jsonBody);

            VolleyCommunicationManager.getInstance().SendRequest(LOGIN_URL, Request.Method.POST, request, cxt, new VolleyCallback() {
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
                                pref.addItem(Constants.PREF_ACCESS_TOKEN, "");
                                pref.addItem(Constants.PREF_ACCESS_TOKEN, tokenStr);
                                pref.addItem(Constants.PREF_EXPIRES, expires);
                                SendRequest(URL, method, requestBody, cxt, callback);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public void onError(VolleyError error) {
                    if (error != null) {
                        if (error.networkResponse != null && error.networkResponse.statusCode == 400) {
                            SharePref sharePref = SharePref.getInstance(cxt);
                            if (sharePref.getBooleanItem(Constants.PREF_USER_LOGED_IN)) {
                                try {

                                    Intent intent = new Intent(Constants.SIGNOUT);
                                    cxt.sendBroadcast(intent);
                                    sharePref.clearPreferences();
                                    trimCache(cxt);
                                    Intent sign_in = new Intent(cxt, SignInActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    cxt.startActivity(sign_in);
                                    if (cxt instanceof Activity) {
                                        ((Activity) cxt).setResult(Constants.SIGN_OUT_RESULT_CODE);
                                        ((Activity) cxt).finish();
                                    }


                                } catch (Exception ex) {
                                    ex.getMessage();
                                }

                            }

                        }
                    }

                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
