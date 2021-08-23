package com.example.realdata;

import android.Manifest;
import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends Activity implements EasyPermissions.PermissionCallbacks {
    static String msg = "Android : ";
    private final int REQUEST_LOCATION_PERMISSION = 1;
    private final String[] perms =
            {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    private void setStart(boolean enabled) {
        Button startSendingData = findViewById(R.id.startService);
        Button stopSendingData = findViewById(R.id.stopService);

        if (enabled && !LocationSender.isRunning) {
            startSendingData.setEnabled(true);
            stopSendingData.setEnabled(false);
        } else {
            startSendingData.setEnabled(false);
            stopSendingData.setEnabled(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        requestLocationPermission();
        Log.d(msg, "The onCreate() event");
        State.serverURL = "http://13.36.229.179";
        State.activityContext = this;

        if (LocationSender.lastSendLocation != null) {
            TextView lastSendView = findViewById(R.id.lastSend);
            lastSendView.setText("Last sent: " + LocationSender.lastSendLocation.toString());
        }

        TextView sendErrorView = findViewById(R.id.error);
        sendErrorView.setText(LocationSender.error);
        this.setStart(true);
        EditText field1 = findViewById(R.id.serverURL);
        field1.setText(State.serverURL);
        field1.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                if(s.length() != 0)
                    State.serverURL = s.toString();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        Log.d(msg, "Granted: " + list.toString());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        Log.d(msg, "Denied: " + list.toString());

        if (EasyPermissions.somePermissionPermanentlyDenied(this, Arrays.asList(perms))) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(this, "Permission window closed", Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @AfterPermissionGranted(REQUEST_LOCATION_PERMISSION)
    public void requestLocationPermission() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", REQUEST_LOCATION_PERMISSION, perms);
        }
    }

    public void startService(View view) {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(this, SendService.class);
            serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
            ContextCompat.startForegroundService(this, serviceIntent);
            //startService(new Intent(getBaseContext(), SendService.class));
            this.setStart(false);
        } else {
            requestLocationPermission();
        }
    }

    // Method to stop the service
    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), SendService.class));
        this.setStart(true);
    }
}