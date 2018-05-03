package com.mobiliya.fleet.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.mobiliya.fleet.models.FaultModel;

import java.util.List;

@SuppressWarnings({"ALL", "unused"})
public class FaultListAdapter extends BaseAdapter {
    private List<FaultModel> faultlist;
    private Activity context;

    public FaultListAdapter(Activity activity, List<FaultModel> list) {
        this.faultlist = list;
        this.context = activity;
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return faultlist.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        return vi;
    }
}
