package tw.nekomimi.nekogram.helpers;

import android.app.Application;
import android.content.pm.PackageInfo;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.distribute.Distribute;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.Map;

import tw.nekomimi.nekogram.NekoConfig;

public class AnalyticsHelper {
    public static void start(Application application) {
        try {
            if (googlePlay()) {
                AppCenter.start(application, "033a70ca-ea8d-4c2f-8c2c-b37f1b47f766", Analytics.class);
            } else {
                AppCenter.start(application, "033a70ca-ea8d-4c2f-8c2c-b37f1b47f766", Analytics.class, Distribute.class);
            }
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
            if (fragment.getClass().getName().startsWith("org.telegram.ui")) {
                properties.put("fragment_simple_class", fragment.getClass().getSimpleName());
                properties.put("fragment_class", fragment.getClass().getName());
            } else {
                properties.put("fragment_simple_class", "NekoSettings");
                properties.put("fragment_class", "tw.nekomimi.nekogram.settings");
            }
            Analytics.trackEvent("fragment_view", properties);
        }
    }

    public static boolean googlePlay() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return pInfo.versionCode % 10 == 0;
        } catch (Exception e) {
            FileLog.e(e);
            return true;
        }
    }
}
