package com.mobiliya.fleet.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.activity.TripDetailsActivity;
import com.mobiliya.fleet.models.Trip;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.DateUtils;

import java.util.List;

public class TripListAdapter extends RecyclerView.Adapter<TripListAdapter.ViewHolder> {

    private List<Trip> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mCxt;

    // data is passed into the constructor
    public TripListAdapter(Context context, List<Trip> map, ItemClickListener clickListner) {
        this.mInflater = LayoutInflater.from(context);
        mData = map;
        mCxt = context;
        mClickListener = clickListner;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.trip_list_item, parent, false);

        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Trip entry = (Trip) mData.get(position);
        String start="", end="";
        try {
            start = entry.startLocation.split("#")[1];
            end = entry.endLocation;
            if (!"NA".equals(end)) {
                end = entry.endLocation.split("#")[1];
            }
        } catch (Exception ex) {
            ex.getMessage();
        }
        holder.mStartLocation.setText(start);
        holder.mEndLocation.setText(end);

        holder.mStarttime.setText(DateUtils.tripDetailFormat(entry.startTime));
        holder.mEndTime.setText(DateUtils.tripDetailFormat(entry.endTime));

        float miles = Float.valueOf(entry.milesDriven);
        //int dist = (int) Math.ceil(miles);
        if (!"NA".equals(entry.milesDriven)) {
            if (miles < 0) {
                holder.textViewMiles.setText("0.0 Miles");
            } else {
                holder.textViewMiles.setText(String.format("%.1f", miles) + " Miles");
            }
        } else {
            holder.textViewMiles.setText("NA");
        }

        if (!"NA".equals(entry.tripDuration)) {
            holder.textViewTime.setText(entry.tripDuration);
        }

        int switchcase = position % 3;

        switch (switchcase) {
            case 0:
                holder.linearLayout.setBackgroundResource(R.drawable.trip_list_gradient_blue);
                break;
            case 1:
                holder.linearLayout.setBackgroundResource(R.drawable.trip_list_gradient_pink);
                break;
            case 2:
                holder.linearLayout.setBackgroundResource(R.drawable.trip_list_gradient_green);
                break;
        }
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void notifyDataChanged(List<Trip> list) {
        mData.clear();
        mData = list;
        notifyDataSetChanged();
    }

    // stores and recycles views as they are scrolled off screen
    @SuppressWarnings({"CanBeFinal", "unused", "RedundantCast"})
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewMiles;
        TextView textViewTime;
        TextView mStartLocation;
        TextView mEndLocation;
        TextView mStarttime;
        TextView mEndTime;
        LinearLayout linearLayout;

        ViewHolder(View itemView) {
            super(itemView);
            mStartLocation = itemView.findViewById(R.id.tv_startlocation);
            mEndLocation = itemView.findViewById(R.id.tv_endlocation);
            mStarttime = itemView.findViewById(R.id.tv_starttime);
            mEndTime = itemView.findViewById(R.id.tv_endttime);

            textViewMiles = itemView.findViewById(R.id.tv_miles);
            textViewTime = itemView.findViewById(R.id.tv_time);
            linearLayout = itemView.findViewById(R.id.ll_background);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            Trip tripdata = (Trip) mData.get(getAdapterPosition());
            String id = tripdata.commonId;
            Intent startdetailsPage = new Intent(mCxt, TripDetailsActivity.class);
            startdetailsPage.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startdetailsPage.putExtra(Constants.TRIPID, id);
            Activity activity = (Activity) mCxt;
            activity.startActivityForResult(startdetailsPage, Constants.DASHBOARD_REQUEST_CODE);
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
