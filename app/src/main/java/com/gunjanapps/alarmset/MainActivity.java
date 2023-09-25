package com.gunjanapps.alarmset;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    private CheckBox[] checkBoxes;
    private TimePicker timePicker;
    private String selectedTime;
    private String selectedDays;
    private ActivityResultLauncher<Intent> autoStartPermissionLauncher;
    private boolean autoStartPermissionGranted=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        // Initialize an array of CheckBoxes
        checkBoxes = new CheckBox[]{
                findViewById(R.id.checkbox_sunday),
                findViewById(R.id.checkbox_monday),
                findViewById(R.id.checkbox_tuesday),
                findViewById(R.id.checkbox_wednesday),
                findViewById(R.id.checkbox_thursday),
                findViewById(R.id.checkbox_friday),
                findViewById(R.id.checkbox_saturday)
        };
        timePicker = findViewById(R.id.timePicker);
        Button setAlarm = findViewById(R.id.button);

        // Initialize the Auto Start permission launcher
        autoStartPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {

                          if(!isAutoStartPermissionEnabled()){
                              Toast.makeText(MainActivity.this, "Enable Auto-Start For Better Performance", Toast.LENGTH_SHORT).show();
                          }
                          autoStartPermissionGranted = true;
                    }
                }
        );

        setAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //get the data from the field and set it globally
                getFieldData();

                //validate fields- empty field validation
                if (!selectedDays.equals("") && !selectedTime.equals("")) {

                    if (!autoStartPermissionGranted) {
                        // Permission is not enabled, shows dialog , then request permission
                        showAutoStartPermissionDialog();
                    } else {
                        // Permission is granted, proceed with your logic
                        checkAndSetNotification();
                    }
                }
            }
        });
    }

    private boolean isAutoStartPermissionEnabled() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        switch (manufacturer) {
            case "xiaomi":
                // For Xiaomi devices, check if the specific permission is granted
                return isXiaomiAutoStartEnabled();
            case "huawei":
                // For Huawei devices, check if the specific permission is granted
                return isHuaweiAutoStartEnabled();
            default:
                // For other manufacturers or as a fallback, check if your app has the necessary permissions
                return isOtherAutoStartEnabled();
        }
    }

    private boolean isXiaomiAutoStartEnabled() {
        int enabled = Settings.Secure.getInt(getContentResolver(), "autostart", 0);
        return enabled == 1;
    }

    private boolean isHuaweiAutoStartEnabled() {
        Toast.makeText(this, "Hawai", Toast.LENGTH_SHORT).show();
        try {
            PackageManager packageManager = getPackageManager();
            ComponentName componentName = new ComponentName(getPackageName(), "com.gunjanapps.alarmset.HawaiAutoStartReceiver");
            int state = packageManager.getComponentEnabledSetting(componentName);
            return state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        } catch (Exception e) {
            return false;
        }
    }



    private boolean isOtherAutoStartEnabled() {
        Toast.makeText(this, "Give Auto-Start Permission Manually", Toast.LENGTH_SHORT).show();
        // Check if  app has any other necessary permissions to run in the background
        return true;
    }

    private void showAutoStartPermissionDialog() {
        // Function is left blank to, display a dialog to the user explaining the need for "Auto Start" permission

        openAutoStartSettings();
    }

    private void openAutoStartSettings() {
        String manufacturer = Build.MANUFACTURER.toLowerCase();
        Intent intent;

        // Intent to open the "Auto Start" settings for known manufacturers
        switch (manufacturer) {
            case "xiaomi":
                intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                break;
            case "huawei":
                intent = new Intent();
                intent.setComponent(new ComponentName("com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"));
                break;
            default:
                // For other manufacturers or as a fallback, open the app settings
                intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", getPackageName(), null));
                break;
        }
        try {
            // Launch the intent using the ActivityResultLauncher
            autoStartPermissionLauncher.launch(intent);
        }catch (Exception e){
            Toast.makeText(MainActivity.this, "Give auto start permission manually for better app functioning", Toast.LENGTH_SHORT).show();
        }

    }

    //Fetch data form the view and set the data globally
    private void getFieldData() {
        StringBuilder days = new StringBuilder();
        for (CheckBox checkBox : checkBoxes) {
            if (checkBox.isChecked()) {
                days.append(checkBox.getText()).append(",");
            }
        }
        // Remove the trailing comma and space if any
        if (days.length() > 0) {
            days.setLength(days.length() - 1);
        }
        //set the day and time data
        selectedDays = days.toString();
        selectedTime = timePicker.getHour() + ":" + timePicker.getMinute();
    }
    private void checkAndSetNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 20);
            } else {
                setAlarmForDifferentDays();
            }
        } else {
            setAlarmForDifferentDays();
        }
    }
    private void setAlarmForDifferentDays() {
        // Parse user input and create work requests
        String[] selectedDaysArr = selectedDays.split(",");
        for (String selectedDay : selectedDaysArr) {
            createWorkRequest(selectedDay, selectedTime);
        }
    }

    // Create a periodic work request based on selected day and time
    private void createWorkRequest(String selectedDay, String timeInput) {
        // Map day string to Calendar day constant
        int dayOfWeek = getDayOfWeek(selectedDay);
        if (dayOfWeek != -1) {

            // Set up the desired execution time
            int hourOfDay = Integer.parseInt(timeInput.split(":")[0]);
            int minute = Integer.parseInt(timeInput.split(":")[1]);

            // Calculate the initial delay until the next desired execution time
            long initialDelay = calculateInitialDelay(dayOfWeek, hourOfDay, minute);

            // Create input data with selected day
            Data inputData = new Data.Builder()
                    .putString("selectedDay", selectedDay)
                    .putString("selectedTime",timeInput)
                    .build();

            // Create constraints based on your requirements
            Constraints constraints = new Constraints.Builder()
                    .setRequiresCharging(false) // Adjust as needed
                    .setRequiresBatteryNotLow(false) // Adjust as needed
                    .setRequiresDeviceIdle(false)// Adjust as needed
                    .build();

            // Create a periodic work request with constraints
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(
                    ReminderWorker.class, 7, TimeUnit.DAYS)
                    .setInputData(inputData) // Pass the input data
                    .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                    .addTag(selectedDay.toUpperCase(Locale.ENGLISH))
                    .setConstraints(constraints) // Apply constraints
                    .build();

            // Enqueue the work request
            WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);

            Log.d("date_time", "Creating work request for day: " + selectedDay);
            Log.d("date_time", "Initial delay: " + initialDelay);
            Toast.makeText(this, "Work request added", Toast.LENGTH_SHORT).show();
        }
    }

    //This calculate and return, total time form now to the given time of alarm, in millisecond
    private long calculateInitialDelay(int desiredDayOfWeek, int hourOfDay, int minute) {
        Calendar currentTime = Calendar.getInstance();
        int currentDayOfWeek = currentTime.get(Calendar.DAY_OF_WEEK);

        // Calculate the days until the next desired day
        int daysUntilDesiredDay = (desiredDayOfWeek + 7 - currentDayOfWeek) % 7;

        // Create a calendar instance for the next desired day
        Calendar nextDesiredDay = (Calendar) currentTime.clone();
        nextDesiredDay.add(Calendar.DAY_OF_YEAR, daysUntilDesiredDay);

        // Set the desired time on the next desired day
        nextDesiredDay.set(Calendar.HOUR_OF_DAY, hourOfDay);
        nextDesiredDay.set(Calendar.MINUTE, minute);
        nextDesiredDay.set(Calendar.SECOND, 0);

        // Calculate the initial delay as the time difference between now and the next desired time
        return nextDesiredDay.getTimeInMillis() - currentTime.getTimeInMillis();
    }
    private int getDayOfWeek(String day) {
        switch (day) {
            case "sun":
                return Calendar.SUNDAY;
            case "mon":
                return Calendar.MONDAY;
            case "tue":
                return Calendar.TUESDAY;
            case "wed":
                return Calendar.WEDNESDAY;
            case "thu":
                return Calendar.THURSDAY;
            case "fri":
                return Calendar.FRIDAY;
            case "sat":
                return Calendar.SATURDAY;
            default:
                return -1;
        }
    }
}
