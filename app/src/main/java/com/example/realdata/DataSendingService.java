package com.example.realdata;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

/**
 * Created by TutorialsPoint7 on 8/23/2016.
 */

public class DataSendingService extends Service {
    private FusedLocationProviderClient fusedLocationClient;
    private LocationSender locationSender = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Let it continue running until it is stopped.
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        locationSender = new LocationSender(fusedLocationClient, Config.serverURL);
        Thread thread = new Thread(locationSender);
        thread.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
        locationSender.stop();
    }
}