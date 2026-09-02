package tw.nekomimi.nekogram.helpers;

import android.net.Uri;

import java.util.List;

public class WebpageHelper {
    private static final List<String> TWITTER_FIXES = List.of(
            "fxtwitter.com",
            "fixupx.com",
            "twittpr.com",
            "vxtwitter.com",
            "fixvx.com"
    );

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
