package com.mobiliya.fleet.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;

import com.mobiliya.fleet.models.FMI;
import com.mobiliya.fleet.models.FaultModel;
import com.mobiliya.fleet.models.SPN;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


@SuppressWarnings({"UnnecessaryLocalVariable", "WeakerAccess", "UnusedAssignment"})
public class SPNData {

    @SuppressLint("StaticFieldLeak")
    private static SPNData instance = null;
    @SuppressLint("StaticFieldLeak")
    private static Context ctx = null;

    public static SPNData getInstance(Context context) {
        ctx = context;
        if (instance == null)
            instance = new SPNData();
        return instance;
    }

    public FaultModel getError(int spn, int fmi) {
        String message = "";
        SPN spnFound = null;
        try {
            message = getProperty(String.valueOf(spn), ctx);
            spnFound = new SPN(spn, message);
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<FMI> fmis = getAllFMI();
        FMI fmiFound = null;
        for (FMI f : fmis) {
            if (f.Fmi == fmi) {
                fmiFound = f;
                break;
            }
        }

        String spnDesc = "";
        if (spnFound != null)
            spnDesc = spnFound.Description;

        String fmiDesc = "";
        String fmiUnit = "";
        if (fmiFound != null) {
            fmiDesc = fmiFound.Description;
            fmiUnit = fmiFound.Lamp;
        }

        String desc = spnDesc + (fmiDesc.equals("") ? "" : ("; " + fmiDesc));

        FaultModel fault = new FaultModel(String.valueOf(fmi), String.valueOf(spn), fmiUnit, desc, DateUtils.getLocalTimeString());

        return fault;
    }

    public List<FMI> getAllFMI() {
        List<FMI> fmis = Arrays.asList(
                new FMI(0, "Data Valid but Above Normal Operational Range, Most Severe Level", "S"),
                new FMI(1, "Data Valid but Below Normal Operational Range, Most Severe Level", "S"),
                new FMI(2, "Data Erratic, Intermittent or Incorrect (rationality)", "W"),
                new FMI(3, "Voltage Above Normal, or Shorted to High Source", "W"),
                new FMI(4, "Voltage Below Normal, or Shorted to High Source", "W"),
                new FMI(5, "Current Below Normal, or Open Circuit", "W"),
                new FMI(6, "Current Above Normal, or Grounded Circuit", "W"),
                new FMI(7, "Mechanical System not Responding or Out of Adjustment", "W"),
                new FMI(8, "Abnormal Frequency or Pulse Width or Period", "W"),
                new FMI(9, "Abnormal Update Rate", "W"),
                new FMI(10, "Abnormal Rate of Change", "W"),
                new FMI(11, "Failure Code not Identifiable", "M"),
                new FMI(12, "Bad Intelligent Device or Component", "M"),
                new FMI(13, "Out of Calibration", "M"),
                new FMI(14, "Special Instructions", "W"),
                new FMI(15, "Data Valid but Above Normal Range : Least Severe Level", "W"),
                new FMI(16, "Data Valid but Above Normal Range: Moderately Severe Level", "M"),
                new FMI(17, "Data Valid but Below Normal Range: Least Severe Level", "W"),
                new FMI(18, "Data Valid but Below Normal Range: Moderately Severe Level", "M"),
                new FMI(19, "Received Network Data in Error: (Multiplexed Data)", "W"),
                new FMI(20, "Data Drifted High (rationality high)", "S"),
                new FMI(21, "Data Drifted Low (rationality low)", "S"),
                new FMI(31, "Condition Exists", "W")
        );
        return fmis;
    }

    /*public List<SPN> getAllSPN(){


        List<SPN> spns = Arrays.asList(
                new SPN(16,"Engine Fuel Filter (suction side) Differential Pressure"),
                new SPN(18,"Engine Extended Range Fuel Pressure")
        );
        return spns;
    }*/

    public String getProperty(String key, Context context) throws IOException {
        Properties properties = new Properties();
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open("spnJ1939.properties");
        properties.load(inputStream);
        return properties.getProperty(key);

    }

}
