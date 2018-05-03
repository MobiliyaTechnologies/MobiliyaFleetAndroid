package com.mobiliya.fleet.comm;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public interface VolleyCallback {
    void onSuccess(JSONObject result);

    void onError(VolleyError result);
}
