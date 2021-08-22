package com.example.realdata;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;


public class LocationSender implements Runnable {
    static final String msg = "MyRunnable: ";
    private final FusedLocationProviderClient fusedLocationClient;
    private boolean isRunning = false;
    private String serverURL = null;

    public LocationSender(FusedLocationProviderClient fusedLocationProviderClient, String serverURL) {
        fusedLocationClient = fusedLocationProviderClient;
        this.serverURL = serverURL;
    }

    @SuppressLint("MissingPermission")
    public void run() {
        isRunning = true;

        while (isRunning) {
            try {
                Log.d(msg, "Trying to get last location ...");
                this.fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                            if (location != null) {
                                try {
                                    Log.d(msg, location.toString());
                                    URL url = new URL(serverURL + "/location?" +
                                            "altitude=" + String.valueOf(location.getAltitude()) +
                                            "&longitude=" + String.valueOf(location.getLongitude()) +
                                            "&latitude=" + String.valueOf(location.getLatitude())
                                    );
                                    HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                                    httpURLConnection.setRequestMethod("GET");
                                    InputStream in = new BufferedInputStream(httpURLConnection.getInputStream());
                                    Log.d(msg, in.toString());
                                    int status = httpURLConnection.getResponseCode();
                                    Log.d(msg, "Status: " + String.valueOf(status));
                                } catch (Exception ex) {
                                    Log.d(msg, "Exception: " + ex.getMessage());
                                }
                            }
                        }
                );

                TimeUnit.SECONDS.sleep(10);
            } catch (Exception ex) {
                Log.d(msg, "Exception: " + ex.getMessage());
                break;
            }
        }
    }

    public void stop() {
        isRunning = false;
    }
}
