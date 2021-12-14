package tw.nekomimi.nekogram.helpers;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.Bulletin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class ApkInstaller {
    // @WorkerThread
    private static void installapk(Context context, File apk) {
        //noinspection InlinedApi
        var flag = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE;
        var action = ApkInstaller.class.getName();
        var intent = new Intent(action).setPackage(context.getPackageName());
        var pending = PendingIntent.getBroadcast(context, 0, intent, flag);

        var installer = context.getPackageManager().getPackageInstaller();
        var params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        try (PackageInstaller.Session session = installer.openSession(installer.createSession(params))) {
            OutputStream out = session.openWrite(apk.getName(), 0, apk.length());
            try (var in = new FileInputStream(apk); out) {
                transfer(in, out);
            }
            session.commit(pending.getIntentSender());
        } catch (IOException e) {
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needShowAlert, 2, e.getLocalizedMessage(), "");
            Log.e(ApkInstaller.class.getSimpleName(), "", e);
        }
    }

    public static void installUpdate(Context context, TLRPC.Document document) {
        if (context == null || document == null) {
            return;
        }
        var apk = FileLoader.getPathToAttach(document, true);
        if (apk == null) {
            return;
        }
        AlertDialog dialog = new AlertDialog(context, 1);
        dialog.setMessage(LocaleController.getString("UpdateInstalling", R.string.UpdateInstalling));
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
        Utilities.globalQueue.postRunnable(() -> {
            var receiver = register(context, dialog::dismiss);
            installapk(context, apk);
            Intent intent = receiver.waitIntent();
            if (intent != null) {
                context.startActivity(intent);
            }
        });
    }

    private static void transfer(InputStream in, OutputStream out) throws IOException {
        int size = 8192;
        var buffer = new byte[size];
        int read;
        while ((read = in.read(buffer, 0, size)) >= 0) {
            out.write(buffer, 0, read);
        }
    }

    private static InstallReceiver register(Context context, Runnable onSuccess) {
        var receiver = new InstallReceiver(context, BuildConfig.APPLICATION_ID, onSuccess);
        var filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addDataScheme("package");
        context.registerReceiver(receiver, filter);
        context.registerReceiver(receiver, new IntentFilter(ApkInstaller.class.getName()));
        return receiver;
    }

    private static class InstallReceiver extends BroadcastReceiver {
        private final Context context;
        private final String packageName;
        private final Runnable onSuccess;
        private final CountDownLatch latch = new CountDownLatch(1);
        private Intent intent = null;

        private InstallReceiver(Context context, String packageName, Runnable onSuccess) {
            this.context = context;
            this.packageName = packageName;
            this.onSuccess = onSuccess;
        }

        @Override
        public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_PACKAGE_ADDED.equals(i.getAction())) {
                Uri data = i.getData();
                if (data == null || onSuccess == null) return;
                String pkg = data.getSchemeSpecificPart();
                if (pkg.equals(packageName)) {
                    onSuccess.run();
                    context.unregisterReceiver(this);
                }
                return;
            }
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE_INVALID);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    intent = i.getParcelableExtra(Intent.EXTRA_INTENT);
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_ERROR, LocaleController.formatString("UpdateFailedToInstall", R.string.UpdateFailedToInstall, status));
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_SUCCESS:
                default:
                    if (onSuccess != null) onSuccess.run();
                    context.unregisterReceiver(this);
            }
            latch.countDown();
        }

        // @WorkerThread @Nullable
        public Intent waitIntent() {
            try {
                //noinspection ResultOfMethodCallIgnored
                latch.await(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
            }
            return intent;
        }
    }
}