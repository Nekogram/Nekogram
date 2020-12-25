package tw.nekomimi.nekogram.updater;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.NekoConfig;

public class UpdateHelper {

    private static volatile UpdateHelper Instance;
    NotificationManager systemNotificationManager = null;
    private static final String NOTIFICATION_CHANNEL_ID = "nekogram-update";
    private static final String NOTIFICATION_CHANNEL_ID_PROGRESS = "nekogram-update-progress-2";
    private static final int NOTIFICATION_ID = 78985;
    UpdateRef updatingRef;
    ArrayList<UpdateRef> updateRefs = new ArrayList<>();

    UpdateHelper() {
    }

    /**
     * @param date {long} - date in milliseconds
     */
    public static String formatDateUpdate(long date) {
        long epoch;
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            epoch = pInfo.lastUpdateTime;
        } catch (Exception e) {
            epoch = 0;
        }
        if (date <= epoch) {
            return LocaleController.formatString("LastUpdateNever", R.string.LastUpdateNever);
        }
        try {
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                if (Math.abs(System.currentTimeMillis() - date) < 60000L) {
                    return LocaleController.formatString("LastUpdateRecently", R.string.LastUpdateRecently);
                }
                return LocaleController.formatString("LastUpdateFormatted", R.string.LastUpdateFormatted, LocaleController.formatString("TodayAtFormatted", R.string.TodayAtFormatted,
                        LocaleController.getInstance().formatterDay.format(new Date(date))));
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString("LastUpdateFormatted", R.string.LastUpdateFormatted, LocaleController.formatString("YesterdayAtFormatted", R.string.YesterdayAtFormatted,
                        LocaleController.getInstance().formatterDay.format(new Date(date))));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime,
                        LocaleController.getInstance().formatterDayMonth.format(new Date(date)),
                        LocaleController.getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LastUpdateDateFormatted", R.string.LastUpdateDateFormatted, format);
            } else {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime,
                        LocaleController.getInstance().formatterYear.format(new Date(date)),
                        LocaleController.getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LastUpdateDateFormatted", R.string.LastUpdateDateFormatted, format);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    public interface UpdateHelperDelegate {
        int getClassGuid();

        void didCheckNewVersionAvailable(String error);
    }

    public static UpdateHelper getInstance() {
        UpdateHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (UpdateHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new UpdateHelper();
                }
                return localInstance;
            }
        }
        return localInstance;
    }

    protected MessagesController getMessagesController() {
        return MessagesController.getInstance(UserConfig.selectedAccount);
    }

    protected ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(UserConfig.selectedAccount);
    }

    protected MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(UserConfig.selectedAccount);
    }

    static class UpdateRef {
        int versionCode;
        String versionName;
        String abi;
        String filename;
        String realFilename;
        TLRPC.Document document;
        TLRPC.Message message;

        UpdateRef(int versionCode, String versionName, String abi, String filename, TLRPC.Document document, TLRPC.Message message) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.abi = abi;
            this.filename = filename;
            this.document = document;
            this.message = message;
            this.realFilename = FileLoader.getAttachFileName(document);
        }
    }

    private void getShouldUpdateVersion(ArrayList<TLRPC.Message> messages) {
        for (TLRPC.Message message : messages) {
            if (!(message.media instanceof TLRPC.TL_messageMediaDocument)) {
                continue;
            }
            TLRPC.TL_messageMediaDocument mediaDocument = (TLRPC.TL_messageMediaDocument) message.media;
            TLRPC.Document document = mediaDocument.document;
            if (!document.mime_type.equals("application/vnd.android.package-archive")) {
                continue;
            }
            for (TLRPC.DocumentAttribute attribute : document.attributes) {
                if (!(attribute instanceof TLRPC.TL_documentAttributeFilename)) {
                    continue;
                }
                TLRPC.TL_documentAttributeFilename filename = (TLRPC.TL_documentAttributeFilename) attribute;
                Pattern r = Pattern.compile("^Nekogram-([^-]+)-(\\d+)-(.+)\\.apk$");
                Matcher m = r.matcher(filename.file_name);
                if (m.find()) {
                    int ver;
                    try {
                        //noinspection ConstantConditions
                        ver = Integer.parseInt(m.group(2));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    this.updateRefs.add(new UpdateRef(ver, m.group(1), m.group(3), filename.file_name, document, message));
                }
            }
        }
    }

    private boolean shouldGreyScaleReleaseNotifyUser(long lastCheckDate, UpdateRef updateRef) {
        try {
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(lastCheckDate);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                return false;
            }
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
        long date = updateRef.document.date * 1000L;
        long currentDate = System.currentTimeMillis();
        final long day = 86400000L;
        Random random = new Random();
        if (currentDate - date > 6 * day) {
            return true;
        } else if (currentDate - date > 5 * day) {
            return random.nextInt(100) < 50;
        } else if (currentDate - date > 4 * day) {
            return random.nextInt(100) < 20;
        } else if (currentDate - date > 3 * day) {
            return random.nextInt(100) < 10;
        } else if (currentDate - date > 2 * day) {
            return random.nextInt(100) < 5;
        } else if (currentDate - date > day) {
            return random.nextInt(100) < 2;
        } else {
            return random.nextInt(100) < 1;
        }
    }

    private void checkNewVersionTLCallback(@Nullable UpdateHelperDelegate delegate, boolean isAutoUpdate, int dialog_id,
                                           TLObject response, TLRPC.TL_error error) {
        long lastDate = NekoConfig.lastSuccessfulCheckUpdateTime;
        if (error == null) {
            final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            getMessagesController().removeDeletedMessagesFromArray(dialog_id, res.messages);
            getShouldUpdateVersion(res.messages);
            if (delegate != null && updateRefs.size() == 0) {
                delegate.didCheckNewVersionAvailable(null); // no available version
                return;
            }
            int code;
            String abi = "";
            try {
                PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
                code = pInfo.versionCode / 10;
                switch (pInfo.versionCode % 10) {
                    case 1:
                    case 3:
                        abi = "armeabi-v7a";
                        break;
                    case 2:
                    case 4:
                        abi = "x86";
                        break;
                    case 5:
                    case 7:
                        abi = "arm64-v8a";
                        break;
                    case 6:
                    case 8:
                        abi = "x86_64";
                        break;
                    case 0:
                    case 9:
                        abi = "universal";
                        break;
                }
            } catch (Exception exception) {
                if (delegate != null) {
                    delegate.didCheckNewVersionAvailable(exception.getLocalizedMessage());
                }
                return;
            }
            int lastVer = 0;
            UpdateRef updateRef = null;
            for (UpdateRef ref : updateRefs) {
                if (ref.versionCode > code && ref.abi.equals(abi) && ref.versionCode >= lastVer) {
                    lastVer = ref.versionCode;
                    updateRef = ref;
                }
            }
            NekoConfig.setLastSuccessfulCheckUpdateTime(System.currentTimeMillis());
            if (updateRef != null) {
                if (!isAutoUpdate || shouldGreyScaleReleaseNotifyUser(lastDate, updateRef)) {
                    askIfShouldDownloadAPK(updateRef);
                }
            }
            if (delegate != null) delegate.didCheckNewVersionAvailable(null);
        } else {
            if (delegate != null) delegate.didCheckNewVersionAvailable(error.text);
        }
    }

    public void checkNewVersionAvailable(@Nullable UpdateHelperDelegate delegate, boolean isAutoUpdate) {
        checkNewVersionAvailable(delegate, isAutoUpdate, false);
    }

    public void checkNewVersionAvailable(@Nullable UpdateHelperDelegate delegate, boolean isAutoUpdate, boolean forceRefreshAccessHash) {
        TLRPC.TL_contacts_resolveUsername req1 = new TLRPC.TL_contacts_resolveUsername();
        int dialog_id = -1302242053;
        req1.username = "NekogramAPKs";
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.limit = 10;
        req.offset_id = 0;
        req.filter = new TLRPC.TL_inputMessagesFilterDocument();
        req.q = "";
        req.peer = getMessagesController().getInputPeer(dialog_id);
        if (req.peer == null || req.peer.access_hash == 0 || forceRefreshAccessHash) {
            getConnectionsManager().sendRequest(req1, (response1, error1) -> {
                if (delegate != null && error1 != null) {
                    delegate.didCheckNewVersionAvailable(error1.text);
                    return;
                }
                if (delegate != null && !(response1 instanceof TLRPC.TL_contacts_resolvedPeer)) {
                    delegate.didCheckNewVersionAvailable("Unexpected TL_contacts_resolvedPeer response");
                    return;
                }
                TLRPC.TL_contacts_resolvedPeer resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) response1;
                getMessagesController().putUsers(resolvedPeer.users, false);
                getMessagesController().putChats(resolvedPeer.chats, false);
                getMessagesStorage().putUsersAndChats(resolvedPeer.users, resolvedPeer.chats, false, true);
                if (delegate != null && (resolvedPeer.chats == null || resolvedPeer.chats.size() == 0)) {
                    delegate.didCheckNewVersionAvailable("Unexpected TL_contacts_resolvedPeer chat size");
                    return;
                }
                req.peer = new TLRPC.TL_inputPeerChannel();
                req.peer.channel_id = resolvedPeer.chats.get(0).id;
                req.peer.access_hash = resolvedPeer.chats.get(0).access_hash;
                int reqId = getConnectionsManager().sendRequest(req, (response, error) -> checkNewVersionTLCallback(delegate, isAutoUpdate, dialog_id, response, error));
                if (delegate != null) {
                    getConnectionsManager().bindRequestToGuid(reqId, delegate.getClassGuid());
                }
            });
        } else {
            int reqId = getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error != null) {
                    checkNewVersionAvailable(delegate, isAutoUpdate, true);
                    return;
                }
                checkNewVersionTLCallback(delegate, isAutoUpdate, dialog_id, response, null);
            });
            if (delegate != null) {
                getConnectionsManager().bindRequestToGuid(reqId, delegate.getClassGuid());
            }
        }
    }

    public NotificationManager getSystemNotificationManager() {
        if (systemNotificationManager == null) {
            systemNotificationManager = (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return systemNotificationManager;
    }

    public NotificationCompat.Builder createNotificationBuilder(boolean isProgress) {
        String id = isProgress ? NOTIFICATION_CHANNEL_ID_PROGRESS : NOTIFICATION_CHANNEL_ID;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            int importance = isProgress ? NotificationManager.IMPORTANCE_LOW : NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = getSystemNotificationManager().getNotificationChannel(id);
            if (notificationChannel == null || notificationChannel.getImportance() != importance) {
                if (notificationChannel != null) {
                    getSystemNotificationManager().deleteNotificationChannel(id);
                }
                notificationChannel = new NotificationChannel(id,
                        LocaleController.getString("Update", R.string.Update), importance);
                if (!isProgress) {
                    notificationChannel.enableLights(true);
                    notificationChannel.enableVibration(true);
                    AudioAttributes.Builder audioBuilder = new AudioAttributes.Builder();
                    audioBuilder.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION);
                    audioBuilder.setUsage(AudioAttributes.USAGE_NOTIFICATION);
                    notificationChannel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, audioBuilder.build());
                } else {
                    notificationChannel.enableLights(false);
                    notificationChannel.enableVibration(false);
                    notificationChannel.setSound(null, null);
                }
                getSystemNotificationManager().createNotificationChannel(notificationChannel);
            }
        }
        return new NotificationCompat.Builder(ApplicationLoader.applicationContext, id);
    }

    private void askIfShouldDownloadAPK(UpdateRef updateRef) {
        updatingRef = updateRef;
        Intent activityIntent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, activityIntent, 0);
        Intent downloadIntent = new Intent("download-app-update");
        PendingIntent downloadPendingIntent =
                PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, downloadIntent, 0);
        IntentFilter downloadFilter = new IntentFilter(downloadIntent.getAction());
        BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                Intent serviceIntent = new Intent(ApplicationLoader.applicationContext, UpdateService.class);
                ApplicationLoader.applicationContext.startService(serviceIntent);
                getSystemNotificationManager().cancel(NOTIFICATION_ID);
            }
        };
        ApplicationLoader.applicationContext.registerReceiver(downloadReceiver, downloadFilter);
        Intent dismissIntent = new Intent("update-gugugu");
        PendingIntent dismissPendingIntent =
                PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, dismissIntent, 0);
        IntentFilter dismissFilter = new IntentFilter(dismissIntent.getAction());
        BroadcastReceiver dismissReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                getSystemNotificationManager().cancel(NOTIFICATION_ID);
            }
        };
        ApplicationLoader.applicationContext.registerReceiver(dismissReceiver, dismissFilter);
        NotificationCompat.Builder builder = createNotificationBuilder(false);
        @SuppressLint("DefaultLocale") Notification notification = builder.setSmallIcon(R.drawable.notification).
                setContentTitle(LocaleController.getString("NewUpdateAvailable", R.string.NewUpdateAvailable)).
                setContentText(String.format("%s (%d)", updateRef.versionName, updateRef.versionCode)).
                setContentIntent(pendingIntent).
                addAction(R.drawable.volume_off, LocaleController.getString("GuGuGu", R.string.GuGuGu), dismissPendingIntent).
                addAction(R.drawable.msg_download, LocaleController.getString("DownloadNow", R.string.DownloadNow), downloadPendingIntent).
                setWhen(System.currentTimeMillis()).
                setSound(Settings.System.DEFAULT_NOTIFICATION_URI, AudioManager.STREAM_NOTIFICATION).
                setPriority(Notification.PRIORITY_MAX).
                setColor(0xff11acfa).
                setGroupSummary(false).
                build();
        getSystemNotificationManager().notify(NOTIFICATION_ID, notification);
    }
}
