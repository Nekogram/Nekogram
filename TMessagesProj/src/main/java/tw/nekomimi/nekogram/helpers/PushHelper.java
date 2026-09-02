package tw.nekomimi.nekogram.helpers;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class PushHelper {

    private static final Gson GSON = new Gson();

    public static void processRemoteMessage(String data) {
        if (!UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            return;
        }
        try {
            var message = GSON.fromJson(data, RemoteMessage.class);
            var action = message.action;
            switch (action) {
                case "check_app_update":
                    checkAppUpdate();
                    break;
                case "load_remote_config":
                    ConfigHelper.getInstance().load();
                    break;
                case "set_remote_config":
                    ConfigHelper.getInstance().onLoadSuccess(message.data);
                    break;
            }
        } catch (Exception e) {
            FileLog.e("failed to do remote action", e);
        }
    }

    private static void checkAppUpdate() {
        UpdateHelper.getInstance().checkNewVersionAvailable((res, error) -> {
            SharedConfig.lastUpdateCheckTime = System.currentTimeMillis();
            SharedConfig.saveConfig();
            AndroidUtilities.runOnUIThread(() -> {
                SharedConfig.setNewAppVersionAvailable(res);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.appUpdateAvailable);
            });
        });
    }

    public static class RemoteMessage {

        @SerializedName("action")
        @Expose
        public String action;

        @SerializedName("data")
        @Expose
        public String data;

    }
}
