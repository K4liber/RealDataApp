package com.example.realdata.utils;

import android.os.Environment;

import java.io.File;

public class Config {
    public static final File tempImg =
            new File(Environment.getExternalStorageDirectory(),"temp.jpg");
}
