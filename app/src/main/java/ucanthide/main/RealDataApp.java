package ucanthide.main;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

import ucanthide.main.utils_static.State;
import ucanthide.main.utils_static.Utils;

public class RealDataApp extends Application {
    public static final String CHANNEL_ID = "autoStartServiceWorldCitizen";
    public static final String CHANNEL_NAME = "Auto Start Service World Citizen";
    public static NotificationChannel serviceChannel;
    private final String tag = "RealDataApp";

    @Override
    public void onCreate() {
        super.onCreate();
        State.appContext = this.getBaseContext();

        try {
            if (State.getSSLContext() == null) {
                State.setSSLContext(Utils.getSSLContext());
            }
        } catch (Exception e) {
            Log.d(this.tag, "Error while getting ssl context: " + e.getMessage());
        }

        createNotificationChannel();
    }

    public void createNotificationChannel() {
        if (RealDataApp.serviceChannel != null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            RealDataApp.serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
}