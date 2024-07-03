package tw.nekomimi.nekogram.helpers.remote;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

public class UpdateHelper extends BaseRemoteHelper {
    public static final String UPDATE_METHOD = "check_for_updates";

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
            return LocaleController.formatString(R.string.LastUpdateNever);
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
                    return LocaleController.formatString(R.string.LastUpdateRecently);
                }
                return LocaleController.formatString(R.string.LastUpdateFormatted, LocaleController.formatString(R.string.TodayAtFormatted,
                        LocaleController.getInstance().getFormatterDay().format(new Date(date))));
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString(R.string.LastUpdateFormatted, LocaleController.formatString(R.string.YesterdayAtFormatted,
                        LocaleController.getInstance().getFormatterDay().format(new Date(date))));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                String format = LocaleController.formatString(R.string.formatDateAtTime,
                        LocaleController.getInstance().getFormatterDayMonth().format(new Date(date)),
                        LocaleController.getInstance().getFormatterDay().format(new Date(date)));
                return LocaleController.formatString(R.string.LastUpdateDateFormatted, format);
            } else {
                String format = LocaleController.formatString(R.string.formatDateAtTime,
                        LocaleController.getInstance().getFormatterYear().format(new Date(date)),
                        LocaleController.getInstance().getFormatterDay().format(new Date(date)));
                return LocaleController.formatString(R.string.LastUpdateDateFormatted, format);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    private static final class InstanceHolder {
        private static final UpdateHelper instance = new UpdateHelper();
    }

    public static UpdateHelper getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    protected void onError(String text, Delegate delegate) {
        delegate.onTLResponse(null, text);
    }

    @Override
    protected String getRequestMethod() {
        return UPDATE_METHOD;
    }

    @Override
    protected String getRequestParams() {
        return " " + TextUtils.join(",", Build.SUPPORTED_ABIS);
    }

    @Override
    protected void onLoadSuccess(ArrayList<TLRPC.BotInlineResult> results, Delegate delegate) {
        var map = results.stream()
                .collect(Collectors.toMap(result -> result.id, result -> result));
        var update_info = map.get("update_info");
        if (update_info == null) {
            delegate.onTLResponse(null, null);
            return;
        }
        var update = new TLRPC.TL_help_appUpdate();
        var json = GSON.fromJson(getTextFromInlineResult(update_info), Update.class);
        if (json == null || json.versionCode <= BuildConfig.VERSION_CODE) {
            delegate.onTLResponse(null, null);
            return;
        }
        update.version = json.version;
        update.can_not_skip = json.canNotSkip;
        if (json.url != null) {
            update.url = json.url;
            update.flags |= 4;
        }
        var document = map.get("document");
        if (document != null && document.document != null) {
            update.document = document.document;
            update.flags |= 2;
        }
        var message = map.get("message");
        if (message != null && message.send_message != null) {
            update.text = message.send_message.message;
            update.entities = message.send_message.entities;
            var entities = map.get("entities");
            if (entities != null) {
                var entities_json = GSON.fromJson(getTextFromInlineResult(entities), Entity[].class);
                Arrays.stream(entities_json)
                        .filter(e -> e.customEmojiId != null)
                        .map(e -> {
                            var entity = new TLRPC.TL_messageEntityCustomEmoji();
                            entity.document_id = e.customEmojiId;
                            entity.offset = e.offset;
                            entity.length = e.length;
                            return entity;
                        })
                        .forEach(e -> update.entities.add(e));
            }
        }
        var sticker = map.get("sticker");
        if (sticker != null && sticker.document != null) {
            update.sticker = sticker.document;
            update.flags |= 8;
        }
        delegate.onTLResponse(update, null);
    }

    public void checkNewVersionAvailable(Delegate delegate) {
        load(delegate);
        ConfigHelper.getInstance().load();
    }

    public static class Update {
        @SerializedName("can_not_skip")
        @Expose
        public Boolean canNotSkip;
        @SerializedName("version")
        @Expose
        public String version;
        @SerializedName("version_code")
        @Expose
        public Integer versionCode;
        @SerializedName("url")
        @Expose
        public String url;
    }

    public static class Entity {
        @SerializedName("custom_emoji_id")
        @Expose
        public Long customEmojiId;
        @SerializedName("length")
        @Expose
        public Integer length;
        @SerializedName("offset")
        @Expose
        public Integer offset;
    }
}
