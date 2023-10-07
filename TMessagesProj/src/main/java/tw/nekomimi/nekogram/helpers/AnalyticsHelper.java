package tw.nekomimi.nekogram.helpers;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;

import java.util.HashMap;

public class AnalyticsHelper {
    private static SharedPreferences preferences;

    private static FirebaseAnalytics firebaseAnalytics;

    public static boolean sendBugReport = true;
    public static boolean analyticsDisabled = false;

    public static void start(Application application) {
        if ("play".equals(BuildConfig.BUILD_TYPE)) {
            firebaseAnalytics = FirebaseAnalytics.getInstance(application);
            firebaseAnalytics.setAnalyticsCollectionEnabled(true);
            var crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE);
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE);
            crashlytics.setCrashlyticsCollectionEnabled(true);
        } else {
            preferences = application.getSharedPreferences("nekoanalytics", Application.MODE_PRIVATE);
            analyticsDisabled = preferences.getBoolean("analyticsDisabled", false);
            if (analyticsDisabled) {
                return;
            }
            firebaseAnalytics = FirebaseAnalytics.getInstance(application);
            firebaseAnalytics.setAnalyticsCollectionEnabled(true);
            sendBugReport = preferences.getBoolean("sendBugReport", true);
            var crashlytics = FirebaseCrashlytics.getInstance();
            crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE);
            crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE);
            crashlytics.setCrashlyticsCollectionEnabled(true);
        }
    }

    public static void trackEvent(String event) {
        if (analyticsDisabled) return;
        firebaseAnalytics.logEvent(event, new Bundle());
    }

    public static void trackEvent(String event, HashMap<String, String> map) {
        if (analyticsDisabled) return;
        Bundle bundle = new Bundle();
        for (String key : map.keySet()) {
            bundle.putString(key, map.get(key));
        }
        firebaseAnalytics.logEvent(event, bundle);
    }

    public static boolean isSettingsAvailable() {
        return !"play".equals(BuildConfig.BUILD_TYPE);
    }

    public static void setAnalyticsDisabled() {
        AnalyticsHelper.analyticsDisabled = true;
        if (BuildConfig.DEBUG) return;
        FirebaseAnalytics.getInstance(ApplicationLoader.applicationContext).setAnalyticsCollectionEnabled(false);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false);
        preferences.edit().putBoolean("analyticsDisabled", true).apply();
    }

    public static void toggleSendBugReport() {
        AnalyticsHelper.sendBugReport = !AnalyticsHelper.sendBugReport;
        if (BuildConfig.DEBUG) return;
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(sendBugReport);
        preferences.edit().putBoolean("sendBugReport", sendBugReport).apply();
    }
}
