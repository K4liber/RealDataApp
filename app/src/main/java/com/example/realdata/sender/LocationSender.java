package com.example.realdata.sender;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.example.realdata.R;
import com.example.realdata.utils.State;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class LocationSender implements Runnable {
    static final String msg = "MyRunnable: ";
    private final FusedLocationProviderClient fusedLocationClient;
    public static boolean isRunning = false;
    private String serverURL = null;
    private String deviceID = null;
    public static LocalDateTime lastSendLocation = null;
    public static String error = "";

    public LocationSender(FusedLocationProviderClient fusedLocationProviderClient,
                          String serverURL, String deviceID) {
        fusedLocationClient = fusedLocationProviderClient;
        this.serverURL = serverURL;
        this.deviceID = deviceID;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @SuppressLint("MissingPermission")
    public void run() {
        LocationCallback mLocationCallback = new LocationCallback() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    if (location != null && isRunning) {
                        String sendError = sendLocation(location);

                        if (sendError != null && !sendError.equals("")) {
                            updateErrorTextView();
                        } else {
                            return;
                        }
                    }
                }
            }
        };
        isRunning = true;

        while (isRunning) {
            Integer exceptions = 0;

            try {
                Log.d(msg, "Trying to get last location ...");
                LocationRequest mLocationRequest = LocationRequest.create();
                mLocationRequest.setInterval(60000);
                mLocationRequest.setFastestInterval(5000);
                mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                fusedLocationClient.requestLocationUpdates(
                        mLocationRequest, mLocationCallback, Looper.getMainLooper());
                TimeUnit.SECONDS.sleep(10);
            } catch (Exception ex) {
                exceptions += 1;
                error = "Exception: " + ex.getMessage();
                Log.d(msg, error);
                updateErrorTextView();

                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (exceptions >= 10) {
                    isRunning = false;
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String sendLocation(Location location) {
        try {
            Log.d(msg, location.toString());
            URL url = new URL(serverURL + "/location?" +
                    "altitude=" + String.valueOf(location.getAltitude()) +
                    "&longitude=" + String.valueOf(location.getLongitude()) +
                    "&latitude=" + String.valueOf(location.getLatitude()) +
                    "&device_id=" + this.deviceID
            );
            URL utlPost = new URL(serverURL + "/location");
            HttpURLConnection httpURLConnection =
                    (HttpURLConnection) utlPost.openConnection();
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            StringBuilder result = new StringBuilder();
            boolean first = true;
            HashMap<String, String> values = new HashMap<>();
            values.put("altitude", String.valueOf(location.getAltitude()));
            values.put("longitude", String.valueOf(location.getLongitude()));
            values.put("latitude", String.valueOf(location.getLatitude()));
            values.put("device_id", this.deviceID);
            values.put("secret_key", State.secretKey);

            for(Map.Entry<String, String> entry : values.entrySet()){
                if (first)
                    first = false;
                else
                    result.append("&");

                result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                result.append("=");
                result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            Log.d(msg, result.toString());
            OutputStream os = httpURLConnection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(result.toString());
            writer.flush();
            writer.close();
            os.close();
            int status = httpURLConnection.getResponseCode();
            Log.d(msg, "Status: " + String.valueOf(status));

            if (status == 200) {
                InputStream in =
                        new BufferedInputStream(httpURLConnection.getInputStream());
                Log.d(msg, in.toString());
                error = "";
                updateErrorTextView();
                lastSendLocation = LocalDateTime.now();

                if (State.activityContext != null) {
                    TextView txtView = ((Activity) State.activityContext)
                            .findViewById(R.id.lastSend);
                    txtView.setText("Last sent: " + LocalDateTime.now().toString());
                }
            } else {
                error = "API status code = " + String.valueOf(status);
            }

        } catch (Exception ex) {
            error = "Exception: " + ex;
            Log.d(msg, error);
        }

        return error;
    }

    public void stop() {
        isRunning = false;
    }

    private void updateErrorTextView() {
        if (State.activityContext != null) {
            TextView errorView = ((Activity) State.activityContext).findViewById(R.id.error);
            errorView.setText(error);
        }
    }
}
