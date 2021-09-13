package com.example.realdata.utils;

import android.content.Context;
import android.net.Uri;

import com.example.realdata.BuildConfig;

public class State {
    public static String serverURL = "http://ucanthide.link:5000";
    public static Context activityContext = null;
    public static String deviceId = null;
    public static final String secretKey = BuildConfig.SECRET_KEY;
    public static Uri imageFileUri = null;
}
