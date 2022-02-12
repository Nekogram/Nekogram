/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger.browser;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.CustomTabsCopyReceiver;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.LaunchActivity;

import java.net.URLEncoder;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;

public class Browser {

    public static void bindCustomTabsService(Activity activity) {
    }

    public static void unbindCustomTabsService(Activity activity) {
    }

    public static void openUrl(Context context, String url) {
        if (url == null) {
            return;
        }
        openUrl(context, Uri.parse(url), true);
    }

    public static void openUrl(Context context, Uri uri) {
        openUrl(context, uri, true);
    }

    public static void openUrl(Context context, String url, boolean allowCustom) {
        if (context == null || url == null) {
            return;
        }
        openUrl(context, Uri.parse(url), allowCustom);
    }

    public static void openUrl(Context context, Uri uri, boolean allowCustom) {
        openUrl(context, uri, allowCustom, true);
    }

    public static void openUrl(final Context context, final String url, final boolean allowCustom, boolean tryTelegraph) {
        openUrl(context, Uri.parse(url), allowCustom, tryTelegraph);
    }

    public static boolean isTelegraphUrl(String url, boolean equals) {
        return isTelegraphUrl(url, equals, false);
    }
    public static boolean isTelegraphUrl(String url, boolean equals, boolean forceHttps) {
        if (equals) {
            return url.equals("telegra.ph") || url.equals("te.legra.ph") || url.equals("graph.org");
        }
        return url.matches("^(https" + (forceHttps ? "" : "?") + "://)?(te\\.?legra\\.ph|graph\\.org).*"); // telegra.ph, te.legra.ph, graph.org
    }

    public static boolean urlMustNotHaveConfirmation(String url) {
        return (
            isTelegraphUrl(url, false, true) ||
            url.matches("^(https://)?t\\.me/iv\\??.*") || // t.me/iv?
            url.matches("^(https://)?telegram\\.org/(blog|tour)/?.*") // telegram.org/blog, telegram.org/tour
        );
    }

    public static void openUrl(final Context context, Uri uri, final boolean allowCustom, boolean tryTelegraph) {
        if (context == null || uri == null) {
            return;
        }
        final int currentAccount = UserConfig.selectedAccount;
        boolean[] forceBrowser = new boolean[]{false};
        boolean internalUri = isInternalUri(uri, forceBrowser);
        if (tryTelegraph) {
            try {
                String host = uri.getHost().toLowerCase();
                if (NekoConfig.tryToOpenAllLinksInIV || isTelegraphUrl(host, true) || uri.toString().toLowerCase().contains("telegram.org/faq") || uri.toString().toLowerCase().contains("telegram.org/privacy")) {
                    final AlertDialog[] progressDialog = new AlertDialog[]{new AlertDialog(context, 3)};

                    Uri finalUri = uri;
                    TLRPC.TL_messages_getWebPagePreview req = new TLRPC.TL_messages_getWebPagePreview();
                    req.message = uri.toString();
                    final int reqId = ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        try {
                            progressDialog[0].dismiss();
                        } catch (Throwable ignore) {

                        }
                        progressDialog[0] = null;

                        boolean ok = false;
                        if (response instanceof TLRPC.TL_messageMediaWebPage) {
                            TLRPC.TL_messageMediaWebPage webPage = (TLRPC.TL_messageMediaWebPage) response;
                            if (webPage.webpage instanceof TLRPC.TL_webPage && webPage.webpage.cached_page != null) {
                                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.openArticle, webPage.webpage, finalUri.toString());
                                ok = true;
                            }
                        }
                        if (!ok) {
                            openUrl(context, finalUri, allowCustom, false);
                        }
                    }));
                    AndroidUtilities.runOnUIThread(() -> {
                        if (progressDialog[0] == null) {
                            return;
                        }
                        try {
                            progressDialog[0].setOnCancelListener(dialog -> ConnectionsManager.getInstance(UserConfig.selectedAccount).cancelRequest(reqId, true));
                            progressDialog[0].show();
                        } catch (Exception ignore) {

                        }
                    }, 1000);
                    return;
                }
            } catch (Exception ignore) {

            }
        }
        try {
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "";
            if ("http".equals(scheme) || "https".equals(scheme)) {
                try {
                    uri = uri.normalizeScheme();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            if (AccountInstance.getInstance(currentAccount).getMessagesController().autologinDomains.contains(host)) {
                String token = "autologin_token=" + URLEncoder.encode(AccountInstance.getInstance(UserConfig.selectedAccount).getMessagesController().autologinToken, "UTF-8");
                String url = uri.toString();
                int idx = url.indexOf("://");
                String path = idx >= 0 ? url.substring(idx + 3) : url;
                String fragment = uri.getEncodedFragment();
                String finalPath = fragment == null ? path : path.substring(0, path.indexOf("#" + fragment));
                if (finalPath.indexOf('?') >= 0) {
                    finalPath += "&" + token;
                } else {
                    finalPath += "?" + token;
                }
                if (fragment != null) {
                    finalPath += "#" + fragment;
                }
                uri = Uri.parse("https://" + finalPath);
            }
            if (allowCustom && SharedConfig.customTabs && !internalUri && !scheme.equals("tel")) {
                PendingIntent copy = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, new Intent(ApplicationLoader.applicationContext, CustomTabsCopyReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);

                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.addMenuItem(LocaleController.getString("CopyLink", R.string.CopyLink), copy);

                builder.setColorScheme(Theme.getActiveTheme().isDark() ? CustomTabsIntent.COLOR_SCHEME_DARK : CustomTabsIntent.COLOR_SCHEME_LIGHT);
                CustomTabColorSchemeParams params = new CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(Theme.getColor(Theme.key_actionBarBrowser))
                        .build();
                builder.setDefaultColorSchemeParams(params);
                builder.setShowTitle(true);
                builder.setShareState(CustomTabsIntent.SHARE_STATE_ON);
                CustomTabsIntent intent = builder.build();
                intent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.launchUrl(context, uri);
                return;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (internalUri) {
                ComponentName componentName = new ComponentName(context.getPackageName(), LaunchActivity.class.getName());
                intent.setComponent(componentName);
            }
            intent.putExtra(android.provider.Browser.EXTRA_CREATE_NEW_TAB, true);
            intent.putExtra(android.provider.Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            intent.putExtra("internal", true);
            context.startActivity(intent);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean isInternalUrl(String url, boolean[] forceBrowser) {
        return isInternalUri(Uri.parse(url), false, forceBrowser);
    }

    public static boolean isInternalUrl(String url, boolean all, boolean[] forceBrowser) {
        return isInternalUri(Uri.parse(url), all, forceBrowser);
    }

    public static boolean isPassportUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            url = url.toLowerCase();
            if (url.startsWith("tg:passport") || url.startsWith("tg://passport") || url.startsWith("tg:secureid") || url.contains("resolve") && url.contains("domain=telegrampassport")) {
                return true;
            }
        } catch (Throwable ignore) {

        }
        return false;
    }

    public static boolean isInternalUri(Uri uri, boolean[] forceBrowser) {
        return isInternalUri(uri, false, forceBrowser);
    }

    public static boolean isInternalUri(Uri uri, boolean all, boolean[] forceBrowser) {
        String host = uri.getHost();
        host = host != null ? host.toLowerCase() : "";
        if ("ton".equals(uri.getScheme())) {
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, uri);
                List<ResolveInfo> allActivities = ApplicationLoader.applicationContext.getPackageManager().queryIntentActivities(viewIntent, 0);
                if (allActivities != null && allActivities.size() > 1) {
                    return false;
                }
            } catch (Exception ignore) {

            }
            return true;
        } else if ("tg".equals(uri.getScheme())) {
            return true;
        } else if ("telegram.dog".equals(host)) {
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                if (all) {
                    return true;
                }
                path = path.substring(1).toLowerCase();
                if (path.startsWith("blog") || path.equals("iv") || path.startsWith("faq") || path.equals("apps") || path.startsWith("s/")) {
                    if (forceBrowser != null) {
                        forceBrowser[0] = true;
                    }
                    return false;
                }
                return true;
            }
        } else if ("telegram.me".equals(host) || "t.me".equals(host)) {
            String path = uri.getPath();
            if (path != null && path.length() > 1) {
                if (all) {
                    return true;
                }
                path = path.substring(1).toLowerCase();
                if (path.equals("iv") || path.startsWith("s/")) {
                    if (forceBrowser != null) {
                        forceBrowser[0] = true;
                    }
                    return false;
                }
                return true;
            }
        } else if (all) {
            if (host.endsWith("telegram.org") || host.endsWith("telegra.ph") || host.endsWith("telesco.pe")) {
                return true;
            }
        }
        return false;
    }
}
