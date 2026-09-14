package com.example.wsafety;

import android.Manifest;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import androidx.core.content.ContextCompat;

public class VolumeKeyService extends AccessibilityService {

    private static final int REQUIRED_PRESSES = 3;
    private static final long MAX_GAP = 1500;

    private int pressCount = 0;
    private long lastPressTime = 0;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo info = getServiceInfo();

        if (info != null) {
            info.flags |=
                    AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;

            setServiceInfo(info);
        }

        Log.d("VOLUME_SERVICE", "Volume service connected");
    }

    @Override
    public boolean onKeyEvent(KeyEvent event) {

        Log.d(
                "VOLUME_SERVICE",
                "Key: " + event.getKeyCode()
        );

        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {

            long now = SystemClock.elapsedRealtime();

            if (now - lastPressTime > MAX_GAP) {
                pressCount = 0;
            }

            lastPressTime = now;
            pressCount++;

            Log.d(
                    "VOLUME_SERVICE",
                    "Volume Down: " + pressCount + "/3"
            );

            if (pressCount >= REQUIRED_PRESSES) {

                pressCount = 0;

                Log.d(
                        "VOLUME_SERVICE",
                        "3 Volume Down detected"
                );

                makeEmergencyCall();

            }

            // false = normal volume control remains
            return false;
        }

        return false;
    }

    private void makeEmergencyCall() {

        SharedPreferences preferences =
                getSharedPreferences(
                        "MySharedPref",
                        MODE_PRIVATE
                );

        String number =
                preferences.getString("ENUM0", "");

        if (number == null || number.trim().isEmpty()) {

            Log.e(
                    "VOLUME_SERVICE",
                    "ENUM0 is empty. No emergency contact saved."
            );

            return;
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
        ) != PackageManager.PERMISSION_GRANTED) {

            Log.e(
                    "VOLUME_SERVICE",
                    "CALL_PHONE permission not granted"
            );

            return;
        }

        try {

            Intent callIntent =
                    new Intent(
                            Intent.ACTION_CALL,
                            Uri.parse("tel:" + number.trim())
                    );

            callIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );

            startActivity(callIntent);

            Log.d(
                    "VOLUME_SERVICE",
                    "Calling: " + number
            );

        } catch (Exception e) {

            Log.e(
                    "VOLUME_SERVICE",
                    "Call failed",
                    e
            );
        }
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event
    ) {
        // Not required
    }

    @Override
    public void onInterrupt() {
        // Not required
    }
}