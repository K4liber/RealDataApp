package com.example.realdata;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
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

import com.example.realdata.sender.LocationSender;
import com.example.realdata.sender.ViewSender;
import com.example.realdata.utils.Config;
import com.example.realdata.utils.State;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends Activity implements EasyPermissions.PermissionCallbacks {
    private static final int REQUEST_CAMERA = 2;
    static String msg = "Android : ";
    private final int REQUEST_PERMISSIONS = 1;
    private final String[] perms = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final String tag = "MainActivity";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        requestPermissions();
        setState();
        setUI();
    }

    private void setUI() {
        TextView deviceIdTestView = findViewById(R.id.deviceId);
        deviceIdTestView.setText("Device ID: " + State.deviceId);

        if (LocationSender.lastSendLocation != null) {
            TextView lastSendView = findViewById(R.id.lastSend);
            lastSendView.setText("Last sent: " + LocationSender.lastSendLocation.toString());
        }

        TextView sendErrorView = findViewById(R.id.error);
        sendErrorView.setText(LocationSender.error);
        this.setStart(true);
        EditText serverURLField = findViewById(R.id.serverURL);
        serverURLField.setText(State.serverURL);
        serverURLField.addTextChangedListener(new TextWatcher() {
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

    @SuppressLint("HardwareIds")
    private void setState() {
        State.activityContext = this;
        TelephonyManager telephonyManager =
                (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        State.deviceId = telephonyManager.getDeviceId();
    }

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
        } else if (requestCode == REQUEST_CAMERA) {
            Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
            FileOutputStream fo;

            try {
                fo = new FileOutputStream(Config.tempImg);
                fo.write(bytes.toByteArray());
                fo.close();
                Log.d(this.tag, "onActivityResult: success");
            } catch (IOException e) {
                Log.d(this.tag, "onActivityResult: error: " + e.getMessage());
                e.printStackTrace();
            }

            ViewSender viewSender = new ViewSender();
            Thread thread = new Thread(viewSender);
            thread.start();
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    public void requestPermissions() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(
                    this, "Please grant the permissions", REQUEST_PERMISSIONS, perms);
        }
    }

    public void startService(View view) {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(this, "Permission already granted", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(this, SendService.class);
            serviceIntent.putExtra("inputExtra", "Foreground Service Example in Android");
            ContextCompat.startForegroundService(this, serviceIntent);
            this.setStart(false);
        } else {
            requestPermissions();
        }
    }

    // Method to stop the service
    public void stopService(View view) {
        stopService(new Intent(getBaseContext(), SendService.class));
        this.setStart(true);
    }

    public void takePhoto(View view) {
        Log.d(this.tag, "takePhoto");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }
}