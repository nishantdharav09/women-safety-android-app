package com.example.wsafety;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupWindow;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.womensafety.R;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int MAX_CONTACTS = 5;

    private ActivityResultLauncher<String[]> multiplePermissions;
    private ShakeDetector shakeDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Permission launcher
        multiplePermissions = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {

                    boolean allGranted = true;

                    for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                        if (!Boolean.TRUE.equals(entry.getValue())) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        startSafetyService();
                    } else {
                        Snackbar.make(
                                findViewById(android.R.id.content),
                                "Required permissions are not granted.",
                                Snackbar.LENGTH_LONG
                        ).show();
                    }
                }
        );

        // Location check
        if (!isLocationEnabled()) {
            showLocationSettingsDialog();
        } else {
            requestRequiredPermissions();
        }

        // Notification channel
        createNotificationChannel();

        // Load saved contacts
        loadSavedNumbers();

        // Shake detector
        try {
            shakeDetector = new ShakeDetector(
                    this,
                    this::sendSOSMessage
            );

            shakeDetector.register();

        } catch (Exception e) {
            Log.e(
                    "SHAKE_ERROR",
                    "Shake detector could not start",
                    e
            );
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh saved contacts when returning
        // from Change Emergency Numbers screen.
        loadSavedNumbers();
    }

    @Override
    protected void onDestroy() {

        if (shakeDetector != null) {
            try {
                shakeDetector.unregister();
            } catch (Exception e) {
                Log.e(
                        "SHAKE_ERROR",
                        "Shake detector unregister failed",
                        e
                );
            }
        }

        super.onDestroy();
    }

    // ==================================================
    // LOCATION
    // ==================================================

    private boolean isLocationEnabled() {

        LocationManager locationManager =
                (LocationManager) getSystemService(
                        Context.LOCATION_SERVICE
                );

        return locationManager != null
                && locationManager.isProviderEnabled(
                LocationManager.GPS_PROVIDER
        );
    }

    private void showLocationSettingsDialog() {

        new AlertDialog.Builder(this)
                .setTitle("Location Services Required")
                .setMessage(
                        "Please turn on location services to use the safety features."
                )
                .setPositiveButton(
                        "OK",
                        (dialog, which) -> {

                            Intent intent =
                                    new Intent(
                                            Settings.ACTION_LOCATION_SOURCE_SETTINGS
                                    );

                            startActivity(intent);
                        }
                )
                .setNegativeButton(
                        "Exit",
                        (dialog, which) -> finish()
                )
                .setCancelable(false)
                .show();
    }

    // ==================================================
    // PERMISSIONS
    // ==================================================

    private void requestRequiredPermissions() {

        List<String> permissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(
                    Manifest.permission.SEND_SMS
            );
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(
                    Manifest.permission.ACCESS_FINE_LOCATION
            );
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            permissions.add(
                    Manifest.permission.ACCESS_COARSE_LOCATION
            );
        }

        if (!permissions.isEmpty()) {

            multiplePermissions.launch(
                    permissions.toArray(
                            new String[0]
                    )
            );

        } else {

            startSafetyService();
        }
    }

    // ==================================================
    // SAFETY SERVICE
    // ==================================================

    private void startSafetyService() {

        boolean smsGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.SEND_SMS
                ) == PackageManager.PERMISSION_GRANTED;

        boolean fineLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseLocationGranted =
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!smsGranted
                || !fineLocationGranted
                || !coarseLocationGranted) {

            showToast(
                    "Please grant all required permissions."
            );

            return;
        }

        try {

            Intent serviceIntent =
                    new Intent(
                            MainActivity.this,
                            ServiceMine.class
                    );

            serviceIntent.setAction("Start");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                startForegroundService(
                        serviceIntent
                );

            } else {

                startService(
                        serviceIntent
                );
            }

            Snackbar.make(
                    findViewById(android.R.id.content),
                    "Safety Service Started",
                    Snackbar.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            Log.e(
                    "SERVICE_ERROR",
                    "Unable to start Safety Service",
                    e
            );

            showToast(
                    "Unable to start Safety Service."
            );
        }
    }

    // ==================================================
    // NOTIFICATION CHANNEL
    // ==================================================

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            "MYID",
                            "Women Safety Service",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );

            channel.setDescription(
                    "Women Safety foreground service"
            );

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    Context.NOTIFICATION_SERVICE
                            );

            if (manager != null) {
                manager.createNotificationChannel(
                        channel
                );
            }
        }
    }

    // ==================================================
    // SAVED CONTACTS
    // ==================================================

    private void loadSavedNumbers() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "MySharedPref",
                        MODE_PRIVATE
                );

        StringBuilder numbersText =
                new StringBuilder(
                        "SOS Will Be Sent To:"
                );

        boolean foundNumber = false;

        for (int i = 0; i < MAX_CONTACTS; i++) {

            String number =
                    preferences.getString(
                            "ENUM" + i,
                            ""
                    );

            if (number != null
                    && !number.trim().isEmpty()) {

                numbersText
                        .append("\n")
                        .append(number);

                foundNumber = true;
            }
        }

        if (!foundNumber) {

            numbersText.append(
                    "\nNo emergency contacts saved."
            );
        }

        TextView textView =
                findViewById(
                        R.id.textNum
                );

        if (textView != null) {

            textView.setText(
                    numbersText.toString()
            );
        }
    }

    // ==================================================
    // SHAKE → SOS
    // ==================================================

    private void sendSOSMessage() {

        Log.d(
                "SOS",
                "Shake detected! Sending SOS..."
        );

        SharedPreferences preferences =
                getSharedPreferences(
                        "MySharedPref",
                        MODE_PRIVATE
                );

        boolean sentAtLeastOne = false;

        for (int i = 0; i < MAX_CONTACTS; i++) {

            String phoneNumber =
                    preferences.getString(
                            "ENUM" + i,
                            ""
                    );

            if (phoneNumber != null
                    && !phoneNumber.trim().isEmpty()) {

                sendSMS(
                        phoneNumber.trim()
                );

                sentAtLeastOne = true;
            }
        }

        if (!sentAtLeastOne) {

            showToast(
                    "No emergency contacts saved!"
            );
        }
    }

    private void sendSMS(String phoneNumber) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED) {

            showToast(
                    "SMS permission is required."
            );

            return;
        }

        try {

            String message =
                    "SOS! I may be in danger. Please help me. "
                            + "My location will be shared by the safety service.";

            SmsManager smsManager =
                    SmsManager.getDefault();

            smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    null,
                    null
            );

            Log.d(
                    "SOS",
                    "SMS sent to: " + phoneNumber
            );

            showToast(
                    "SOS sent to " + phoneNumber
            );

        } catch (Exception e) {

            Log.e(
                    "SOS_ERROR",
                    "SMS sending failed",
                    e
            );

            showToast(
                    "Failed to send SOS message."
            );
        }
    }

    // ==================================================
    // START SERVICE BUTTON
    // ==================================================

    public void startServiceV(View view) {

        requestRequiredPermissions();
    }

    // ==================================================
    // EMERGENCY NUMBERS BUTTON
    // ==================================================

    public void showEmergencyNumbers(View view) {

        try {

            Intent intent =
                    new Intent(
                            MainActivity.this,
                            EmergencyNumbersActivity.class
                    );

            startActivity(intent);

        } catch (Exception e) {

            Log.e(
                    "NAVIGATION_ERROR",
                    "Emergency Numbers activity failed",
                    e
            );

            showToast(
                    "Unable to open Emergency Numbers."
            );
        }
    }

    // ==================================================
    // CUSTOM MENU
    // ==================================================

    public void PopupMenu(View view) {

        LinearLayout menuLayout =
                new LinearLayout(this);

        menuLayout.setOrientation(
                LinearLayout.VERTICAL
        );

        menuLayout.setPadding(
                dp(8),
                dp(8),
                dp(8),
                dp(8)
        );

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                Color.WHITE
        );

        background.setCornerRadius(
                dp(20)
        );

        background.setStroke(
                dp(1),
                Color.parseColor("#E4E6EC")
        );

        menuLayout.setBackground(
                background
        );

        // TITLE
        TextView title =
                createMenuTitle(
                        "Safety Menu"
                );

        // CHANGE NUMBER
        TextView changeNumbers =
                createMenuItem(
                        "Change Emergency Numbers"
                );

        // INSTRUCTIONS
        TextView instructions =
                createMenuItem(
                        "Safety Instructions"
                );

        menuLayout.addView(title);
        menuLayout.addView(changeNumbers);
        menuLayout.addView(instructions);

        PopupWindow popupWindow =
                new PopupWindow(
                        menuLayout,
                        dp(300),
                        dp(165),
                        true
                );

        popupWindow.setBackgroundDrawable(
                new ColorDrawable(
                        Color.TRANSPARENT
                )
        );

        popupWindow.setOutsideTouchable(
                true
        );

        popupWindow.setFocusable(
                true
        );

        popupWindow.setElevation(
                dp(12)
        );

        // ----------------------------------------------
        // Change Emergency Numbers
        // ----------------------------------------------

        changeNumbers.setOnClickListener(v -> {

            popupWindow.dismiss();

            try {

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                RegisterNumberActivity.class
                        );

                startActivity(
                        intent
                );

            } catch (Exception e) {

                Log.e(
                        "MENU_ERROR",
                        "Register activity failed",
                        e
                );

                showToast(
                        "Unable to open Emergency Numbers settings."
                );
            }
        });

        // ----------------------------------------------
        // Safety Instructions
        // ----------------------------------------------

        instructions.setOnClickListener(v -> {

            popupWindow.dismiss();

            try {

                Intent intent =
                        new Intent(
                                MainActivity.this,
                                InstructionActivity.class
                        );

                startActivity(
                        intent
                );

            } catch (Exception e) {

                Log.e(
                        "MENU_ERROR",
                        "Instruction activity failed",
                        e
                );

                showToast(
                        "Unable to open Safety Instructions."
                );
            }
        });

        // Show menu
        popupWindow.showAsDropDown(
                view,
                -dp(220),
                -dp(5)
        );
    }

    // ==================================================
    // MENU TITLE
    // ==================================================

    private TextView createMenuTitle(
            String text
    ) {

        TextView textView =
                new TextView(this);

        textView.setText(
                text
        );

        textView.setTextSize(
                13
        );

        textView.setTextColor(
                Color.parseColor(
                        "#8A8F9D"
                )
        );

        textView.setGravity(
                Gravity.CENTER_VERTICAL
        );

        textView.setTypeface(
                null,
                android.graphics.Typeface.BOLD
        );

        textView.setPadding(
                dp(16),
                dp(10),
                dp(16),
                dp(8)
        );

        return textView;
    }

    // ==================================================
    // MENU ITEM
    // ==================================================

    private TextView createMenuItem(
            String text
    ) {

        TextView textView =
                new TextView(this);

        textView.setText(
                text
        );

        textView.setTextSize(
                16
        );

        textView.setTextColor(
                Color.parseColor(
                        "#202337"
                )
        );

        textView.setGravity(
                Gravity.CENTER_VERTICAL
        );

        textView.setPadding(
                dp(16),
                dp(15),
                dp(16),
                dp(15)
        );

        return textView;
    }

    // ==================================================
    // TOAST
    // ==================================================

    private void showToast(
            String message
    ) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // ==================================================
    // DP
    // ==================================================

    private int dp(int value) {

        float density =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return Math.round(
                value * density
        );
    }
}