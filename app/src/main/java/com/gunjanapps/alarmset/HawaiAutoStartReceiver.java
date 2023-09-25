package com.gunjanapps.alarmset;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


public class HawaiAutoStartReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // This method is called when the BroadcastReceiver receives an intent.
        // You can implement your custom logic here.
        // For demonstration purposes, we'll display a toast message.
        // Replace this with your actual auto-start logic.

        String action = intent.getAction();
        if (action != null && action.equals("android.intent.action.BOOT_COMPLETED")) {
            // The device has just booted up
            // Add your auto-start logic here
        }

        // Display a toast message (replace with your logic)
        Toast.makeText(context, "AutoStartReceiver triggered", Toast.LENGTH_SHORT).show();
    }
}
