package ucanthide.main;

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

import ucanthide.main.sender.LocationSender;
import ucanthide.main.utils_static.Config;
import ucanthide.main.utils_static.State;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.concurrent.TimeUnit;


public class SendService extends Service {
    private LocationSender locationSender = null;

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
        Notification notification =
                new NotificationCompat.Builder(this, RealDataApp.CHANNEL_ID)
                .setContentTitle("Foreground Service")
                .setContentText(input)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(2, notification);
        Toast.makeText(this, "Sending data started", Toast.LENGTH_LONG).show();
        locationSender = new LocationSender(fusedLocationClient, this.getDeviceID());
        Thread thread = new Thread(locationSender);
        thread.start();
        return START_NOT_STICKY;
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        super.onDestroy();
        locationSender.stop();
        stopForeground(Service.STOP_FOREGROUND_REMOVE);
        Toast.makeText(this, "Sending data stopped", Toast.LENGTH_LONG).show();
    }

    @SuppressLint("HardwareIds")
    private String getDeviceID() {
        while (State.deviceId == null) {
            TelephonyManager telephonyManager =
                    (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

            if (telephonyManager != null) {
                State.deviceId = telephonyManager.getDeviceId();
            }

            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return State.deviceId;
    }
}