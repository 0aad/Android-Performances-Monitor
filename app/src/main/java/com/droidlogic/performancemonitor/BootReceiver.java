package com.droidlogic.performancemonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Toast.makeText(context, "Boot completed - starting service", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(context, FloatingService.class);
            context.startService(serviceIntent);
        }
    }
}


