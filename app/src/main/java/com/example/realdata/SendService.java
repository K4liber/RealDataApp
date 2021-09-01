package com.example.realdata;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import android.os.StrictMode;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;


public class SendService extends Service {
    private LocationSender locationSender = null;
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        FusedLocationProviderClient fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);
        String input = intent.getStringExtra("inputExtra");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setContentIntent(pendingIntent)
                .build();
        NotificationManager  mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    RealDataApp.CHANNEL_ID, RealDataApp.CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
            new NotificationCompat.Builder(this, RealDataApp.CHANNEL_ID);
        }

        startForeground(1, notification);
        Toast.makeText(this, "Sending data started", Toast.LENGTH_LONG).show();
        locationSender = new LocationSender(
                fusedLocationClient, State.serverURL, this.getDeviceID());
        Thread thread = new Thread(locationSender);
        thread.start();
        return START_NOT_STICKY;
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Sending data stopped", Toast.LENGTH_LONG).show();
        locationSender.stop();
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
    }

    @SuppressLint("HardwareIds")
    private String getDeviceID() {
        if (State.device_id == null) {
            TelephonyManager telephonyManager =
                    (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            State.device_id = telephonyManager.getDeviceId();
        }

        if (State.device_id == null) {
            Toast.makeText(this, "Cannot get device ID", Toast.LENGTH_LONG).show();
        }
        return State.device_id;
    }
}