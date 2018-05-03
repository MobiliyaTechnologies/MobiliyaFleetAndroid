package com.mobiliya.fleet.utils;


import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings({"ALL", "unused"})
public class SignalLogs {

    private static final String TAG = SignalLogs.class.getName();
    private static String mFoldername = "";
    /**
     * Singleton class object initialization
     */
    @SuppressLint("StaticFieldLeak")
    private static SignalLogs ourInstance;

    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    /**
     * Object for intrinsic lock (per docs 0 length array "lighter" than a normal Object
     */

    private static final Object[] DATA_LOCK = new Object[0];

    private static int mFileCounter = 0;

    private static String mFilename = "'ValvolineParameterLogs_'yyyyMMdd";
    //ourInstance.mFoldername = new SimpleDateFormat("ValvolineParameterLogs_'yyyyMMdd").format(new Date());

    private String header;

    private static ArrayList<String> mLogBuffer = new ArrayList<>();

    /**
     * Default private Constructor
     */
    private SignalLogs() {

    }

    public void setHeaderLine(String headerLine) {
        this.header = headerLine;
    }

    public String getHeaderLine() {
        return this.header;
    }

    static List<File> subfolders;
    static File folders = null;

    /**
     * Provide instance of class
     *
     * @return @{@link SignalLogs} object
     */
    public static SignalLogs getInstance(Context context) {
        if (ourInstance == null) {
            ourInstance = new SignalLogs();
            ourInstance.mFoldername = "ValvolineParameterLogs";

            try {
                mContext = context;
                folders = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + mFoldername);
                boolean success = true;
                if (!folders.exists()) {
                    success = folders.mkdir();
                }
                if (success) {
                    Log.d(TAG, "Folder created :" + mFoldername);
                } else {
                    // Do something else on failure
                    Log.d(TAG, "Folder creation failed :");
                }
            } catch (Exception ex) {
                ex.getMessage();
            }
        }

        return ourInstance;
    }


    @SuppressLint("SimpleDateFormat")
    public void log(String logMessage) {
        if (mLogBuffer.size() >= 1) {
            new WriteFile(new SimpleDateFormat(mFilename).format(new Date()), mLogBuffer).execute();
            mLogBuffer.clear();
        }
        mLogBuffer.add(logMessage);
    }

    public static void finalLog() {
        new WriteFile(mFilename + mFileCounter, mLogBuffer).execute();
        mLogBuffer.clear();
        mFileCounter++;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "CanBeFinal", "unused", "UnusedAssignment"})
    private static class WriteFile extends AsyncTask<Void, Void, Void> {

        private FileOutputStream mFileoutputStream = null;
        private OutputStreamWriter mOutputStreamWriter = null;
        private File mLogFile = null;

        String logFilename;
        ArrayList<String> logbufferdata = new ArrayList<>();

        public WriteFile(String filename, ArrayList<String> buffer) {
            this.logFilename = filename;
            //noinspection unchecked
            this.logbufferdata = (ArrayList<String>) buffer.clone();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                //String time= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                // Do something on success
                Log.d(TAG, "Folder created :" + mFoldername);

                try {
                    if (folders == null) {
                        folders = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + mFoldername);
                    }
                    boolean success;
                    if (!folders.exists()) {
                        success = folders.mkdir();
                        Log.d(TAG, "Folder created in case deleted between data sync: " + mFoldername);
                    }
                } catch (Exception e) {
                    Log.d(TAG, "Error while Folder creation: " + mFoldername);
                    e.printStackTrace();
                }

                // Assigning File Location
                if (mLogFile == null) {
                    mLogFile = new File(Environment.getExternalStorageDirectory().getPath()
                            + File.separator + mFoldername + File.separator + logFilename + ".csv");
                }

//                Log.d(TAG, "Writing data to file" + Environment.getExternalStorageDirectory().getPath()
//                        + File.separator + mFoldername + File.separator + logFilename + ".csv");

                boolean isCreated = false;
                if (!mLogFile.exists()) {
                    mLogFile.createNewFile();
                    isCreated = true;
                }
                FileWriter fw = new FileWriter(mLogFile.getAbsoluteFile(), true);
                BufferedWriter bw = new BufferedWriter(fw);
                if (isCreated) {
                    String header = SignalLogs.ourInstance.getHeaderLine();//"VIN,RPM,Speed,MaxSpeed,HiResMaxSpeed,Distance,HiResDistance,LoResDistance,Odometer,HiResOdometer,LoResOdometer,TotalHours,IdleHours,PctLoad,PctTorque,DrvPctTorque,TorqueMode,FuelUsed,HiResFuelUsed,IdleFuelUsed,FuelRate,AvgFuelEcon,InstFuelEcon,PrimaryFuelLevel,SecondaryFuelLevel,OilTemp,OilPressure,TransTemp,akeTemp,akePressure,CoolantTemp,CoolantLevel,CoolantPressure,BrakeAppPressure,Brake1AirPressure,Brake2AirPressure,AccelPedal,ThrottlePos,BatteryPotential,SelectedGear,CurrentGear,Make,Model,SerialNo,UnitNo,EngineVIN,EngineMake,EngineModel,EngineSerialNo,EngineUnitNo,ClutchSwitch,BrakeSwitch,ParkBrakeSwitch,CruiseSetSpeed,CruiseOnOff,CruiseSet,CruiseCoast,CruiseResume,CruiseAccel,CruiseActive,CruiseState,FaultSource,FaultSPN,FaultFMI,FaultOccurrence,FaultConversion,latitude,longitude,ParameterDateTime,AdapterId,FirmwareVersion,HardwareVersion,AdapterSerialNo,HardwareType,IsKeyOn,SleepMode,LedBrightness,Message,Status";
                    bw.write(header + System.getProperty("line.separator"));

                }

                for (String value : logbufferdata) {
                    bw.write(value + System.getProperty("line.separator"));
                }
                bw.flush();
                bw.close();
                fw.close();


/*
                // creating file on particular location
                mFileoutputStream = new FileOutputStream(mLogFile);
                mOutputStreamWriter = new OutputStreamWriter(mFileoutputStream);
                String header = SignalLogs.ourInstance.getHeaderLine();//"VIN,RPM,Speed,MaxSpeed,HiResMaxSpeed,Distance,HiResDistance,LoResDistance,Odometer,HiResOdometer,LoResOdometer,TotalHours,IdleHours,PctLoad,PctTorque,DrvPctTorque,TorqueMode,FuelUsed,HiResFuelUsed,IdleFuelUsed,FuelRate,AvgFuelEcon,InstFuelEcon,PrimaryFuelLevel,SecondaryFuelLevel,OilTemp,OilPressure,TransTemp,akeTemp,akePressure,CoolantTemp,CoolantLevel,CoolantPressure,BrakeAppPressure,Brake1AirPressure,Brake2AirPressure,AccelPedal,ThrottlePos,BatteryPotential,SelectedGear,CurrentGear,Make,Model,SerialNo,UnitNo,EngineVIN,EngineMake,EngineModel,EngineSerialNo,EngineUnitNo,ClutchSwitch,BrakeSwitch,ParkBrakeSwitch,CruiseSetSpeed,CruiseOnOff,CruiseSet,CruiseCoast,CruiseResume,CruiseAccel,CruiseActive,CruiseState,FaultSource,FaultSPN,FaultFMI,FaultOccurrence,FaultConversion,latitude,longitude,ParameterDateTime,AdapterId,FirmwareVersion,HardwareVersion,AdapterSerialNo,HardwareType,IsKeyOn,SleepMode,LedBrightness,Message,Status";
                mOutputStreamWriter.append(header + System.getProperty("line.separator"));

                mOutputStreamWriter.close();
*/
            } catch (IOException e) {
                Log.e(TAG, "File write failed: " + e.toString());
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }

    }

}
