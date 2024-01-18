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
import android.text.TextUtils;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.CustomTabsCopyReceiver;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
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
import java.util.regex.Matcher;

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
        return url.matches("^(https" + (forceHttps ? "" : "?") + "://)?(te\\.?legra\\.ph|graph\\.org)(/.*|$)"); // telegra.ph, te.legra.ph, graph.org
    }

    public static String extractUsername(String link) {
        if (link == null || TextUtils.isEmpty(link)) {
            return null;
        }
        if (link.startsWith("@")) {
            return link.substring(1);
        }
        if (link.startsWith("t.me/")) {
            return link.substring(5);
        }
        if (link.startsWith("http://t.me/")) {
            return link.substring(12);
        }
        if (link.startsWith("https://t.me/")) {
            return link.substring(13);
        }
        Matcher prefixMatcher = LaunchActivity.PREFIX_T_ME_PATTERN.matcher(link);
        if (prefixMatcher.find()) {
            return prefixMatcher.group(1);
        }
        return null;
    }

    public static boolean urlMustNotHaveConfirmation(String url) {
        return (
            isTelegraphUrl(url, false, true) ||
            url.matches("^(https://)?t\\.me/iv\\??(/.*|$)") || // t.me/iv?
            url.matches("^(https://)?telegram\\.org/(blog|tour)(/.*|$)") || // telegram.org/blog, telegram.org/tour
            url.matches("^(https://)?fragment\\.com(/.*|$)") // fragment.com
        );
    }

    public static class Progress {
        public void init() {}
        public void end() {
            end(false);
        }
        public void end(boolean replaced) {}

        private Runnable onCancelListener;
        public void cancel() {
            cancel(false);
        }
        public void cancel(boolean replaced) {
            if (onCancelListener != null) {
                onCancelListener.run();
            }
            end(replaced);
        }
        public void onCancel(Runnable onCancelListener) {
            this.onCancelListener = onCancelListener;
        }
    }

    public static void openUrl(final Context context, Uri uri, final boolean allowCustom, boolean tryTelegraph) {
        openUrl(context, uri, allowCustom, tryTelegraph, false, null);
    }

    public static void openUrl(final Context context, Uri uri, final boolean allowCustom, boolean tryTelegraph, Progress inCaseLoading) {
        openUrl(context, uri, allowCustom, tryTelegraph, false, inCaseLoading);
    }

    public static void openUrl(final Context context, Uri uri, final boolean allowCustom, boolean tryTelegraph, boolean forceNotInternalForApps, Progress inCaseLoading) {
        if (context == null || uri == null) {
            return;
        }
        final int currentAccount = UserConfig.selectedAccount;
        boolean[] forceBrowser = new boolean[]{false};
        boolean internalUri = isInternalUri(uri, forceBrowser);
        if (tryTelegraph) {
            try {
                String host = AndroidUtilities.getHostAuthority(uri);
                if (NekoConfig.tryToOpenAllLinksInIV || isTelegraphUrl(host, true) || "telegram.org".equalsIgnoreCase(host) && (uri.toString().toLowerCase().contains("telegram.org/faq") || uri.toString().toLowerCase().contains("telegram.org/privacy") || uri.toString().toLowerCase().contains("telegram.org/blog"))) {
                    final AlertDialog[] progressDialog = new AlertDialog[] {
                        new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER)
                    };

                    Uri finalUri = uri;
                    TLRPC.TL_messages_getWebPagePreview req = new TLRPC.TL_messages_getWebPagePreview();
                    req.message = uri.toString();
                    final int reqId = ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                        if (inCaseLoading != null) {
                            inCaseLoading.end();
                        } else {
                            try {
                                progressDialog[0].dismiss();
                            } catch (Throwable ignore) {}
                            progressDialog[0] = null;
                        }

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
                    if (inCaseLoading != null) {
                        inCaseLoading.init();
                    } else {
                        AndroidUtilities.runOnUIThread(() -> {
                            if (progressDialog[0] == null) {
                                return;
                            }
                            try {
                                progressDialog[0].setOnCancelListener(dialog -> ConnectionsManager.getInstance(UserConfig.selectedAccount).cancelRequest(reqId, true));
                                progressDialog[0].show();
                            } catch (Exception ignore) {}
                        }, 1000);
                    }
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
            String host = AndroidUtilities.getHostAuthority(uri.toString().toLowerCase());
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
                if (MessagesController.getInstance(currentAccount).authDomains.contains(host)) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ApplicationLoader.applicationContext.startActivity(intent);
                    return;
                }

                PendingIntent copy = PendingIntent.getBroadcast(ApplicationLoader.applicationContext, 0, new Intent(ApplicationLoader.applicationContext, CustomTabsCopyReceiver.class), PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

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
            if (internalUri && context instanceof LaunchActivity) {
                intent.putExtra(LaunchActivity.EXTRA_FORCE_NOT_INTERNAL_APPS, forceNotInternalForApps);
                ((LaunchActivity) context).onNewIntent(intent, inCaseLoading);
            } else {
                context.startActivity(intent);
            }
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

    public static boolean isTMe(String url) {
        try {
            final String linkPrefix = MessagesController.getInstance(UserConfig.selectedAccount).linkPrefix;
            return TextUtils.equals(AndroidUtilities.getHostAuthority(url), linkPrefix);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static boolean isInternalUri(Uri uri, boolean[] forceBrowser) {
        return isInternalUri(uri, false, forceBrowser);
    }

    public static boolean isInternalUri(Uri uri, boolean all, boolean[] forceBrowser) {
        String host = AndroidUtilities.getHostAuthority(uri);
        host = host != null ? host.toLowerCase() : "";

        if (MessagesController.getInstance(UserConfig.selectedAccount).authDomains.contains(host)) {
            if (forceBrowser != null) {
                forceBrowser[0] = true;
            }
            return false;
        }

        Matcher prefixMatcher = LaunchActivity.PREFIX_T_ME_PATTERN.matcher(host);
        if (prefixMatcher.find()) {
            uri = Uri.parse("https://t.me/" + prefixMatcher.group(1) + (TextUtils.isEmpty(uri.getPath()) ? "" : "/" + uri.getPath()) + (TextUtils.isEmpty(uri.getQuery()) ? "" : "?" + uri.getQuery()));

            host = uri.getHost();
            host = host != null ? host.toLowerCase() : "";
        }

        if ("ton".equals(uri.getScheme())) {
            try {
                Intent viewIntent = new Intent(Intent.ACTION_VIEW, uri);
                List<ResolveInfo> allActivities = ApplicationLoader.applicationContext.getPackageManager().queryIntentActivities(viewIntent, 0);
                if (allActivities != null && allActivities.size() >= 1) {
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
        } else if ("telegram.org".equals(host) && uri != null && uri.getPath() != null && uri.getPath().startsWith("/blog/")) {
            return true;
        } else if (all) {
            if (host.endsWith("telegram.org") || host.endsWith("telegra.ph") || host.endsWith("telesco.pe")) {
                return true;
            }
        }
        return false;
    }

    // © ChatGPT. All puns reserved. 🤖📜
    public static String replaceHostname(Uri originalUri, String newHostname) {
        String scheme = originalUri.getScheme();
        String userInfo = originalUri.getUserInfo();
        int port = originalUri.getPort();
        String path = originalUri.getPath();
        String query = originalUri.getQuery();
        String fragment = originalUri.getFragment();

        StringBuilder modifiedUriBuilder = new StringBuilder();
        modifiedUriBuilder.append(scheme).append("://");
        if (userInfo != null) {
            modifiedUriBuilder.append(userInfo).append("@");
        }
        modifiedUriBuilder.append(newHostname);
        if (port != -1) {
            modifiedUriBuilder.append(":").append(port);
        }
        modifiedUriBuilder.append(path);
        if (query != null) {
            modifiedUriBuilder.append("?").append(query);
        }
        if (fragment != null) {
            modifiedUriBuilder.append("#").append(fragment);
        }
        return modifiedUriBuilder.toString();
    }
}
