package com.gunjanapps.alarmset;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/*
Created By- Bishal Maji
Date- 05/09/2023
 */
public class ReminderWorker extends Worker {
    private final Context context;

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        // Get the selected day from input data
        String selectedDay = getInputData().getString("selectedDay");
        String selectedTime = getInputData().getString("selectedTime");

            showNotification(selectedDay,selectedTime);

        return Result.success();
    }


    private void showNotification(String dayOfWeek, String selectedTime) {
        NotificationUtils.createNotificationChannel(context);
        // Set the default notification sound
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Set vibration pattern
        long[] vibratePattern = {0, 500, 500, 500}; // Vibrate for 200 milliseconds, pause for 200 milliseconds, repeat

        // Build the notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NotificationUtils.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Reminder")
                .setContentText("Notification created on Day= " + dayOfWeek+" Time= "+selectedTime)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(defaultSoundUri)
                .setVibrate(vibratePattern);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        notificationManager.notify(NotificationUtils.NOTIFICATION_ID, notificationBuilder.build());
        Log.d("work_tag", "Notification shown for the scheduled day: " + dayOfWeek);
    }

}
