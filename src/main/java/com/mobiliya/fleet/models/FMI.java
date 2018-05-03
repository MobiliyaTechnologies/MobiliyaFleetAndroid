package com.mobiliya.fleet.models;

/**
 * Created by aquil on 1/13/2018.
 */

@SuppressWarnings("DefaultFileTemplate")
public class FMI {
    public int Fmi = -1;
    public String Description = "";
    public String Lamp = "";

    public FMI(int fmi, String description, String lamp) {
        Fmi = fmi;
        Description = description;
        Lamp = lamp;
    }
}
