package ucanthide.main;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class RealDataApp extends Application {
    public static final String CHANNEL_ID = "autoStartServiceUCantHide";
    public static final String CHANNEL_NAME = "Auto Start Service UCantHide";
    public static NotificationChannel serviceChannel;

    @Override
    public void onCreate() {
        super.onCreate();
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