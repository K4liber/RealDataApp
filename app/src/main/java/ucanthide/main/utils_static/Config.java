package ucanthide.main.utils_static;

import android.os.Environment;

import ucanthide.main.BuildConfig;

import java.io.File;

public class Config {
    public static final File tempImg =
            new File(Environment.getExternalStorageDirectory(),"temp.jpg");
    public static final Integer imgMaxPixels = 800;
    public static final String serverURL = "https://ucanthide.link:9443";
    public static final String secretKey = BuildConfig.SECRET_KEY;
}
