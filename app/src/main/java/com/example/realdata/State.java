package com.example.realdata;

import android.content.Context;

public class State {
    private static final State instance = new State();
    public static String serverURL = "";
    public static Context activityContext = null;
    public static String device_id = null;

    private State() {
        if (instance == null) {
            System.out.println("creating");
        }
    }
}
