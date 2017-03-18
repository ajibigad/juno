package com.ajibigad.juno.juno;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Julius on 28/02/2017.
 */
public class AlertReciever extends BroadcastReceiver {

    public static String ACTION_SET_NEXT_TRIGGER_TIME = "com.ajibigad.juno.ACTION_SET_NEXT_TRIGGER_TIME";

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent alertScreamerIntent = new Intent(context, AlertScreamerActivity.class);
        alertScreamerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alertScreamerIntent.putExtras(intent.getExtras());
        context.startActivity(alertScreamerIntent);
    }
}
