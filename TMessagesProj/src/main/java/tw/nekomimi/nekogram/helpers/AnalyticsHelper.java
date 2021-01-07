package tw.nekomimi.nekogram.helpers;

import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;

import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.Map;

import tw.nekomimi.nekogram.NekoConfig;

public class AnalyticsHelper {
    public static final boolean GOOGLE_PLAY = false;

    public static void start(Application application) {
        try {
            AppCenter.start(application, "033a70ca-ea8d-4c2f-8c2c-b37f1b47f766", Analytics.class);
            AppCenter.setUserId(String.valueOf(UserConfig.getInstance(UserConfig.selectedAccount).clientUserId));
        } catch (Exception ignore) {
            //
        }
    }

    public static void trackEvent(String event) {
        if (isEnabled()) {
            Analytics.trackEvent(event);
        }
    }

    public static boolean isEnabled() {
        return AppCenter.isConfigured() && NekoConfig.enableAnalytics;
    }

    public static void onFragmentView(BaseFragment fragment) {
        if (isEnabled()) {
            Map<String, String> properties = new java.util.HashMap<>();
            properties.put("fragment_simple_class", fragment.getClass().getSimpleName());
            properties.put("fragment_class", fragment.getClass().getName());
            Analytics.trackEvent("fragment_view", properties);
        }
    }
}
