package tw.nekomimi.nekogram.helpers.remote;

import android.app.Application;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.channel.AbstractChannelListener;
import com.microsoft.appcenter.channel.Channel;
import com.microsoft.appcenter.crashes.Crashes;
import com.microsoft.appcenter.ingestion.models.Log;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;

import java.util.HashMap;

import tw.nekomimi.nekogram.Extra;

public class AnalyticsHelper {
    private static final Channel.Listener patchDeviceListener = new AbstractChannelListener() {
        @Override
        public void onPreparedLog(@NonNull Log log, @NonNull String groupName, int flags) {
            var device = log.getDevice();
            device.setAppVersion(BuildConfig.VERSION_NAME);
            device.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE / 10));
        }
    };

    private static void addPatchDeviceListener() {
        try {
            var channelField = AppCenter.class.getDeclaredField("mChannel");
            channelField.setAccessible(true);
            var channel = (Channel) channelField.get(AppCenter.getInstance());
            assert channel != null;
            channel.addListener(patchDeviceListener);
        } catch (ReflectiveOperationException e) {
            FileLog.e("add listener", e);
        }
    }

    private static void patchDevice() {
        try {
            var handlerField = AppCenter.class.getDeclaredField("mHandler");
            handlerField.setAccessible(true);
            var handler = ((Handler) handlerField.get(AppCenter.getInstance()));
            assert handler != null;
            handler.post(AnalyticsHelper::addPatchDeviceListener);
        } catch (ReflectiveOperationException e) {
            FileLog.e("patch device", e);
        }
    }

    public static void start(Application application) {
        AppCenter.start(application, Extra.APPCENTER_SECRET, Analytics.class, Crashes.class);
        patchDevice();
    }

    public static void trackEvent(String event) {
        Analytics.trackEvent(event);
    }

    public static void trackEvent(String event, HashMap<String, String> map) {
        Analytics.trackEvent(event, map);
    }
}
