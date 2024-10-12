package tw.nekomimi.nekogram.helpers;

import android.net.Uri;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.util.List;

public class WebpageHelper {
    private static final List<String> TWITTER_FIXES = List.of(
            "fxtwitter.com",
            "fixupx.com",
            "twittpr.com",
            "vxtwitter.com",
            "fixvx.com"
    );

    public static String toFixUrl(String url) {
        try {
            var uri = Uri.parse(!url.startsWith("http") ? "https://" + url : url);
            if (uri == null) {
                return url;
            }
            var host = AndroidUtilities.getHostAuthority(uri.toString().toLowerCase());
            if (host == null) {
                return url;
            }
            String targetAuthority;
            if ("twitter.com".equals(host) || "x.com".equals(host)) {
                targetAuthority = "vxtwitter.com";
            } else if ("tiktok.com".equals(host) || host.endsWith(".tiktok.com")) {
                targetAuthority = host.replace("tiktok.com", "vxtiktok.com");
            } else if ("reddit.com".equals(host) || "www.reddit.com".equals(host)) {
                targetAuthority = "vxreddit.com";
            } else if ("instagram.com".equals(host) || "www.instagram.com".equals(host)) {
                targetAuthority = "ddinstagram.com";
            } else if ("pixiv.net".equals(host) || "www.pixiv.net".equals(host)) {
                targetAuthority = "phixiv.net";
            } else {
                return url;
            }
            return uri.buildUpon().authority(targetAuthority).build().toString();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return url;
    }

    public static Uri toNormalUrl(String host, Uri uri) {
        if (host == null) {
            return uri;
        }
        String targetAuthority;
        if (TWITTER_FIXES.stream().anyMatch(host::endsWith)) {
            targetAuthority = "twitter.com";
        } else if (host.endsWith("vxtiktok.com")) {
            targetAuthority = host.replace("vxtiktok.com", "tiktok.com");
        } else if (host.endsWith("vxreddit.com")) {
            targetAuthority = "www.reddit.com";
        } else if (host.endsWith("ddinstagram.com")) {
            targetAuthority = "www.instagram.com";
        } else if (host.endsWith("phixiv.net")) {
            targetAuthority = "www.pixiv.net";
        } else {
            return uri;
        }
        return uri.buildUpon().authority(targetAuthority).build();
    }
}
