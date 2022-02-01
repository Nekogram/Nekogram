package tw.nekomimi.nekogram.helpers.remote;

import android.app.Application;

import com.microsoft.appcenter.AppCenter;
import com.microsoft.appcenter.analytics.Analytics;
import com.microsoft.appcenter.crashes.Crashes;

import java.util.HashMap;

import tw.nekomimi.nekogram.Extra;

public class AnalyticsHelper {
    public static void start(Application application) {
        AppCenter.start(application, Extra.APPCENTER_SECRET, Analytics.class, Crashes.class);
    }

    public static void trackEvent(String event) {
        Analytics.trackEvent(event);
    }

    public static void trackEvent(String event, HashMap<String, String> map) {
        Analytics.trackEvent(event, map);
    }
}
