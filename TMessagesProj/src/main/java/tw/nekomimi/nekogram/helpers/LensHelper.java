package tw.nekomimi.nekogram.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class LensHelper {

    private static final String QUICK_SEARCH_BOX = "com.google.android.googlequicksearchbox";
    private static final String LENS_UPLOAD = "https://lens.google.com/upload";
    private static Call.Factory okHttpClient;

    private static boolean hasLens = false;
    private static CharSequence lensName;
    private static String lensClass;

    public static boolean hasLens() {
        return hasLens;
    }

    public static CharSequence getLensName() {
        return lensName;
    }

    public static CharSequence getMenuLabel() {
        if (hasLens && lensName != null) {
            return lensName;
        }
        return LocaleController.getString(R.string.LensSearchImage);
    }

    public static void checkLensSupportAsync() {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                checkLensSupport();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private static void checkLensSupport() {
        var pm = ApplicationLoader.applicationContext.getPackageManager();
        var intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(Uri.parse("content://" + ApplicationLoader.getApplicationId() + ".provider"), "image/jpeg");
        intent.setPackage(QUICK_SEARCH_BOX);
        var activities = pm.queryIntentActivities(intent, 0);
        for (var info : activities) {
            if (info.activityInfo.name.contains("Lens")) {
                hasLens = true;
                lensClass = info.activityInfo.name;
                lensName = info.loadLabel(pm);
                break;
            }
        }
    }

    public static void launchLens(Activity activity, Uri uri) {
        var intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setClassName(QUICK_SEARCH_BOX, lensClass);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            FileLog.e(e);
        }
    }

    private static Call.Factory getOkHttpClient() {
        if (okHttpClient == null) {
            var builder = new OkHttpClient.Builder();
            builder.connectTimeout(30, TimeUnit.SECONDS);
            builder.readTimeout(30, TimeUnit.SECONDS);
            builder.writeTimeout(30, TimeUnit.SECONDS);
            builder.followRedirects(false);
            builder.followSslRedirects(false);
            okHttpClient = builder.build();
        }
        return okHttpClient;
    }

    public static void searchOnWeb(Activity activity, File file) {
        if (activity == null || file == null || !file.exists()) {
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            try {
                android.graphics.BitmapFactory.Options opts = new android.graphics.BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
                int imgW = opts.outWidth > 0 ? opts.outWidth : 1000;
                int imgH = opts.outHeight > 0 ? opts.outHeight : 1000;
                String url = LENS_UPLOAD + "?re=mf&ep=wil&ctx=wa1&processed_image_dimensions=" + imgW + "," + imgH;
                String mime = java.net.URLConnection.guessContentTypeFromName(file.getName());
                if (mime == null) {
                    mime = "application/octet-stream";
                }
                var body = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("encoded_image", file.getName(), RequestBody.create(file, MediaType.get(mime)))
                        .build();
                var request = new Request.Builder()
                        .url(url)
                        .header("accept", "*/*")
                        .header("accept-language", "en-US,en;q=0.9")
                        .header("user-agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36")
                        .post(body)
                        .build();
                String location = null;
                List<String> cookies = new ArrayList<>();
                try (var response = getOkHttpClient().newCall(request).execute()) {
                    FileLog.d("Lens upload code " + response.code());
                    if (response.code() == 301 || response.code() == 302 || response.code() == 303 || response.code() == 307 || response.code() == 308) {
                        location = response.header("Location");
                        for (String h : response.headers("Set-Cookie")) {
                            int i = h.indexOf(';');
                            String pair = (i >= 0 ? h.substring(0, i) : h).trim();
                            if (pair.contains("=")) {
                                cookies.add(pair);
                            }
                        }
                    } else {
                        FileLog.e("Lens upload unexpected code " + response.code());
                    }
                }
                if (location != null && !location.isEmpty()) {
                    if (location.contains("consent.")) {
                        FileLog.e("Lens upload got consent URL, opening externally");
                        String consentUrl = location;
                        AndroidUtilities.runOnUIThread(() -> {
                            try {
                                Browser.openUrl(activity, Uri.parse(consentUrl), true);
                            } catch (Exception e2) {
                                FileLog.e(e2);
                            }
                        });
                    } else {
                        String resultUrl = location;
                        ArrayList<String> resultCookies = new ArrayList<>(cookies);
                        FileLog.d("Lens upload got redirect, cookies=" + resultCookies.size());
                        AndroidUtilities.runOnUIThread(() -> showLensSheet(activity, resultUrl, resultCookies));
                    }
                } else {
                    FileLog.e("Lens upload got no redirect");
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static void showLensSheet(Activity activity, String resultUrl, List<String> cookies) {
        try {
            CookieManager cm = CookieManager.getInstance();
            cm.setAcceptCookie(true);
            for (String c : cookies) {
                cm.setCookie("https://www.google.com", c);
                cm.setCookie("https://lens.google.com", c);
            }
            cm.flush();
            WebView webView = new WebView(activity);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setDomStorageEnabled(true);
            webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36");
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String newUrl) {
                    return false;
                }
            });
            int sheetH = AndroidUtilities.displaySize.y > 0 ? (int) (AndroidUtilities.displaySize.y * 0.85) : AndroidUtilities.dp(500);
            FrameLayout box = new FrameLayout(activity);
            box.addView(webView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, sheetH));
            webView.loadUrl(resultUrl);
            BottomSheet sheet = new BottomSheet.Builder(activity).setCustomView(box).create();
            sheet.setOnDismissListener(dialog -> {
                try {
                    webView.stopLoading();
                    webView.loadUrl("about:blank");
                    webView.destroy();
                } catch (Exception e) {
                    FileLog.e(e);
                }
                try {
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
            sheet.show();
        } catch (Exception e) {
            FileLog.e(e);
            try {
                Browser.openUrl(activity, Uri.parse(resultUrl), true);
            } catch (Exception e2) {
                FileLog.e(e2);
            }
        }
    }

}
