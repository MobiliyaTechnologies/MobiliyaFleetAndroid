package com.mobiliya.fleet.models;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class FaultModel implements Comparable<FaultModel> {
    public int ID;
    public String fmi;
    public String spn;
    public String unit;
    public String description;
    public String currentDateTime;

    public FaultModel(String FMI, String SPN, String UNIT, String DESC, String CURRENT_DATE) {
        this.fmi = FMI;
        this.spn = SPN;
        this.unit = UNIT;
        this.description = DESC;
        this.currentDateTime = CURRENT_DATE;
    }

    public Date getDateTime() {
        Date dateTime = null;
        if (this.currentDateTime != null && !this.currentDateTime.equalsIgnoreCase("")) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            try {
                dateTime = format.parse(this.currentDateTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return dateTime;
    }

    @Override
    public int compareTo(@NonNull FaultModel o) {
        if (getDateTime() != null && o.getDateTime() != null)
            return getDateTime().compareTo(o.getDateTime());
        else
            return 0;

    }
}
