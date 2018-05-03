package com.mobiliya.fleet.models;

@SuppressWarnings({"ALL", "unused"})
public class PGN {
    String PGN;

    public String getPGN() {
        return PGN;
    }

    public void setPGN(String PGN) {
        this.PGN = PGN;
    }

    public String getLocation() {
        return Location;
    }

    public void setLocation(String location) {
        Location = location;
    }

    public String getScaling() {
        return Scaling;
    }

    public void setScaling(String scaling) {
        Scaling = scaling;
    }

    public String getOffset() {
        return Offset;
    }

    public void setOffset(String offset) {
        Offset = offset;
    }

    public String getUnits() {
        return Units;
    }

    public void setUnits(String units) {
        Units = units;
    }

    public String getParameterName() {
        return ParameterName;
    }

    public void setParameterName(String parameterName) {
        ParameterName = parameterName;
    }

    public double getActualValues() {
        return actualValues;
    }

    public void setActualValues(double actualValues) {
        this.actualValues = actualValues;
    }

    public double getRawValue() {
        return RawValue;
    }

    public void setRawValue(double rawValue) {
        RawValue = rawValue;
    }

    String Location;
    String Scaling;
    String Offset;
    String Units;
    String ParameterName;
    double actualValues = -1;
    double RawValue = -1;

    public String getHexRawValue() {
        return hexRawValue;
    }

    public void setHexRawValue(String hexRawValue) {
        this.hexRawValue = hexRawValue;
    }

    String hexRawValue;

    public PGN(String PGN, String Location, String Scaling, String Offset, String Units, String ParameterName) {

        this.PGN = PGN;
        this.Location = Location;
        this.Scaling = Scaling;
        this.Offset = Offset;
        this.Units = Units;
        this.ParameterName = ParameterName;
    }


}
