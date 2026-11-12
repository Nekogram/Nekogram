-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep *; }

-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
# org.telegram.messenger: only the names that native code and reflection resolve at runtime.
# Before changing these rules see TMessagesProj/proguard-checklist.md and run Tools/r8_keep_check.py.
# Static JNI exports Java_org_telegram_messenger_<Class>_<method> (Utilities, Intro, MediaController, MrzRecognizer).
-keepclasseswithmembers,includedescriptorclasses class org.telegram.messenger.* {
    native <methods>;
}
# gifvideo.cpp: FindClass + GetMethodID, called from the native reader.
-keep class org.telegram.messenger.AnimatedFileDrawableStream {
    int read(int, int);
    void cancel();
    boolean isFinishedLoadingFile();
    boolean isCanceled();
    java.lang.String getFinishedFilePath();
}
# NotificationBadge.BADGERS holds class literals, instances are made with Class.newInstance().
-keep class org.telegram.messenger.NotificationBadge$* implements org.telegram.messenger.NotificationBadge$Badger {
    <init>();
}
-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.messenger.AnimatedFileDrawableStream { <methods>; }
-keep class org.telegram.SQLite.SQLiteException { <methods>; }
-keep class org.telegram.tgnet.ConnectionsManager { <methods>; }
-keep class org.telegram.tgnet.NativeByteBuffer { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class org.telegram.utils.code.highlight.PrismaHighlighter {
    private static native <methods>;
}

# https://developers.google.com/ml-kit/known-issues#android_issues
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }

# WidgetSandbox: JNI_OnLoad uses RegisterNatives; backend methods are called by native workers.
-keep class org.telegram.utils.twidget.WidgetSandboxNative { *; }
# The client's share (filters, gesture arbiter) in the client library: static Java_* exports, resolved by name.
-keepclasseswithmembers,includedescriptorclasses class org.telegram.utils.twidget.WidgetClientNative {
    native <methods>;
}
-keep class org.telegram.utils.twidget.WidgetSandboxBackend { *; }
# The isolated engine process's backend (WidgetEngineService): the same JNI callbacks.
-keep class org.telegram.utils.twidget.WidgetEngineBackend { *; }

# Huawei Services
-keep class com.huawei.hianalytics.**{ *; }
-keep class com.huawei.updatesdk.**{ *; }
-keep class com.huawei.hms.**{ *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

-keep class io.nano.tex.** {*;}

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# Use -keep to explicitly keep any other classes shrinking would remove
#-dontoptimize
#-dontobfuscate

-keepnames class ** extends org.telegram.ui.ActionBar.BaseFragment

-keepclassmembernames class androidx.core.widget.NestedScrollView {
    private android.widget.OverScroller mScroller;
    private void abortAnimatedScroll();
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers enum * {
     public static **[] values();
     public static ** valueOf(java.lang.String);
}

-keepnames class androidx.recyclerview.widget.RecyclerView
-keepclassmembers class androidx.recyclerview.widget.RecyclerView {
    public void suppressLayout(boolean);
    public boolean isLayoutSuppressed();
}

-repackageclasses
-allowaccessmodification
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn android.support.annotation.*
-dontwarn androidx.compose.**
