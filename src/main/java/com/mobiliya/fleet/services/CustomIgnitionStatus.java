package com.mobiliya.fleet.services;

public class CustomIgnitionStatus {

    private static CustomIgnitionStatus sInstance;

    public static CustomIgnitionStatus getsInstance() {
        if (sInstance == null)
            sInstance = new CustomIgnitionStatus();
        return sInstance;
    }

    private CustomIgnitionStatusInterface listener;

    private CustomIgnitionStatus() {
    }

    public CustomIgnitionStatusInterface getInterface() {
        return listener;
    }

    public void setListener(CustomIgnitionStatusInterface listener) {
        this.listener = listener;
    }

}
