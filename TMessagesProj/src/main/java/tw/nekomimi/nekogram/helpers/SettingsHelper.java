package tw.nekomimi.nekogram.helpers;

import android.net.Uri;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.ui.ActionBar.BaseFragment;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoAppearanceSettings;
import tw.nekomimi.nekogram.settings.NekoChatSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoDonateActivity;
import tw.nekomimi.nekogram.settings.NekoExperimentalSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoGeneralSettingsActivity;
import tw.nekomimi.nekogram.settings.NekoSettingsActivity;
import tw.nekomimi.nekogram.settings.WsSettingsActivity;

public class SettingsHelper {
    public static void processDeepLink(String link, Callback callback, Runnable unknown) {
        Uri uri = null;
        try {
            uri = Uri.parse(link);
        } catch (Exception e) {
            FileLog.e(e);
        }
        processDeepLink(uri, callback, unknown, false);
    }

    public static void processDeepLink(Uri uri, Callback callback, Runnable unknown, boolean me) {
        if (uri == null) {
            unknown.run();
            return;
        }
        var segments = uri.getPathSegments();
        if (me) {
            if (segments.isEmpty() || segments.size() > 2 || !"nekosettings".equals(segments.get(0))) {
                unknown.run();
                return;
            }
        } else if (segments.size() > 1 || !"neko".equals(uri.getHost())) {
            unknown.run();
            return;
        }
        BaseNekoSettingsActivity fragment;
        if (segments.isEmpty() || me && segments.size() == 1) {
            fragment = new NekoSettingsActivity();
        } else {
            switch (segments.get(me ? 1 : 0)) {
                case "appearance":
                case "a":
                    fragment = new NekoAppearanceSettings();
                    break;
                case "chat":
                case "chats":
                case "c":
                    fragment = new NekoChatSettingsActivity();
                    break;
                case "donate":
                case "d":
                    fragment = new NekoDonateActivity();
                    break;
                case "experimental":
                case "e":
                    fragment = new NekoExperimentalSettingsActivity(false, false);
                    break;
                case "general":
                case "g":
                    fragment = new NekoGeneralSettingsActivity();
                    break;
                case "ws":
                case "w":
                    fragment = new WsSettingsActivity();
                    break;
                default:
                    unknown.run();
                    return;
            }
        }
        callback.presentFragment(fragment);
        var row = uri.getQueryParameter("r");
        if (TextUtils.isEmpty(row)) {
            row = uri.getQueryParameter("row");
        }
        if (!TextUtils.isEmpty(row)) {
            var rowFinal = row;
            AndroidUtilities.runOnUIThread(() -> fragment.scrollToRow(rowFinal, unknown));
        }

    }

    public interface Callback {
        void presentFragment(BaseFragment fragment);
    }
}
