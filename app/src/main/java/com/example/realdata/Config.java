package com.example.realdata;

public class Config {
    private static final Config instance = new Config();
    public static String serverURL = "";

    private Config() {
        if (instance == null) {
            System.out.println("creating");
        }
    }
}
