package com.mobiliya.fleet.adapters;

import com.android.volley.VolleyError;

import org.json.JSONObject;

public interface ApiCallBackListener {
    void onSuccess(JSONObject object);

    void onError(VolleyError result);
}
