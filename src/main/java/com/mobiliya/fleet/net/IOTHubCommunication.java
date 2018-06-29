package com.mobiliya.fleet.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.BaseParameters;
import com.mobiliya.fleet.models.Parameter;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.DateUtils;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static com.mobiliya.fleet.utils.Constants.IOTURL;
import static com.mobiliya.fleet.utils.Constants.LAST_SYNC_DATE;

//@SuppressWarnings({"ALL", "unused"})
public class IOTHubCommunication {

    private static final String TAG = "IOTHubCommunication";

    @SuppressLint("StaticFieldLeak")
    private static IOTHubCommunication mInstance;
    private Context mCtx;
    private IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private static DeviceClient client = null;

    private IOTHubCommunication(Context context) {
        mCtx = context;
    }

    public static synchronized IOTHubCommunication getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new IOTHubCommunication(context);

            String stringC = SharePref.getInstance(context).getItem(IOTURL);
            if (!TextUtils.isEmpty(stringC)) {
                try {
                    client = new DeviceClient(stringC, IotHubClientProtocol.MQTT);
                    client.open();
                } catch (URISyntaxException | IOException e) {
                    e.printStackTrace();
                }
            } else {
                LogUtil.i(TAG, "Connection String is null");
            }
        }
        return mInstance;
    }

    private BaseParameters convertToBaseParameterObject(Parameter param) {
        BaseParameters p = new BaseParameters();

        p.ID = param.ID;
        p.TripId = param.TripId;
        p.TenantId = param.TenantId;
        p.UserId = param.UserId;
        p.VehicleId = param.VehicleId;
        p.RPM = param.RPM;
        p.Speed = param.Speed;
        p.Distance = param.Distance;
        p.FuelUsed = param.FuelUsed;
        p.OilTemp = param.OilTemp;
        p.IntakePressure = param.IntakePressure;
        p.AccelPedal = param.AccelPedal;
        p.EngineVIN = param.EngineVIN;
        p.EngineSerialNo = param.EngineSerialNo;
        p.ClutchSwitch = param.ClutchSwitch;
        p.BrakeSwitch = param.BrakeSwitch;
        p.FaultSPN = param.FaultSPN;
        p.Latitude = param.Latitude;
        p.Longitude = param.Longitude;
        p.ParameterDateTime = param.ParameterDateTime;
        p.AmbientTemp = param.AmbientTemp;
        p.EngineIntakeManifoldPressure = param.EngineIntakeManifoldPressure;
        p.EngineCrankcasePressure = param.EngineCrankcasePressure;
        p.BarometricPressure = param.BarometricPressure;
        p.FaultDescription = param.FaultDescription;
        p.isConnected = param.isConnected;
        p.VehicleRegNumber = param.VehicleRegNumber;
        p.AvgFuelEcon = param.AvgFuelEcon;
        p.AirIntakeTemperature = param.AirIntakeTemperature;
        p.DiagonosticTroubleCodes = param.DiagonosticTroubleCodes;
        return p;
    }

    public void SendMessage(Parameter[] parameters) {

        LogUtil.d("SendMessage", "SendMessage called");

        try {
            if (client == null) {
                String stringC = SharePref.getInstance(mCtx).getItem(IOTURL);
                if (!TextUtils.isEmpty(stringC)) {
                    client = new DeviceClient(stringC, protocol);
                    client.open();
                    LogUtil.i("SendMessage", "SendMessage called string: " + stringC);
                } else {
                    LogUtil.i("SendMessage", "SendMessage called string: null");
                    return;
                }
            }

            BaseParameters param;
            for (Parameter paramobj : parameters) {
                param = convertToBaseParameterObject(paramobj);

                param.ParameterDateTime = DateUtils.getLocalTimeString();
                param.VehicleRegNumber = SharePref.getInstance(mCtx).getVehicleData().getRegistrationNo();
                String jsonString = CommonUtil.getVehicleAndParameterJson(param, mCtx);

                LogUtil.d(TAG, "Json Send IOTHub :" + jsonString);
                Message msg = new Message(jsonString);

                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("ID", String.valueOf(param.ID));
                hashMap.put("DateTime", param.ParameterDateTime);

                msg.setMessageId(java.util.UUID.randomUUID().toString());
                EventCallback eventCallback = new EventCallback();

                client.sendEventAsync(msg, eventCallback, hashMap);

                // Wait for IoT Hub to respond.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } catch (IllegalStateException e) {
            LogUtil.d(TAG, "IllegalStateException: while sending IOT data, so try to open connection");
            try {
                if (client != null) {
                    client.open();
                }
            } catch (Exception ep) {
                LogUtil.d(TAG, "Exception while opening client");
                ep.printStackTrace();
            }
        } catch (IOException e1) {
            LogUtil.d(TAG, "Exception while opening IoTHub connection: " + e1.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void closeClient() {
        try {
            if (client != null) {
                LogUtil.d(TAG, "closeClient");
                client.closeNow();
                client = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object param) {
            LogUtil.d(TAG, "IOT status " + status);
            LogUtil.d(TAG, "IOT data " + param);
            try {
                if (IotHubStatusCode.OK_EMPTY.equals(status) || IotHubStatusCode.OK.equals(status)) {
                    HashMap<String, String> hashMap = (HashMap<String, String>) param;
                    String ID = hashMap.get("ID");
                    Long paramId = Long.valueOf(ID);
                    SharePref.getInstance(mCtx).addItem(LAST_SYNC_DATE, hashMap.get("DateTime"));
                    if (paramId > 0) {
                        LogUtil.d(TAG, "Successfully send data on IOT & entry deleted :" + paramId);
                        DatabaseProvider.getInstance(mCtx).deleteParameter(paramId);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
