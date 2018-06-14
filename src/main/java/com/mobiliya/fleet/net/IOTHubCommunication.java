package com.mobiliya.fleet.net;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.mobiliya.fleet.db.DatabaseProvider;
import com.mobiliya.fleet.models.Parameter;
import com.mobiliya.fleet.utils.CommonUtil;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.DateUtils;
import com.mobiliya.fleet.utils.LogUtil;
import com.mobiliya.fleet.utils.SharePref;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubEventCallback;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;

import static com.mobiliya.fleet.utils.Constants.IOTURL;
import static com.mobiliya.fleet.utils.Constants.LAST_SYNC_DATE;

@SuppressWarnings({"ALL", "unused"})
public class IOTHubCommunication {

    private static final String TAG = "IOTHubCommunication";

    @SuppressLint("StaticFieldLeak")
    private static IOTHubCommunication mInstance;
    @SuppressLint("StaticFieldLeak")
    private static Context mCtx;
    public static String token;
    private static Gson gson = new Gson();
    private static final int TYPE_PARAM = 1;
    private static final int TYPE_TRIP = 2;
    private IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private DeviceClient client = null;

    private IOTHubCommunication(Context context) {
        mCtx = context;
    }

    public static synchronized IOTHubCommunication getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new IOTHubCommunication(context);
        }
        return mInstance;
    }

    public void SendMessage(Parameter[] parameters) throws URISyntaxException, IOException {

        LogUtil.d("SendMessage", "SendMessage called");
        if (client == null)
        {
            try {
                String stringC = SharePref.getInstance(mCtx).getItem(IOTURL);
                if(!TextUtils.isEmpty(stringC)) {
                    client = new DeviceClient(stringC, protocol);
                    LogUtil.i("SendMessage", "SendMessage called string: " + stringC);
                }else {
                    LogUtil.i("SendMessage", "SendMessage called string: null");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }
        try {
            client.open();
            for (Parameter param : parameters) {
                param.ParameterDateTime = DateUtils.getLocalTimeString();
                param.VehicleRegNumber = SharePref.getInstance(mCtx).getVehicleData().getRegistrationNo();
                String jsonString = CommonUtil.getVehicleAndParameterJson(param, mCtx);

                LogUtil.d(TAG, "Json Send IOTHub :" + jsonString);
                Message msg = new Message(jsonString);

                HashMap<String, String> hashMap = new HashMap<>();
                hashMap.put("ID",String.valueOf(param.ID));
                hashMap.put("DateTime",param.ParameterDateTime);
                msg.setMessageId(java.util.UUID.randomUUID().toString());
                EventCallback eventCallback = new EventCallback();
                client.sendEventAsync(msg, eventCallback, hashMap);

                LogUtil.d(TAG, new Date() + " : Message Sent over IoT : Parameter RPM: " + param.RPM);
                LogUtil.d(TAG, "Parameter Speed: " + param.Speed);
                LogUtil.d(TAG, "Parameter Distance: " + param.Distance);
            }

        } catch (IOException e1) {
            LogUtil.d(TAG, "Exception while opening IoTHub connection: " + e1.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        client.closeNow();
    }

    @SuppressWarnings("unused")
    protected static class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object param) {
            try {
                /*Parameter parameter = (Parameter) param;
                Long paramId = Long.parseLong(parameter.ID.toString());
                LogUtil.d(TAG, "Sync date:" + parameter.ParameterDateTime);
                SharePref.getInstance(mCtx).addItem(LAST_SYNC_DATE, parameter.ParameterDateTime);

                if (paramId > 0) {
                    LogUtil.d(TAG, "Succesfully send data on IOT & entry deleted :" + paramId);
                    DatabaseProvider.getInstance(mCtx).deleteParameter(paramId);
                }*/
                HashMap<String, String> hashMap = (HashMap<String, String>)param;

                String ID = hashMap.get("ID");
                Long paramId = Long.valueOf(ID);

                //LogUtil.d(TAG, "Sync date:" + parameter.ParameterDateTime);
                SharePref.getInstance(mCtx).addItem(LAST_SYNC_DATE, hashMap.get("DateTime"));

                if (paramId > 0) {
                    LogUtil.d(TAG, "Succesfully send data on IOT & entry deleted :" + paramId);
                    DatabaseProvider.getInstance(mCtx).deleteParameter(paramId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
