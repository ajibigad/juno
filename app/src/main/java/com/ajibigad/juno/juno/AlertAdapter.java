package com.ajibigad.juno.juno;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ajibigad.juno.juno.model.Alert;

import java.text.SimpleDateFormat;
import java.util.List;

import io.realm.RealmResults;

/**
 * Created by Julius on 26/02/2017.
 */
public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.ViewHolder> {

    RealmResults<Alert> alerts;
    private static SimpleDateFormat dateFormat;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView triggerTime;
        public TextView message;
        //public SwitchCompat isEnabled;

        public ViewHolder(View alertView) {
            super(alertView);
            triggerTime = (TextView) alertView.findViewById(R.id.trigger_time);
            message = (TextView) alertView.findViewById(R.id.alert_message);
        }
    }

    static{
        dateFormat = new SimpleDateFormat("hh:mm:a");
    }

    public AlertAdapter(RealmResults<Alert> alerts){
        this.alerts = alerts;
    }

    @Override
    public AlertAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Alert alert = alerts.get(position);
        holder.triggerTime.setText(dateFormat.format(alert.getTriggerTime()));
        holder.message.setText(alert.getMessage());
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }
}
