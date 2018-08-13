package com.mobiliya.fleet.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;

import java.util.ArrayList;
import java.util.Map;

public class VehicleDataAdapter extends RecyclerView.Adapter<VehicleDataAdapter.ViewHolder> {

    private ArrayList<Map.Entry<String, String>> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    // data is passed into the constructor
    public VehicleDataAdapter(Context context, Map<String, String> map) {
        this.mInflater = LayoutInflater.from(context);
        //noinspection unchecked
        mData = new ArrayList(map.entrySet());
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.grid_item_vehicle_health_data, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        @SuppressWarnings("unchecked") Map.Entry<String, String> entry = (Map.Entry) mData.get(position);

        holder.textViewUnits.setText(entry.getKey());
        holder.textViewSignalType.setText(entry.getValue());
        if(Constants.ADDITIONAL_PARAMETERS.equalsIgnoreCase(entry.getKey())
                ||Constants.VEHICLE_PARAMETERS.equalsIgnoreCase(entry.getKey())) {
            LogUtil.d("Kunal","set font bold");
            holder.textViewSignalType.setTypeface(null, Typeface.BOLD);
            holder.textViewUnits.setTypeface(null, Typeface.BOLD);
            holder.textViewUnits.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            holder.textViewSignalType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            holder.textViewUnits.setTextColor(Color.parseColor("#4a4a4a"));
            holder.textViewSignalType.setTextColor(Color.parseColor("#4a4a4a"));
        }

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }


    public void addListItem(Map<String, String> map) {
        //noinspection unchecked
        mData = new ArrayList(map.entrySet());
        notifyDataSetChanged();
    }

    // stores and recycles views as they are scrolled off screen
    @SuppressWarnings({"CanBeFinal", "unused"})
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewUnits;
        TextView textViewSignalType;

        ViewHolder(View itemView) {
            super(itemView);
            textViewSignalType = itemView.findViewById(R.id.units);
            textViewUnits = itemView.findViewById(R.id.signalType);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
        }
    }


    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    @SuppressWarnings("unused")
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
