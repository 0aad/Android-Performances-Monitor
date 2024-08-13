package com.droidlogic.performancemonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RestartServiceReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        //if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) || intent.getAction().equals("RestartService")) {Log.d("RestartServiceReceiver", "Service is being restarted...");
        context.startService(new Intent(context, FloatingService.class));
        //}
    }
}


