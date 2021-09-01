package com.example.realdata;

import android.content.Context;
import android.telephony.TelephonyManager;

public class State {
    private static final State instance = new State();
    public static String serverURL = "http://15.237.128.97:5000";
    public static Context activityContext = null;
    public static String device_id = null;

    private State() {
        if (instance == null) {
            System.out.println("creating");
        }
    }
}
