package tw.nekomimi.nekogram.helpers;

import android.app.Application;
import android.content.Context;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.distribute.Distribute;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import tw.nekomimi.nekogram.NekoConfig;

public class AnalyticsHelper {
    public static void start(Application application) {
        try {
            if (googlePlay(application)) {
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

    //似乎不好使
    public static boolean googlePlay(Context context) {
        try {
            List<String> validInstallers = new ArrayList<>(Arrays.asList("com.android.vending", "com.google.android.feedback"));
            final String installer = context.getPackageManager().getInstallerPackageName(context.getPackageName());
            if (installer != null) {
                if (isEnabled()) {
                    Map<String, String> properties = new java.util.HashMap<>();
                    properties.put("PackageName", installer);
                    Analytics.trackEvent("getInstallerPackageName", properties);
                }
                return validInstallers.contains(installer);
            }
            return false;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
}
