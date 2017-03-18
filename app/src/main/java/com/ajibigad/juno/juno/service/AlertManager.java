package com.ajibigad.juno.juno.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.ajibigad.juno.juno.AlertReciever;
import com.ajibigad.juno.juno.database.AlertRepository;
import com.ajibigad.juno.juno.model.Alert;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Julius on 28/02/2017.
 */
public class AlertManager {

    private Context context;
    private AlertRepository alertRepository;

    //Creates the next alarm trigger based on alerts in the db
    //fetch the alert with next trigger time after the current time and set it in the alarm manager
    // when the alarm goes off, the broadcasted intent would be received to start the scream activity

    public AlertManager(Context context){
        this.context = context;
        alertRepository = new AlertRepository();
    }

    public void createAlarmTrigger(){
        AlarmManager alarmMgr;
        PendingIntent alarmIntent;
        alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        Date now = Calendar.getInstance().getTime();
        Alert nextAlert = alertRepository.findNextAfterTriggerTime(now);
        if(nextAlert != null){
            long triggerAtMillis;
            String ALARM_ACTION = AlertReciever.ACTION_SET_NEXT_TRIGGER_TIME;
            Intent intent = new Intent(ALARM_ACTION);
            Bundle bundle = new Bundle();
            bundle.putString("alertMessage", nextAlert.getMessage());
            bundle.putLong("alertID", nextAlert.getId());
            intent.putExtras(bundle);
            alarmIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

            if(nextAlert.isRepeat()){
                if(nextAlert.getTriggerTime().after(now)){
                    triggerAtMillis = nextAlert.getTriggerTime().getTime();
                    nextAlert.setNextTriggerTime(nextAlert.getTriggerTime());
                }
                else{
                    triggerAtMillis = nextAlert.getNextTriggerTime().getTime();
                    nextAlert.setNextTriggerTime(nextAlert.getNextTriggerTime());
                }
            }
            else{
                triggerAtMillis = nextAlert.getTriggerTime().getTime();
            }
            alarmMgr.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, alarmIntent);
        }
    }

}
