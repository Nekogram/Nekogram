package tw.nekomimi.nekogram.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CheckBoxSquare;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.TextViewSwitcher;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import tw.nekomimi.nekogram.NekoConfig;

public class CloudSettingsHelper {
    private static final int CONFIG_VERSION = 0;

    private final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekocloud", Context.MODE_PRIVATE);
    private final long[] cloudSyncedDate = new long[UserConfig.MAX_ACCOUNT_COUNT];
    private final Handler handler = new Handler();
    private final Runnable cloudSyncRunnable = () -> CloudSettingsHelper.getInstance().syncToCloud((success, error) -> {
        if (!success) {
            var global = BulletinFactory.global();
            if (error == null) {
                global.createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.CloudConfigSyncFailed)).show();
            } else {
                global.createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.CloudConfigSyncFailed), error).show();
            }
        }
    });

    private long localSyncedDate = preferences.getLong("updated_at", -1);
    private boolean autoSync = preferences.getBoolean("auto_sync", false);

    private static final class InstanceHolder {
        private static final CloudSettingsHelper instance = new CloudSettingsHelper();
    }

    public static CloudSettingsHelper getInstance() {
        return InstanceHolder.instance;
    }

    public void showDialog(BaseFragment parentFragment) {
        if (parentFragment == null) {
            return;
        }

        Context context = parentFragment.getParentActivity();
        Theme.ResourcesProvider resourcesProvider = parentFragment.getResourceProvider();
        int selectedAccount = UserConfig.selectedAccount;

        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString(R.string.CloudConfig));
        builder.setMessage(AndroidUtilities.replaceTags(LocaleController.getString(R.string.CloudConfigDesc)));
        builder.setTopImage(R.drawable.cloud, Theme.getColor(Theme.key_dialogTopBackground, resourcesProvider));

        TextViewSwitcher syncedDate = new TextViewSwitcher(context);
        syncedDate.setFactory(() -> {
            TextView tv = new TextView(context);
            tv.setGravity(Gravity.START);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            tv.setTextColor(Theme.getColor(Theme.key_dialogTextGray3, resourcesProvider));
            return tv;
        });
        syncedDate.setInAnimation(context, R.anim.alpha_in);
        syncedDate.setOutAnimation(context, R.anim.alpha_out);
        syncedDate.setText(formatSyncedDate(), false);

        var storageHelper = getCloudStorageHelper();
        storageHelper.getItem("neko_settings_updated_at", (res, error) -> {
            if (error == null && AndroidUtilities.isNumeric(res)) {
                cloudSyncedDate[selectedAccount] = Long.parseLong(res);
            } else {
                cloudSyncedDate[selectedAccount] = -1;
            }
            syncedDate.setText(formatSyncedDate());
        });

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        ButtonWithCounterView buttonTextView = new ButtonWithCounterView(context, true, resourcesProvider);
        buttonTextView.setText(LocaleController.getString(R.string.CloudConfigSync), false);
        linearLayout.addView(buttonTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 0, 16, 0));
        buttonTextView.setOnClickListener(view -> {
            syncedDate.setText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.CloudConfigSyncing)));
            syncToCloud((success, error) -> {
                syncedDate.setText(formatSyncedDate());
                if (!success) {
                    if (error == null) {
                        BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.CloudConfigSyncFailed)).show();
                    } else {
                        BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.CloudConfigSyncFailed), error).show();
                    }
                }
            });
        });

        ButtonWithCounterView textView = new ButtonWithCounterView(context, false, resourcesProvider);
        textView.setText(LocaleController.getString(R.string.CloudConfigRestore), false);
        linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, 16, 8, 16, 0));
        textView.setOnClickListener(view -> {
            syncedDate.setText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.CloudConfigSyncing)));
            restoreFromCloud((success, error) -> {
                syncedDate.setText(formatSyncedDate());
                if (!success) {
                    if (error == null) {
                        BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.CloudConfigRestoreFailed)).show();
                    } else {
                        BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.chats_infotip, LocaleController.getString(R.string.CloudConfigRestoreFailed), error).show();
                    }
                }
            });
        });

        MiniCheckBoxCell autoSyncCheck = new MiniCheckBoxCell(context, 8, resourcesProvider);
        autoSyncCheck.setTextAndValueAndCheck(LocaleController.getString(R.string.CloudConfigAutoSync), LocaleController.getString(R.string.CloudConfigAutoSyncDesc), autoSync);
        autoSyncCheck.setOnClickListener(view13 -> {
            autoSync = !autoSync;
            preferences.edit().putBoolean("auto_sync", autoSync).apply();
            autoSyncCheck.setChecked(autoSync);
        });
        linearLayout.addView(autoSyncCheck, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 8, 8, 8, 0));

        linearLayout.addView(syncedDate, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 16, 8, 16, 0));

        builder.setView(linearLayout);
        parentFragment.showDialog(builder.create());
    }

    public void doAutoSync() {
        if (!autoSync) {
            return;
        }
        handler.removeCallbacks(cloudSyncRunnable);
        handler.postDelayed(cloudSyncRunnable, 1200);
    }

    private void syncToCloud(Utilities.Callback2<Boolean, String> callback) {
        String rawConfig = NekoConfig.exportConfigs();
        String compressed = encodeConfig(rawConfig);
        getCloudStorageHelper().setItem("neko_settings", rawConfig.length() >= compressed.length() ? compressed : rawConfig, (res, error) -> {
            if (error == null) {
                localSyncedDate = cloudSyncedDate[UserConfig.selectedAccount] = System.currentTimeMillis();
                getCloudStorageHelper().setItem("neko_settings_updated_at", String.valueOf(localSyncedDate), null);
                preferences.edit().putLong("updated_at", localSyncedDate).apply();
                callback.run(true, null);
            } else {
                callback.run(false, error);
            }
        });
    }

    private void restoreFromCloud(Utilities.Callback2<Boolean, String> callback) {
        getCloudStorageHelper().getItem("neko_settings", (res, error) -> {
            if (error == null) {
                if (TextUtils.isEmpty(res)) {
                    callback.run(false, "EMPTY_CONFIG");
                } else {
                    String config = decodeConfig(res);
                    if (config == null) {
                        callback.run(false, "DECODE_FAILED");
                    } else {
                        try {
                            NekoConfig.importConfigs(config);
                            localSyncedDate = System.currentTimeMillis();
                            preferences.edit().putLong("updated_at", localSyncedDate).apply();
                            callback.run(true, null);
                        } catch (Exception e) {
                            FileLog.e(e);
                            callback.run(false, e.getLocalizedMessage());
                        }
                    }
                }
            } else {
                callback.run(false, error);
            }
        });
    }

    private CloudStorageHelper getCloudStorageHelper() {
        return CloudStorageHelper.getInstance(UserConfig.selectedAccount);
    }

    private String formatSyncedDate() {
        return LocaleController.formatString(
                R.string.CloudConfigSyncDate,
                localSyncedDate > 0 ? formatDateUntil(localSyncedDate) : LocaleController.getString(R.string.CloudConfigSyncDateNever),
                cloudSyncedDate[UserConfig.selectedAccount] > 0 ? formatDateUntil(cloudSyncedDate[UserConfig.selectedAccount]) : LocaleController.getString(R.string.CloudConfigSyncDateNever));
    }

    public static String encodeConfig(String string) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(string.length());
            GZIPOutputStream gzip = new GZIPOutputStream(bos);
            gzip.write(string.getBytes());
            gzip.close();
            byte[] compressed = bos.toByteArray();
            bos.close();
            return CONFIG_VERSION + Base64.encodeToString(compressed, Base64.NO_PADDING | Base64.NO_WRAP);
        } catch (Exception e) {
            FileLog.e(e);
            return string;
        }
    }

    private static String decodeConfig(String string) {
        if (string.startsWith("{")) {
            return string;
        } else if (string.startsWith(String.valueOf(CONFIG_VERSION))) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(Base64.decode(string.substring(1), Base64.DEFAULT));
                GZIPInputStream gis = new GZIPInputStream(bis);
                //noinspection CharsetObjectCanBeUsed
                String config = new Scanner(gis, "UTF-8")
                        .useDelimiter("\\A")
                        .next();
                gis.close();
                bis.close();
                return config;
            } catch (Exception e) {
                FileLog.e(e);
                return null;
            }
        } else {
            return null;
        }
    }

    private static String formatDateUntil(long date) {
        try {
            Calendar rightNow = Calendar.getInstance();
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (year == dateYear) {
                return LocaleController.getInstance().getFormatterBannedUntilThisYear().format(new Date(date));
            } else {
                return LocaleController.getInstance().getFormatterBannedUntil().format(new Date(date));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    @SuppressLint("ViewConstructor")
    private static class MiniCheckBoxCell extends FrameLayout {

        private final TextView textView;
        private final TextView valueTextView;
        private final CheckBoxSquare checkBox;

        public MiniCheckBoxCell(Context context, int padding, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            ScaleStateListAnimator.apply(this, .02f, 1.2f);

            setForeground(Theme.createRadSelectorDrawable(Theme.multAlpha(Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider), .10f), 8, 8));

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            linearLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            valueTextView = new TextView(context);
            valueTextView.setTextColor(Theme.getColor(Theme.key_dialogIcon, resourcesProvider));
            valueTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            valueTextView.setEllipsize(TextUtils.TruncateAt.END);
            linearLayout.addView(valueTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));

            addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, LocaleController.isRTL ? 22 + padding : padding, 4, LocaleController.isRTL ? padding : 22 + padding, 4));

            checkBox = new CheckBoxSquare(context, true, resourcesProvider);
            addView(checkBox, LayoutHelper.createFrame(18, 18, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL, LocaleController.isRTL ? padding : 4, 0, LocaleController.isRTL ? 4 : padding, 0));
        }

        public void setTextAndValueAndCheck(String text, String value, boolean checked) {
            textView.setText(text);
            valueTextView.setText(value);
            checkBox.setChecked(checked, false);
        }

        public void setChecked(boolean checked) {
            checkBox.setChecked(checked, true);
        }

        public boolean isChecked() {
            return checkBox.isChecked();
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            info.setClassName("android.widget.CheckBox");
            info.setCheckable(true);
            info.setChecked(checkBox.isChecked());
            StringBuilder sb = new StringBuilder();
            sb.append(textView.getText());
            if (!TextUtils.isEmpty(valueTextView.getText())) {
                sb.append('\n');
                sb.append(valueTextView.getText());
            }
            info.setContentDescription(sb);
        }
    }
}
