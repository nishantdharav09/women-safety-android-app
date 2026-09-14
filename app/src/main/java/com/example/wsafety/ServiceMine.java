package com.example.wsafety;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.womensafety.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class ServiceMine extends Service implements SensorEventListener {

    private static final int MAX_CONTACTS = 5;

    private static final String CHANNEL_ID = "MYID";
    private static final int NOTIFICATION_ID = 115;

    // Shake detection settings
    private static final float SHAKE_THRESHOLD = 14.0f;
    private static final long SHAKE_TIME_INTERVAL = 700;
    private static final int REQUIRED_SHAKES = 3;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private FusedLocationProviderClient fusedLocationClient;
    private SmsManager smsManager;

    private int shakeCount = 0;
    private long lastShakeTime = 0;

    private String myLocation = "Location unavailable";

    // Prevent two SOS triggers from happening too quickly
    private boolean sosInProgress = false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // =========================================================
    // SERVICE CREATE
    // =========================================================

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d("SERVICE", "Service created");

        smsManager = SmsManager.getDefault();

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        createNotificationChannel();

        setupAccelerometer();

        fetchLocation();
    }

    // =========================================================
    // ACCELEROMETER
    // =========================================================

    private void setupAccelerometer() {

        sensorManager =
                (SensorManager) getSystemService(
                        Context.SENSOR_SERVICE
                );

        if (sensorManager == null) {
            Log.e(
                    "SHAKE",
                    "SensorManager unavailable"
            );
            return;
        }

        accelerometer =
                sensorManager.getDefaultSensor(
                        Sensor.TYPE_ACCELEROMETER
                );

        if (accelerometer == null) {

            Log.e(
                    "SHAKE",
                    "Accelerometer unavailable"
            );

            return;
        }

        sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
        );

        Log.d(
                "SHAKE",
                "Accelerometer listener registered"
        );
    }

    // =========================================================
    // NOTIFICATION CHANNEL
    // =========================================================

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Women Safety Service",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription(
                    "Women Safety background protection service"
            );

            NotificationManager manager =
                    (NotificationManager)
                            getSystemService(
                                    Context.NOTIFICATION_SERVICE
                            );

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // =========================================================
    // FOREGROUND SERVICE
    // =========================================================

    private void startSafetyForegroundService() {

        Intent notificationIntent =
                new Intent(
                        this,
                        MainActivity.class
                );

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        Notification notification =
                new NotificationCompat.Builder(
                        this,
                        CHANNEL_ID
                )
                        .setContentTitle(
                                "Women Safety"
                        )
                        .setContentText(
                                "Safety service is active • Shake 3 times for SOS"
                        )
                        .setSmallIcon(
                                R.drawable.girl_vector
                        )
                        .setContentIntent(
                                pendingIntent
                        )
                        .setOngoing(true)
                        .setPriority(
                                NotificationCompat.PRIORITY_LOW
                        )
                        .build();

        startForeground(
                NOTIFICATION_ID,
                notification
        );

        Log.d(
                "SERVICE",
                "Foreground service started"
        );
    }

    // =========================================================
    // LOCATION
    // =========================================================

    private void fetchLocation() {

        boolean fineGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        boolean coarseGranted =
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED;

        if (!fineGranted && !coarseGranted) {

            myLocation =
                    "Location permission not granted";

            Log.e(
                    "LOCATION",
                    "Location permission not granted"
            );

            return;
        }

        fusedLocationClient
                .getLastLocation()
                .addOnSuccessListener(
                        location -> {

                            if (location != null) {

                                myLocation =
                                        "https://maps.google.com/?q="
                                                + location.getLatitude()
                                                + ","
                                                + location.getLongitude();

                                Log.d(
                                        "LOCATION",
                                        "Location updated: "
                                                + myLocation
                                );

                            } else {

                                myLocation =
                                        "Location unavailable";

                                Log.e(
                                        "LOCATION",
                                        "Last location is null"
                                );
                            }
                        }
                )
                .addOnFailureListener(
                        e -> {

                            myLocation =
                                    "Location unavailable";

                            Log.e(
                                    "LOCATION",
                                    "Location fetch failed",
                                    e
                            );
                        }
                );
    }

    // =========================================================
    // SHAKE DETECTION
    // =========================================================

    @Override
    public void onSensorChanged(
            SensorEvent event
    ) {

        if (event.sensor.getType()
                != Sensor.TYPE_ACCELEROMETER) {
            return;
        }

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float acceleration =
                (float) Math.sqrt(
                        x * x
                                + y * y
                                + z * z
                );

        float force =
                Math.abs(
                        acceleration - 9.81f
                );

        if (force > SHAKE_THRESHOLD) {

            long currentTime =
                    System.currentTimeMillis();

            if (currentTime - lastShakeTime
                    > SHAKE_TIME_INTERVAL) {

                lastShakeTime =
                        currentTime;

                shakeCount++;

                Log.d(
                        "SHAKE",
                        "Shake "
                                + shakeCount
                                + "/"
                                + REQUIRED_SHAKES
                );

                vibrateShort();

                if (shakeCount >= REQUIRED_SHAKES) {

                    shakeCount = 0;

                    if (!sosInProgress) {

                        sosInProgress = true;

                        Log.d(
                                "SOS",
                                "3 shakes detected"
                        );

                        fetchLocation();

                        /*
                         * Small delay so the latest location
                         * has time to update before SMS.
                         */
                        new Handler(
                                Looper.getMainLooper()
                        ).postDelayed(
                                () -> {

                                    sendEmergencyMessages();

                                    /*
                                     * Allow another SOS after
                                     * a short cooldown.
                                     */
                                    new Handler(
                                            Looper.getMainLooper()
                                    ).postDelayed(
                                            () -> sosInProgress = false,
                                            2500
                                    );

                                },
                                1000
                        );
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy
    ) {
        // Not required
    }

    // =========================================================
    // SOS MESSAGE SYSTEM
    // =========================================================

    private void sendEmergencyMessages() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED) {

            Log.e(
                    "SMS",
                    "SEND_SMS permission not granted"
            );

            return;
        }

        SharedPreferences preferences =
                getSharedPreferences(
                        "MySharedPref",
                        MODE_PRIVATE
                );

        /*
         * 10 UNIQUE SOS MESSAGES
         *
         * Every new SOS gets the next message.
         */
        String[] messages = {

                "SOS! Please help me. I may be in danger.\n"
                        + "Please contact me immediately.\n"
                        + "My current location:\n"
                        + myLocation,

                "Emergency! I need your help right now.\n"
                        + "Please reach me as soon as possible.\n"
                        + "My current location:\n"
                        + myLocation,

                "Please help me! I feel unsafe right now.\n"
                        + "Please contact me immediately.\n"
                        + "My current location:\n"
                        + myLocation,

                "This is an emergency. Please call me or reach me quickly.\n"
                        + "My current location:\n"
                        + myLocation,

                "I need urgent help. Please check on me.\n"
                        + "My current location:\n"
                        + myLocation,

                "Please respond as soon as possible. I need assistance.\n"
                        + "My current location:\n"
                        + myLocation,

                "Emergency alert! Please help me immediately.\n"
                        + "My current location:\n"
                        + myLocation,

                "I may be in an unsafe situation. Please help me.\n"
                        + "My current location:\n"
                        + myLocation,

                "Please contact me urgently. I need assistance.\n"
                        + "My current location:\n"
                        + myLocation,

                "SOS alert! Please help me and check my location.\n"
                        + "My current location:\n"
                        + myLocation
        };

        /*
         * Read which message should be sent next.
         *
         * First SOS = message 1
         * Second SOS = message 2
         * ...
         * Tenth SOS = message 10
         */
        int messageIndex =
                preferences.getInt(
                        "SOS_MESSAGE_INDEX",
                        0
                );

        if (messageIndex < 0
                || messageIndex >= messages.length) {

            messageIndex = 0;
        }

        String message =
                messages[messageIndex];

        boolean messageSent =
                false;

        /*
         * IMPORTANT:
         *
         * One SOS trigger sends ONE selected message
         * to each saved contact.
         *
         * It does NOT send multiple different messages
         * during the same trigger.
         */
        for (int i = 0;
             i < MAX_CONTACTS;
             i++) {

            String contactNumber =
                    preferences.getString(
                            "ENUM" + i,
                            ""
                    );

            if (contactNumber == null) {
                continue;
            }

            contactNumber =
                    contactNumber.trim();

            if (contactNumber.isEmpty()) {
                continue;
            }

            try {

                smsManager.sendTextMessage(
                        contactNumber,
                        null,
                        message,
                        null,
                        null
                );

                messageSent = true;

                Log.d(
                        "SMS_SENT",
                        "Message "
                                + (messageIndex + 1)
                                + " sent to "
                                + contactNumber
                );

            } catch (Exception e) {

                Log.e(
                        "SMS_ERROR",
                        "Failed to send SMS to "
                                + contactNumber,
                        e
                );
            }
        }

        // =====================================================
        // SAVE NEXT MESSAGE NUMBER
        // =====================================================

        if (messageSent) {

            int nextIndex =
                    messageIndex + 1;

            /*
             * After message 10,
             * start again from message 1.
             */
            if (nextIndex >= messages.length) {
                nextIndex = 0;
            }

            preferences.edit()
                    .putInt(
                            "SOS_MESSAGE_INDEX",
                            nextIndex
                    )
                    .apply();

            vibrateSOS();

            Log.d(
                    "SOS",
                    "SOS completed. Next message index = "
                            + nextIndex
            );

        } else {

            Log.d(
                    "SOS",
                    "No emergency contacts saved."
            );
        }
    }

    // =========================================================
    // SHORT VIBRATION
    // =========================================================

    private void vibrateShort() {

        Vibrator vibrator =
                (Vibrator)
                        getSystemService(
                                Context.VIBRATOR_SERVICE
                        );

        if (vibrator == null) {
            return;
        }

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            100,
                            VibrationEffect.DEFAULT_AMPLITUDE
                    )
            );

        } else {

            vibrator.vibrate(100);
        }
    }

    // =========================================================
    // SOS VIBRATION
    // =========================================================

    private void vibrateSOS() {

        Vibrator vibrator =
                (Vibrator)
                        getSystemService(
                                Context.VIBRATOR_SERVICE
                        );

        if (vibrator == null) {
            return;
        }

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.O) {

            vibrator.vibrate(
                    VibrationEffect.createWaveform(
                            new long[]{
                                    0,
                                    300,
                                    150,
                                    300
                            },
                            -1
                    )
            );

        } else {

            vibrator.vibrate(
                    new long[]{
                            0,
                            300,
                            150,
                            300
                    },
                    -1
            );
        }
    }

    // =========================================================
    // START / STOP
    // =========================================================

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (intent != null
                && "STOP".equalsIgnoreCase(
                intent.getAction()
        )) {

            stopSafetyService();

            return START_NOT_STICKY;
        }

        startSafetyForegroundService();

        return START_STICKY;
    }

    private void stopSafetyService() {

        if (sensorManager != null) {

            sensorManager.unregisterListener(
                    this
            );
        }

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.N) {

            stopForeground(
                    STOP_FOREGROUND_REMOVE
            );

        } else {

            stopForeground(true);
        }

        stopSelf();

        Log.d(
                "SERVICE",
                "Safety service stopped"
        );
    }

    // =========================================================
    // DESTROY
    // =========================================================

    @Override
    public void onDestroy() {

        if (sensorManager != null) {

            sensorManager.unregisterListener(
                    this
            );
        }

        super.onDestroy();

        Log.d(
                "SERVICE",
                "Service destroyed"
        );
    }
}