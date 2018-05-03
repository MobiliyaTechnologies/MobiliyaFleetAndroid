package com.mobiliya.fleet.models;

/**
 * Created by aquil on 1/13/2018.
 */

@SuppressWarnings({"ALL", "unused"})
public class OBDFaultCode {
    public String Spn = "";
    public String Description = "";

    public OBDFaultCode(String spn, String description) {
        Spn = spn;
        Description = description;
    }
}
