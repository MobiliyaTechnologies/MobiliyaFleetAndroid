package com.mobiliya.fleet.utils;


public enum TripStatus {
    Start(1), Stop(0), Pause(2);

    private final int value;

    TripStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

}
