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
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
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
        File f = getMessageAttachment(updateRef.message);
        installIntent.setDataAndType(FileProvider.getUriForFile(this,
                BuildConfig.APPLICATION_ID + ".provider", f), "application/vnd.android.package-archive");
        PendingIntent installPendingIntent = PendingIntent.getActivity(this, 500, installIntent, 0);
        if (f.exists()) {
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
            AccountInstance.getInstance(UserConfig.selectedAccount).getFileLoader().loadFile(
                    updateRef.document, updateRef.message, 0, 0
            );
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.FileLoadProgressChanged);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileDidLoad);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.httpFileDidLoad);
            NotificationCenter.getInstance(UserConfig.selectedAccount).addObserver(this, NotificationCenter.fileDidFailToLoad);
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
        if (id == NotificationCenter.FileLoadProgressChanged) {
            if (args[0].equals(updateRef.realFilename)) {
                getUpdateHelper().getSystemNotificationManager().notify(NOTIFICATION_ID_PROGRESS,
                        builder.setProgress((int) (long) args[2], (int) (long) args[1], false).build());
            }
        } else if (id == NotificationCenter.fileDidLoad || id == NotificationCenter.httpFileDidLoad) {
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
                File f = null;
                if (updateRef.message.attachPath != null && updateRef.message.attachPath.length() != 0) {
                    f = new File(updateRef.message.attachPath);
                }
                if (f == null || !f.exists()) {
                    f = FileLoader.getPathToMessage(updateRef.message);
                }
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
        } else if (id == NotificationCenter.fileDidFailToLoad || id == NotificationCenter.httpFileDidFailedLoad) {
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
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.FileLoadProgressChanged);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.httpFileDidLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.fileDidFailToLoad);
        NotificationCenter.getInstance(UserConfig.selectedAccount).removeObserver(this, NotificationCenter.httpFileDidFailedLoad);
        super.onDestroy();
    }

    private static File getMessageAttachment(TLRPC.Message messageOwner) {
        File f = null;
        if (messageOwner.attachPath != null && messageOwner.attachPath.length() != 0) {
            f = new File(messageOwner.attachPath);
        }
        if (f == null || !f.exists()) {
            f = FileLoader.getPathToMessage(messageOwner);
        }
        return f;
    }
}
