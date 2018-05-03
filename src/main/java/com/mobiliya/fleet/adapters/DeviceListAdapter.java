package com.mobiliya.fleet.adapters;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.utils.LogUtil;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

/**
 * Adapter for available bluetooth devices list
 */

@SuppressWarnings({"ALL", "unused"})
public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    private final LayoutInflater mLayoutInflater;
    private final ArrayList<BluetoothDevice> mDevices;
    private final int mViewResourceId;
    private Context mContext;

    public DeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
        super(context, R.layout.device_adapter_view, devices);
        this.mDevices = devices;
        this.mContext = context;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = R.layout.device_adapter_view;
    }


    @SuppressLint("ViewHolder")
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);
        BluetoothDevice device = mDevices.get(position);

        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.tvDeviceName);
            TextView deviceStatus = (TextView) convertView.findViewById(R.id.tvDeviceAddress);
            LogUtil.d(TAG, "bonding state:" + device.getBondState());
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                deviceStatus.setText(this.mContext.getResources().getString(R.string.paired));
                deviceStatus.setTextColor(Color.parseColor("#577cfc"));
            } else if (device.getBondState() == BluetoothDevice.BOND_BONDING) {
                deviceStatus.setText("Pairing...");
                deviceStatus.setTextColor(Color.parseColor("#577cfc"));
            }
            if (deviceName != null) {
                deviceName.setText(device.getName());
            }
        }
        return convertView;
    }

}
