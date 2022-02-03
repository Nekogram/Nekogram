-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
-keep class org.telegram.tgnet.RequestDelegateInternal { *; }
-keep class org.telegram.tgnet.RequestTimeDelegate { *; }
-keep class org.telegram.tgnet.RequestDelegate { *; }
-keep class org.telegram.tgnet.QuickAckDelegate { *; }
-keep class org.telegram.tgnet.WriteToSocketDelegate { *; }
-keep class com.google.android.exoplayer2.ext.** { *; }
-keep class com.google.android.exoplayer2.util.FlacStreamMetadata { *; }
-keep class com.google.android.exoplayer2.metadata.flac.PictureFrame { *; }
-keep class com.google.android.exoplayer2.decoder.SimpleOutputBuffer { *; }

# Constant folding for resource integers may mean that a resource passed to this method appears to be unused. Keep the method to prevent this from happening.
-keep class com.google.android.exoplayer2.upstream.RawResourceDataSource {
  public static android.net.Uri buildRawResourceUri(int);
}

# Methods accessed via reflection in DefaultExtractorsFactory
-dontnote com.google.android.exoplayer2.ext.flac.FlacLibrary
-keepclassmembers class com.google.android.exoplayer2.ext.flac.FlacLibrary {

}

# Some members of this class are being accessed from native methods. Keep them unobfuscated.
-keep class com.google.android.exoplayer2.video.VideoDecoderOutputBuffer {
  *;
}

-dontnote com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer
-keepclassmembers class com.google.android.exoplayer2.ext.opus.LibopusAudioRenderer {
  <init>(android.os.Handler, com.google.android.exoplayer2.audio.AudioRendererEventListener, com.google.android.exoplayer2.audio.AudioProcessor[]);
}
-dontnote com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer
-keepclassmembers class com.google.android.exoplayer2.ext.flac.LibflacAudioRenderer {
  <init>(android.os.Handler, com.google.android.exoplayer2.audio.AudioRendererEventListener, com.google.android.exoplayer2.audio.AudioProcessor[]);
}
-dontnote com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer
-keepclassmembers class com.google.android.exoplayer2.ext.ffmpeg.FfmpegAudioRenderer {
  <init>(android.os.Handler, com.google.android.exoplayer2.audio.AudioRendererEventListener, com.google.android.exoplayer2.audio.AudioProcessor[]);
}

# Constructors accessed via reflection in DefaultExtractorsFactory
-dontnote com.google.android.exoplayer2.ext.flac.FlacExtractor
-keepclassmembers class com.google.android.exoplayer2.ext.flac.FlacExtractor {
  <init>();
}

# Constructors accessed via reflection in DefaultDownloaderFactory
-dontnote com.google.android.exoplayer2.source.dash.offline.DashDownloader
-keepclassmembers class com.google.android.exoplayer2.source.dash.offline.DashDownloader {
  <init>(android.net.Uri, java.util.List, com.google.android.exoplayer2.offline.DownloaderConstructorHelper);
}
-dontnote com.google.android.exoplayer2.source.hls.offline.HlsDownloader
-keepclassmembers class com.google.android.exoplayer2.source.hls.offline.HlsDownloader {
  <init>(android.net.Uri, java.util.List, com.google.android.exoplayer2.offline.DownloaderConstructorHelper);
}
-dontnote com.google.android.exoplayer2.source.smoothstreaming.offline.SsDownloader
-keepclassmembers class com.google.android.exoplayer2.source.smoothstreaming.offline.SsDownloader {
  <init>(android.net.Uri, java.util.List, com.google.android.exoplayer2.offline.DownloaderConstructorHelper);
}

# Constructors accessed via reflection in DownloadHelper
-dontnote com.google.android.exoplayer2.source.dash.DashMediaSource$Factory
-keepclasseswithmembers class com.google.android.exoplayer2.source.dash.DashMediaSource$Factory {
  <init>(com.google.android.exoplayer2.upstream.DataSource$Factory);
}
-dontnote com.google.android.exoplayer2.source.hls.HlsMediaSource$Factory
-keepclasseswithmembers class com.google.android.exoplayer2.source.hls.HlsMediaSource$Factory {
  <init>(com.google.android.exoplayer2.upstream.DataSource$Factory);
}
-dontnote com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource$Factory
-keepclasseswithmembers class com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource$Factory {
  <init>(com.google.android.exoplayer2.upstream.DataSource$Factory);
}

-keep class com.microsoft.appcenter.AppCenter {
    private com.microsoft.appcenter.channel.Channel mChannel;
    private android.os.Handler mHandler;
}

-keep class org.telegram.messenger.voip.* { *; }
-keep class org.telegram.messenger.AnimatedFileDrawableStream { <methods>; }
-keep class org.telegram.SQLite.SQLiteException { <methods>; }
-keep class org.telegram.tgnet.ConnectionsManager { <methods>; }
-keep class org.telegram.tgnet.NativeByteBuffer { <methods>; }
-keepclassmembernames class org.telegram.ui.* { <fields>; }
-keepclassmembernames class org.telegram.ui.Cells.* { <fields>; }
-keepclassmembernames class org.telegram.ui.Components.* { <fields>; }

-keepclassmembernames class tw.nekomimi.nekogram.settings.NekoGeneralSettingsActivity {
    private org.telegram.ui.Components.RecyclerListView listView;
    private int translatorTypeRow;
}

-keepclassmembernames class androidx.core.widget.NestedScrollView {
    private android.widget.OverScroller mScroller;
    private void abortAnimatedScroll();
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
}

-dontwarn org.jetbrains.annotations.NotNull
-dontwarn org.jetbrains.annotations.Nullable

-repackageclasses
-allowaccessmodification
-overloadaggressively
