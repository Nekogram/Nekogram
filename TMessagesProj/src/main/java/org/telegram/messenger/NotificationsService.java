/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import tw.nekomimi.nekogram.NekoConfig;

public class NotificationsService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();

        if (NekoConfig.residentNotification) {
            NotificationChannelCompat channel = new NotificationChannelCompat.Builder("nekogram", NotificationManagerCompat.IMPORTANCE_DEFAULT)
                    .setName(LocaleController.getString("NekogramRunning", R.string.NekogramRunning))
                    .setLightsEnabled(false)
                    .setVibrationEnabled(false)
                    .setSound(null, null)
                    .build();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.createNotificationChannel(channel);
            startForeground(38264,
                    new NotificationCompat.Builder(this, "nekogram")
                            .setSmallIcon(R.drawable.notification)
                            .setColor(NekoConfig.getNotificationColor())
                            .setColorized(true)
                            .setShowWhen(false)
                            .setContentText(LocaleController.getString("NekogramRunning", R.string.NekogramRunning))
                            .setCategory(NotificationCompat.CATEGORY_STATUS)
                            .build());
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
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }
}
