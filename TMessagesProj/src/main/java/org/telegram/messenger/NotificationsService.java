/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import org.telegram.ui.LaunchActivity;

import tw.nekomimi.nekogram.NekoConfig;

public class NotificationsService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
        if (NekoConfig.residentNotification) {
            Intent activityIntent = new Intent(this, LaunchActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("nekogram", LocaleController.getString("NekogramRunning", R.string.NekogramRunning), NotificationManager.IMPORTANCE_DEFAULT);
                channel.enableLights(false);
                channel.enableVibration(false);
                channel.setSound(null, null);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(channel);
                }
            }
            Notification notification = new NotificationCompat.Builder(this, "nekogram")
                    .setSmallIcon(R.drawable.notification)
                    .setColor(0xff11acfa)
                    .setContentTitle(LocaleController.getString("NekogramRunning", R.string.NekogramRunning))
                    .setContentIntent(pendingIntent)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .build();
            startForeground(38264, notification);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true)) {
            Intent intent = new Intent("org.telegram.start");
            sendBroadcast(intent);
        }
    }
}
