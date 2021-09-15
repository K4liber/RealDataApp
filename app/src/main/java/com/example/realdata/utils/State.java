package com.example.realdata.utils;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.example.realdata.BuildConfig;

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

public class State {
    public static String serverURL = "https://ucanthide.link:9443";
    public static Context activityContext = null;
    public static String deviceId = null;
    public static final String secretKey = BuildConfig.SECRET_KEY;
    private static Uri imageFileUri = null;
    private static File lastView = null;
    public static SSLContext sslContext = null;

    public static File lastView() {
        if (lastView != null) {
            return lastView;
        }

        String folderPath = State.appFolderCheckAndCreate();

        if (folderPath == null) {
            return null;
        }

        lastView = new File(State.appFolderCheckAndCreate(), "img.jpg");
        return lastView;
    }

    public static Uri imageFileUri() {
        if (imageFileUri != null) {
            return imageFileUri;
        }

        File file = State.lastView();

        if (file == null) {
            return null;
        }

        imageFileUri = Uri.fromFile(file);
        return imageFileUri;
    }

    private static String appFolderCheckAndCreate(){
        String appFolderPath = null;
        File externalStorage = Environment.getExternalStorageDirectory();

        if (externalStorage.canWrite()) {
            appFolderPath = externalStorage.getAbsolutePath() + "/MyApp";
            File dir = new File(appFolderPath);

            if (!dir.exists()) {
                dir.mkdirs();
            }
        }

        return appFolderPath;
    }
}
