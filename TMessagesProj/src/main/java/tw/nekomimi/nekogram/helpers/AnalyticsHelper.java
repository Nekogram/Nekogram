package tw.nekomimi.nekogram.helpers;

import android.app.Application;
import android.content.SharedPreferences;
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
    private static SharedPreferences preferences;

    public static boolean sendBugReport = true;
    public static boolean analyticsDisabled = false;

    private static final Channel.Listener patchDeviceListener = new AbstractChannelListener() {
        @Override
        public void onPreparedLog(@NonNull Log log, @NonNull String groupName, int flags) {
            var device = log.getDevice();
            device.setAppVersion(BuildConfig.VERSION_NAME);
            device.setAppBuild(String.valueOf(BuildConfig.VERSION_CODE));
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
        if (BuildConfig.DEBUG) return;
        if ("play".equals(BuildConfig.BUILD_TYPE)) {
            AppCenter.start(application, Extra.APPCENTER_SECRET, Analytics.class);
            patchDevice();
        } else {
            preferences = application.getSharedPreferences("nekoanalytics", Application.MODE_PRIVATE);
            analyticsDisabled = preferences.getBoolean("analyticsDisabled", false);
            if (analyticsDisabled) {
                return;
            }
            AppCenter.start(application, Extra.APPCENTER_SECRET, Analytics.class, Crashes.class);
            patchDevice();
            sendBugReport = preferences.getBoolean("sendBugReport", true);
            Crashes.setEnabled(sendBugReport);
        }
    }

    public static void trackEvent(String event) {
        if (BuildConfig.DEBUG || analyticsDisabled) return;
        Analytics.trackEvent(event);
    }

    public static void trackEvent(String event, HashMap<String, String> map) {
        if (BuildConfig.DEBUG || analyticsDisabled) return;
        Analytics.trackEvent(event, map);
    }

    public static boolean isSettingsAvailable() {
        return !"play".equals(BuildConfig.BUILD_TYPE);
    }

    public static void setAnalyticsDisabled() {
        AnalyticsHelper.analyticsDisabled = true;
        if (BuildConfig.DEBUG) return;
        Analytics.setEnabled(false);
        Crashes.setEnabled(false);
        preferences.edit().putBoolean("analyticsDisabled", true).apply();
    }

    public static void toggleSendBugReport() {
        AnalyticsHelper.sendBugReport = !AnalyticsHelper.sendBugReport;
        if (BuildConfig.DEBUG) return;
        Crashes.setEnabled(sendBugReport);
        preferences.edit().putBoolean("sendBugReport", sendBugReport).apply();
    }
}
