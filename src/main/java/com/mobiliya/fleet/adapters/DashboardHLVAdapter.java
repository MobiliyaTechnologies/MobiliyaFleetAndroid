package com.mobiliya.fleet.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.activity.SettingsActivity;
import com.mobiliya.fleet.activity.TripActivity;
import com.mobiliya.fleet.activity.TripListActivity;
import com.mobiliya.fleet.activity.VehicleHealthAcitivity;
import com.mobiliya.fleet.utils.Constants;
import com.mobiliya.fleet.utils.LogUtil;

import static android.content.ContentValues.TAG;

@SuppressWarnings({"WeakerAccess", "CanBeFinal", "UnusedAssignment"})
public class DashboardHLVAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private String mProtocol = null;

    public DashboardHLVAdapter(Context context, String mProtocol) {
        super();
        this.mContext = context;
        this.mProtocol = mProtocol;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        RecyclerView.ViewHolder viewHolder = null;
        View v = null;
        switch (i) {
            case 0:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.grid_item_start_trip, viewGroup, false);
                viewHolder = new ViewHolderStartTrip(v);
                break;
            case 1:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.grid_item_vehicle_health, viewGroup, false);
                viewHolder = new ViewHolderHealth(v);
                break;
            case 2:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.grid_item_trips, viewGroup, false);
                viewHolder = new ViewHolderTrips(v);
                break;
            case 3:
                v = LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.grid_item_settings, viewGroup, false);
                viewHolder = new ViewHolderSettings(v);
                break;
        }
        return viewHolder;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return 4;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolderStartTrip) {
            ViewHolderStartTrip holder1 = (ViewHolderStartTrip) holder;

            holder1.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    Activity activity = (Activity) mContext;
                    Intent intent = new Intent(mContext, TripActivity.class);
                    mContext.startActivity(intent);
                    activity.overridePendingTransition(R.anim.enter, R.anim.leave);
                }
            });
        } else if (holder instanceof ViewHolderHealth) {
            ViewHolderHealth holder2 = (ViewHolderHealth) holder;

            holder2.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    connect();
                }
            });
        } else if (holder instanceof ViewHolderTrips) {
            ViewHolderTrips holder3 = (ViewHolderTrips) holder;

            holder3.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    Intent intent = new Intent(mContext, TripListActivity.class);
                    mContext.startActivity(intent);
                    Activity activity = (Activity) mContext;
                    activity.overridePendingTransition(R.anim.enter, R.anim.leave);
                }
            });
        } else if (holder instanceof ViewHolderSettings) {
            ViewHolderSettings holder4 = (ViewHolderSettings) holder;
            holder4.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    Intent intent = new Intent(mContext, SettingsActivity.class);
                    Activity activity = (Activity) mContext;
                    activity.startActivityForResult(intent, Constants.DASHBOARD_REQUEST_CODE);
                    activity.overridePendingTransition(R.anim.enter, R.anim.leave);
                }
            });
        }

    }

    @SuppressWarnings("deprecation")
    public static class ViewHolderStartTrip extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private ItemClickListener clickListener;

        public ViewHolderStartTrip(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.clickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(view, getPosition(), false);
        }

        @Override
        public boolean onLongClick(View view) {
            clickListener.onClick(view, getPosition(), true);
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    public static class ViewHolderHealth extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private ItemClickListener clickListener;

        public ViewHolderHealth(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.clickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(view, getPosition(), false);
        }

        @Override
        public boolean onLongClick(View view) {
            clickListener.onClick(view, getPosition(), true);
            return true;
        }
    }

    @SuppressWarnings("deprecation")
    public static class ViewHolderTrips extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener {

        private ItemClickListener clickListener;

        public ViewHolderTrips(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.clickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(view, getPosition(), false);
        }

        @Override
        public boolean onLongClick(View view) {
            clickListener.onClick(view, getPosition(), true);
            return true;
        }
    }


    @SuppressWarnings("deprecation")
    public static class ViewHolderSettings extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ItemClickListener clickListener;

        public ViewHolderSettings(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.clickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            clickListener.onClick(view, getPosition(), false);
        }

    }

    public void connect() {
        LogUtil.d(TAG, "Protocol Selected: " + mProtocol);
        Intent intent = new Intent(mContext, VehicleHealthAcitivity.class);
        intent.putExtra(Constants.PROTOCOL, mProtocol);
        mContext.startActivity(intent);
        Activity activity = (Activity) mContext;
        activity.overridePendingTransition(R.anim.enter, R.anim.leave);
    }
}

