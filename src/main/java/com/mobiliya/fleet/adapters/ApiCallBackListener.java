package com.mobiliya.fleet.adapters;

import com.android.volley.VolleyError;

import org.json.JSONObject;

@SuppressWarnings({"ALL", "unused"})
public interface ApiCallBackListener {
    void onSuccess(JSONObject object);

    void onError(VolleyError result);
}
