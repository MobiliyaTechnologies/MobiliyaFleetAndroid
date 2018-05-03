package com.mobiliya.fleet.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mobiliya.fleet.R;
import com.mobiliya.fleet.models.FaultModel;

import java.util.List;

@SuppressWarnings({"ALL", "unused"})
public class EngineFaultAdapter extends RecyclerView.Adapter<EngineFaultAdapter.ViewHolder> {
    List<FaultModel> faultlist;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;

    // data is passed into the constructor
    public EngineFaultAdapter(Context context, List<FaultModel> list) {
        this.mInflater = LayoutInflater.from(context);
        //noinspection unchecked
        faultlist = list;
        Context context1 = context;
    }

    // inflates the row layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.grid_item_engine_faults_data, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        FaultModel data = faultlist.get(position);
        String codestring = data.spn;
        if (!TextUtils.isEmpty(data.fmi)) {
            codestring = codestring + "-" + data.fmi;
        }
        String unitstring = data.unit;

        if (data.spn.equalsIgnoreCase(data.description)) {
            codestring = "";
        }
        String desc = data.description;
        holder.textViewUnits.setText(codestring);
        holder.textViewStatus.setText(unitstring);
        holder.textViewDesc.setText(desc);
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return faultlist.size();
    }


    public void addListItem(List<FaultModel> list) {
        //noinspection unchecked
        faultlist = list;
        notifyDataSetChanged();
    }

    // stores and recycles views as they are scrolled off screen
    @SuppressWarnings({"CanBeFinal", "unused"})
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewUnits;
        TextView textViewStatus;
        TextView textViewDesc;

        ViewHolder(View itemView) {
            super(itemView);
            textViewUnits = itemView.findViewById(R.id.units);
            textViewStatus = itemView.findViewById(R.id.status);
            textViewDesc = itemView.findViewById(R.id.desc);
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
