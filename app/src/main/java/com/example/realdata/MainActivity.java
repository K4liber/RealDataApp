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
import com.example.realdata.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

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
        loadLastView();
    }

    private void loadLastView() {
        Bitmap lastView = downloadLastView();

        if (lastView != null) {
            ivCompressed.setImageBitmap(lastView);
        }
    }

    private ByteArrayOutputStream loadImageFromFile() {
        try {
            Bitmap thumbnail = getThumbnail(State.imageFileUri());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
            Log.d(this.tag, "onActivityResult() creating bitmap");
            ivCompressed.setImageBitmap(thumbnail);
            return bytes;
        } catch (IOException e) {
            Log.d(this.tag, e.getMessage());
        }

        return null;
    }

    @SuppressLint("HardwareIds")
    private void setState() {
        State.activityContext = this;
        TelephonyManager telephonyManager =
                (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
        State.deviceId = telephonyManager.getDeviceId();

        try {
            State.sslContext = this.getSSLContext();
        } catch (Exception e) {
            Log.d(this.tag, "Error while getting ssl context: " + e.getMessage());
        }
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

            if (State.lastView().exists() == false) {
                loadLastView();
                return;
            }

            ByteArrayOutputStream bytes = loadImageFromFile();
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
        File lastView = State.lastView();
        State.lastView().delete();
        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, State.imageFileUri());
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private Bitmap getThumbnail(Uri uri) throws IOException{
        InputStream input = this.getContentResolver().openInputStream(uri);

        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither=true;//optional
        onlyBoundsOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();

        if ((onlyBoundsOptions.outWidth == -1) || (onlyBoundsOptions.outHeight == -1)) {
            return null;
        }

        int originalSize = (onlyBoundsOptions.outHeight > onlyBoundsOptions.outWidth)
                ? onlyBoundsOptions.outHeight : onlyBoundsOptions.outWidth;

        double ratio = (originalSize > Config.imgMaxPixels)
                ? (originalSize / Config.imgMaxPixels) : 1.0;

        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = Utils.getPowerOfTwoForSampleRatio(ratio);
        bitmapOptions.inDither = true; //optional
        bitmapOptions.inPreferredConfig=Bitmap.Config.ARGB_8888;
        input = this.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();
        return bitmap;
    }

    private Bitmap downloadLastView() {
        try {
            URL url = new URL(State.serverURL + "/view?" +
                    "device_id=" + State.deviceId
            );
            HttpsURLConnection httpsURLConnection =
                    (HttpsURLConnection) url.openConnection();
            httpsURLConnection.setSSLSocketFactory(State.sslContext.getSocketFactory());
            httpsURLConnection.setRequestMethod("GET");
            httpsURLConnection.setDoInput(true);
            httpsURLConnection.setUseCaches(false);
            return BitmapFactory.decodeStream(httpsURLConnection.getInputStream());
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }

        return null;
    }

    private SSLContext getSSLContext() throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException, KeyManagementException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        final InputStream in = this.getResources().openRawResource(R.raw.cert);
        Certificate ca = cf.generateCertificate(in);
        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);
        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);
        // Create an SSLContext that uses our TrustManager
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), null);
        return sslContext;
    }
}
