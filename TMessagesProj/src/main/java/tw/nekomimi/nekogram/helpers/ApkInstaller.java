package tw.nekomimi.nekogram.helpers;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.XiaomiUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import tw.nekomimi.nekogram.NekoConfig;

public final class ApkInstaller {
    @SuppressLint("StaticFieldLeak")
    private static AlertDialog dialog;

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
            FileLog.e(e);
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            AlertsCreator.createSimpleAlert(context, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + e.getLocalizedMessage()).show();
        }
    }

    public static void installUpdate(Activity context, TLRPC.Document document) {
        if (context == null || document == null) {
            return;
        }
        if (XiaomiUtilities.isMIUI()) {
            AndroidUtilities.openForView(document, context);
            return;
        }
        var apk = FileLoader.getPathToAttach(document, true);
        if (apk == null) {
            return;
        }
        if (dialog != null && dialog.isShowing()) {
            return;
        }
        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 4, 4, 4, 4));

        RLottieImageView imageView = new RLottieImageView(context);
        imageView.setAutoRepeat(true);
        imageView.setAnimation(R.raw.db_migration_placeholder, 160, 160);
        imageView.playAnimation();
        linearLayout.addView(imageView, LayoutHelper.createLinear(160, 160, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 17, 24, 17, 0));

        TextView textView = new TextView(context);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText(LocaleController.getString("UpdateInstalling", R.string.UpdateInstalling));
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 17, 20, 17, 0));

        TextView textView2 = new TextView(context);
        textView2.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        textView2.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
        textView2.setText(LocaleController.getString("UpdateInstallingRelaunch", R.string.UpdateInstallingRelaunch));
        linearLayout.addView(textView2, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 17, 4, 17, 24));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(linearLayout);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
        Utilities.globalQueue.postRunnable(() -> {
            var receiver = register(context, () -> {
                if (dialog != null) {
                    dialog.dismiss();
                    dialog = null;
                }
            });
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
                    int id = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, 0);
                    if (id > 0) {
                        var installer = context.getPackageManager().getPackageInstaller();
                        var info = installer.getSessionInfo(id);
                        if (info != null) {
                            installer.abandonSession(info.getSessionId());
                        }
                    }
                    if (context instanceof LaunchActivity) {
                        ((LaunchActivity) context).showBulletin(factory -> factory.createErrorBulletin(LocaleController.formatString("UpdateFailedToInstall", R.string.UpdateFailedToInstall, status)));
                    }
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

    public static class UpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) return;

            var packageName = context.getPackageName();
            var installer = context.getPackageManager().getInstallerPackageName(packageName);
            if (!packageName.equals(installer)) return;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Settings.canDrawOverlays(context)) {
                context.startActivity(new Intent(context, LaunchActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                NotificationsController.checkOtherNotificationsChannel();
                NotificationManagerCompat
                        .from(context)
                        .notify(38264,
                                new NotificationCompat.Builder(context, NotificationsController.OTHER_NOTIFICATIONS_CHANNEL)
                                        .setSmallIcon(R.drawable.notification)
                                        .setColor(NekoConfig.getNotificationColor())
                                        .setShowWhen(false)
                                        .setContentText(LocaleController.getString("UpdateInstalledNotification", R.string.UpdateInstalledNotification))
                                        .setCategory(NotificationCompat.CATEGORY_STATUS)
                                        .build());
            }
        }
    }
}
