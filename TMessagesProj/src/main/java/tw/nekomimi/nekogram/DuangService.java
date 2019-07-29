package tw.nekomimi.nekogram;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.LaunchActivity;

public class DuangService extends Service {

    NotificationManager systemNotificationManager = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent activityIntent = new Intent(this, LaunchActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplication(), 0, activityIntent, 0);

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("duang", "Other", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setSound(null, null);
            systemNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
            systemNotificationManager.createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(getApplication(), "duang");
        } else {
            builder = new Notification.Builder(getApplication());

        }
        Notification notification = builder.setSmallIcon(R.drawable.notification).
                setContentTitle(LocaleController.getString("NekogramRunning", R.string.NekogramRunning)).
                setContentIntent(pendingIntent).
                setWhen(System.currentTimeMillis()).
                build();
        startForeground(38264, notification);
        return super.onStartCommand(intent, flags, startId);
    }
}
