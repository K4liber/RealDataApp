package com.example.realdata;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.google.android.gms.location.FusedLocationProviderClient;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;


public class LocationSender implements Runnable {
    static final String msg = "MyRunnable: ";
    private final FusedLocationProviderClient fusedLocationClient;
    public static boolean isRunning = false;
    private String serverURL = null;
    public static LocalDateTime lastSendLocation = null;
    public static String error = "";

    public LocationSender(FusedLocationProviderClient fusedLocationProviderClient, String serverURL) {
        fusedLocationClient = fusedLocationProviderClient;
        this.serverURL = serverURL;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
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

                                    if (status == 200) {
                                        error = "";
                                        updateErrorTextView();
                                        lastSendLocation = LocalDateTime.now();
                                        TextView txtView = ((Activity)State.activityContext).findViewById(R.id.lastSend);
                                        txtView.setText("Last sent: " + LocalDateTime.now().toString());
                                    } else {
                                        error = "API status code = " + String.valueOf(status);
                                        updateErrorTextView();
                                    }

                                } catch (Exception ex) {
                                    error = "Exception: " + ex.getMessage();
                                    Log.d(msg, error);
                                    updateErrorTextView();
                                }
                            }
                        }
                );

                TimeUnit.SECONDS.sleep(10);
            } catch (Exception ex) {
                error = "Exception: " + ex.getMessage();
                Log.d(msg, error);
                updateErrorTextView();
                isRunning = false;
            }
        }
    }

    public void stop() {
        isRunning = false;
    }

    private void updateErrorTextView() {
        TextView errorView = ((Activity)State.activityContext).findViewById(R.id.error);
        errorView.setText(error);
    }
}
