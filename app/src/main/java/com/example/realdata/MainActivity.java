package com.example.realdata;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.example.realdata.sender.LocationSender;
import com.example.realdata.sender.ViewSender;
import com.example.realdata.utils.Config;
import com.example.realdata.utils.State;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends Activity implements EasyPermissions.PermissionCallbacks {
    private static final int REQUEST_CAMERA = 2;
    private static final int THUMBNAIL_SIZE = 800;
    static String msg = "Android : ";
    private final int REQUEST_PERMISSIONS = 1;
    private final String[] perms = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private final String tag = "MainActivity";
    private ImageView ivCompressed;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        ivCompressed = findViewById(R.id.imageButton);
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
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
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
        Log.d(this.tag, "onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE) {
            // Do something after user returned from app settings screen, like showing a Toast.
            Toast.makeText(this, "Permission window closed", Toast.LENGTH_SHORT)
                    .show();
        } else if (requestCode == REQUEST_CAMERA) {
            Log.d(this.tag, "onActivityResult() requestCode = REQUEST_CAMERA");

            try {
                Bitmap thumbnail = getThumbnail(State.imageFileUri);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                Log.d(this.tag, "onActivityResult() creating bitmap");
                ivCompressed.setImageBitmap(thumbnail);
                FileOutputStream fo;
                Log.d(this.tag, "onActivityResult() writing bitmap");

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
            } catch (IOException e) {
                Log.d(this.tag, "error: " + e.getMessage());
                e.printStackTrace();
            }

        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSIONS)
    public void requestPermissions() {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(
                    this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
        else {
            EasyPermissions.requestPermissions(
                    this, "Please grant the permissions", REQUEST_PERMISSIONS, perms);
        }
    }

    public void startService(View view) {
        if (EasyPermissions.hasPermissions(this, perms)) {
            Toast.makeText(
                    this, "Permission already granted", Toast.LENGTH_SHORT).show();
            Intent serviceIntent = new Intent(this, SendService.class);
            serviceIntent.putExtra(
                    "inputExtra", "Foreground Service Example in Android");
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
        File image = new File(this.appFolderCheckAndCreate(), "img.jpg");
        Uri imageFileUri = Uri.fromFile(image);
        State.imageFileUri = imageFileUri;
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private Bitmap getThumbnail(Uri uri) throws IOException{
        InputStream input = this.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth)
                ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > THUMBNAIL_SIZE) ? (originalSize / THUMBNAIL_SIZE) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true; //optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;//
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private static int getPowerOfTwoForSampleRatio(double ratio){
        int k = Integer.highestOneBit((int)Math.floor(ratio));
        if(k==0) return 1;
        else return k;
    }

    private String appFolderCheckAndCreate(){
        String appFolderPath="";
        File externalStorage = Environment.getExternalStorageDirectory();

        if (externalStorage.canWrite()) {
            appFolderPath = externalStorage.getAbsolutePath() + "/MyApp";
            File dir = new File(appFolderPath);

            if (!dir.exists()) {
                dir.mkdirs();
            }

        } else {
            Toast.makeText(this,
                    "Storage media not found or is full!", Toast.LENGTH_SHORT).show();
        }

        return appFolderPath;
    }
}
