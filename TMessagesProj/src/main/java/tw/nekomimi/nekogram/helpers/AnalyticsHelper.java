package tw.nekomimi.nekogram.helpers;

import android.app.Application;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.util.HashMap;

import tw.nekomimi.nekogram.Extra;

public class AnalyticsHelper {
    private static SharedPreferences preferences;

    private static FirebaseAnalytics firebaseAnalytics;

    public static boolean sendBugReport = true;
    public static boolean analyticsDisabled = false;
    public static String userId = null;

    public static void start(Application application) {
        preferences = application.getSharedPreferences("nekoanalytics", Application.MODE_PRIVATE);
        analyticsDisabled = preferences.getBoolean("analyticsDisabled", false) && !Extra.FORCE_ANALYTICS;
        sendBugReport = preferences.getBoolean("sendBugReport", true) && !Extra.FORCE_ANALYTICS;
        if (analyticsDisabled) {
            FileLog.d("Analytics: userId = disabled");
            return;
        }
        userId = preferences.getString("userId", null);
        if (userId == null || userId.length() < 32) {
            preferences.edit().putString("userId", userId = generateUserID()).apply();
        }
        firebaseAnalytics = FirebaseAnalytics.getInstance(application);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        firebaseAnalytics.setUserId(userId);
        var crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setUserId(userId);
        crashlytics.setCustomKey("version_code", BuildConfig.VERSION_CODE);
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE);
        crashlytics.setCrashlyticsCollectionEnabled(true);

        FileLog.d("Analytics: userId = " + userId);
    }

    private static String generateUserID() {
        return Utilities.generateRandomString(32);
    }

    public static void trackScreenView() {
        trackEvent(FirebaseAnalytics.Event.SCREEN_VIEW);
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
        return !Extra.FORCE_ANALYTICS;
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
