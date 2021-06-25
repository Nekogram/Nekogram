package tw.nekomimi.nekogram.updater;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.LaunchActivity;

import java.io.File;

public class UpdateService extends Service implements NotificationCenter.NotificationCenterDelegate {
    private static final int NOTIFICATION_ID_PROGRESS = 25263;
    private static final int NOTIFICATION_ID = 25264;
    UpdateHelper updateHelper;
    UpdateHelper.UpdateRef updateRef;
    NotificationCompat.Builder builder;
    boolean canceled = false;

    public UpdateHelper getUpdateHelper() {
        if (updateHelper == null) {
            updateHelper = UpdateHelper.getInstance();
        }
        return updateHelper;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateRef = getUpdateHelper().updatingRef;
        if (updateRef == null) {
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }
        startDownload();
        return super.onStartCommand(intent, flags, startId);
    }

    @SuppressLint("DefaultLocale")
    private void startDownload() {
        Intent activityIntent = new Intent(this, LaunchActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, 0);
        Intent cancelIntent = new Intent("cancel-download-update");
        PendingIntent cancelPendingIntent =
                PendingIntent.getBroadcast(this, 0, cancelIntent, 0);
        IntentFilter filter = new IntentFilter(cancelIntent.getAction());
        this.registerReceiver(receiver, filter);

        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        File f = FileLoader.getPathToAttach(updateRef.document, true);
        if (f.exists()) {
            installIntent.setDataAndType(FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider", f), "application/vnd.android.package-archive");
            PendingIntent installPendingIntent = PendingIntent.getActivity(this, 500, installIntent, 0);
            builder = getUpdateHelper().createNotificationBuilder(false);
            builder = builder.setSmallIcon(R.drawable.notification).
                    setContentTitle(LocaleController.getString("DownloadingUpdateFinish", R.string.DownloadingUpdateFinish)).
                    setContentText(String.format("%s (%d)", updateRef.versionName, updateRef.versionCode)).
                    setContentIntent(installPendingIntent).
                    setWhen(System.currentTimeMillis()).
                    setColor(0xff11acfa).
                    setGroupSummary(false);
            getUpdateHelper().getSystemNotificationManager().notify(NOTIFICATION_ID, builder.build());
            stopSelf();
        } else {
            builder = getUpdateHelper().createNotificationBuilder(true);
            builder = builder.setSmallIcon(R.drawable.notification).
                    setContentTitle(LocaleController.getString("DownloadingUpdate", R.string.DownloadingUpdate)).
                    setContentText(String.format("%s (%d)", updateRef.versionName, updateRef.versionCode)).
                    setContentIntent(pendingIntent).
                    addAction(R.drawable.notification, LocaleController.getString("Cancel", R.string.Cancel), cancelPendingIntent).
                    setWhen(System.currentTimeMillis()).
                    setColor(0xff11acfa).
                    setColorized(true).
                    setGroupSummary(false).
                    setProgress(100, 0, true);
            startForeground(NOTIFICATION_ID_PROGRESS, builder.build());
            FileLoader.getInstance(UserConfig.selectedAccount).loadFile(SharedConfig.pendingAppUpdate.document, "update", 1, 1);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileLoadProgressChanged);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileLoaded);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.httpFileDidLoad);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileLoadFailed);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.httpFileDidFailedLoad);
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent intent) {
            canceled = true;
            AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().cancelLoadFile(updateRef.document);
            getUpdateHelper().getSystemNotificationManager().cancel(NOTIFICATION_ID_PROGRESS);
            stopSelf();
        }
    };

    @SuppressLint("DefaultLocale")
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.fileLoadProgressChanged) {
            if (args[0].equals(updateRef.realFilename)) {
                getUpdateHelper().getSystemNotificationManager().notify(NOTIFICATION_ID_PROGRESS,
                        builder.setProgress((int) (long) args[2], (int) (long) args[1], false).build());
            }
        } else if (id == NotificationCenter.fileLoaded || id == NotificationCenter.httpFileDidLoad) {
            if (args[0].equals(updateRef.realFilename)) {
                builder = getUpdateHelper().createNotificationBuilder(false);
                builder = builder.setSmallIcon(R.drawable.notification).
                        setContentTitle(LocaleController.getString("DownloadingUpdateFinish", R.string.DownloadingUpdateFinish)).
                        setContentText(String.format("%s (%d)", updateRef.versionName, updateRef.versionCode)).
                        setWhen(System.currentTimeMillis()).
                        setColor(0xff11acfa).
                        setGroupSummary(false);
                Intent installIntent = new Intent(Intent.ACTION_VIEW);
                installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                File f = FileLoader.getPathToAttach(updateRef.document, true);
                installIntent.setDataAndType(FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider", f), "application/vnd.android.package-archive");
                PendingIntent installPendingIntent = PendingIntent.getActivity(this, 500, installIntent, 0);
                if (f.exists()) {
                    builder = builder.setContentIntent(installPendingIntent);
                } else {
                    builder = builder.setContentTitle(LocaleController.getString("DownloadingUpdateFail", R.string.DownloadingUpdateFail));
                }
                stopForeground(true);
                getUpdateHelper().getSystemNotificationManager().notify(NOTIFICATION_ID, builder.build());
                stopSelf();
            }
        } else if (id == NotificationCenter.fileLoadFailed || id == NotificationCenter.httpFileDidFailedLoad) {
            if (!canceled && args[0].equals(updateRef.realFilename)) {
                stopForeground(true);
                Intent downloadIntent = new Intent("download-app-update");
                PendingIntent downloadPendingIntent =
                        PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, downloadIntent, 0);
                IntentFilter downloadFilter = new IntentFilter(downloadIntent.getAction());
                BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context arg0, Intent intent) {
                        Intent serviceIntent = new Intent(ApplicationLoader.applicationContext, UpdateService.class);
                        ApplicationLoader.applicationContext.startService(serviceIntent);
                        getUpdateHelper().getSystemNotificationManager().cancel(NOTIFICATION_ID);
                    }
                };
                ApplicationLoader.applicationContext.registerReceiver(downloadReceiver, downloadFilter);
                builder = getUpdateHelper().createNotificationBuilder(false);
                builder = builder.setSmallIcon(R.drawable.notification).
                        setContentTitle(LocaleController.getString("DownloadingUpdateFail", R.string.DownloadingUpdateFail)).
                        setContentText(LocaleController.getString("ClickToRetry", R.string.ClickToRetry)).
                        setContentIntent(downloadPendingIntent).
                        setWhen(System.currentTimeMillis()).
                        setColor(0xff11acfa).
                        setGroupSummary(false);
                getUpdateHelper().getSystemNotificationManager().notify(NOTIFICATION_ID, builder.build());
                stopSelf();
            }
        }
    }

    @Override
    public void onDestroy() {
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileLoadProgressChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileLoaded);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.httpFileDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileLoadFailed);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.httpFileDidFailedLoad);
        super.onDestroy();
    }
}
