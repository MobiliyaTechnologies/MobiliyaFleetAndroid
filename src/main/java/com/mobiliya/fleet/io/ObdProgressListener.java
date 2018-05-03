package com.mobiliya.fleet.io;

@SuppressWarnings({"ALL", "unused"})
public interface ObdProgressListener {
    void stateUpdate(final ObdCommandJob job);
}