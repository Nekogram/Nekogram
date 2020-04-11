package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import tw.nekomimi.nekogram.NekoConfig;

public class TranslateBottomSheet extends BottomSheet {

    @SuppressLint("StaticFieldLeak")
    private static TranslateBottomSheet instance;
    private WebView webView;
    private RadialProgressView progressBar;
    private Activity parentActivity;
    private FrameLayout containerLayout;
    private String url;

    @SuppressLint("SetJavaScriptEnabled")
    protected TranslateBottomSheet(Context context, final String text) {
        super(context, false);
        setApplyTopPadding(false);
        setApplyBottomPadding(false);
        setCanDismissWithSwipe(false);

        if (context instanceof Activity) {
            parentActivity = (Activity) context;
        }

        try {
            switch (NekoConfig.translationProvider) {
                case -1:
                    url = String.format("https://translate.google.com/?view=home&op=translate&text=%s", URLEncoder.encode(text, "UTF-8"));
                    break;
                case -2:
                    url = String.format("https://translate.google.cn/?view=home&op=translate&text=%s", URLEncoder.encode(text, "UTF-8"));
                    break;
                case -3:
                    url = String.format("https://fanyi.baidu.com/?aldtype=38319&tpltype=sigma#auto/zh/%s", Utils.encodeURIComponent(text));
                    break;
                case -4:
                    url = String.format("https://www.deepl.com/translator#auto/auto/%s", Utils.encodeURIComponent(text));
                    break;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        containerLayout = new FrameLayout(context) {
            @Override
            protected void onDetachedFromWindow() {
                super.onDetachedFromWindow();
                try {
                    if ((webView.getVisibility() != VISIBLE) && webView.getParent() != null) {
                        removeView(webView);
                        webView.stopLoading();
                        webView.loadUrl("about:blank");
                        webView.destroy();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int h = (int) (AndroidUtilities.displaySize.y / 1.5);
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(h + AndroidUtilities.dp(48) + 1, MeasureSpec.EXACTLY));
            }
        };
        containerLayout.setOnTouchListener((v, event) -> true);
        setCustomView(containerLayout);

        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        if (Build.VERSION.SDK_INT >= 17) {
            webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
        }

        if (Build.VERSION.SDK_INT >= 21) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.INVISIBLE);

            }
        });

        containerLayout.addView(webView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 48));

        progressBar = new RadialProgressView(context);
        progressBar.setVisibility(View.INVISIBLE);
        containerLayout.addView(progressBar, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, (48) / 2));

        View lineView = new View(context);
        lineView.setBackgroundColor(Theme.getColor(Theme.key_dialogGrayLine));
        containerLayout.addView(lineView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1, Gravity.LEFT | Gravity.BOTTOM));
        ((FrameLayout.LayoutParams) lineView.getLayoutParams()).bottomMargin = AndroidUtilities.dp(48);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_dialogBackground));
        containerLayout.addView(frameLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setWeightSum(1);
        frameLayout.addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.RIGHT));

        TextView textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue4));
        textView.setGravity(Gravity.CENTER);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), 0));
        textView.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);
        textView.setText(LocaleController.getString("Close", R.string.Close).toUpperCase());
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        frameLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        textView.setOnClickListener(v -> dismiss());

        TextView openInButton = new TextView(context);
        openInButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        openInButton.setTextColor(Theme.getColor(Theme.key_dialogTextBlue4));
        openInButton.setGravity(Gravity.CENTER);
        openInButton.setSingleLine(true);
        openInButton.setEllipsize(TextUtils.TruncateAt.END);
        openInButton.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.getColor(Theme.key_dialogButtonSelector), 0));
        openInButton.setPadding(AndroidUtilities.dp(18), 0, AndroidUtilities.dp(18), 0);
        openInButton.setText(LocaleController.getString("OpenInBrowser", R.string.OpenInBrowser).toUpperCase());
        openInButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        linearLayout.addView(openInButton, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        openInButton.setOnClickListener(v -> {
            Browser.openUrl(parentActivity, url);
            dismiss();
        });

        setDelegate(new BottomSheet.BottomSheetDelegate() {
            @Override
            public void onOpenAnimationEnd() {
                progressBar.setVisibility(View.VISIBLE);
                webView.setVisibility(View.VISIBLE);
                try {
                    webView.loadUrl(url);
                } catch (Exception e) {
                    FileLog.e(e);
                }

            }

        });

        instance = this;
    }

    public static void show(Context context, final String text) {
        if (instance != null) {
            instance.destroy();
        }
        new TranslateBottomSheet(context, text).show();
    }

    public static TranslateBottomSheet getInstance() {
        return instance;
    }

    public void destroy() {
        if (webView != null && webView.getVisibility() == View.VISIBLE) {
            containerLayout.removeView(webView);
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
        }
        instance = null;
        dismissInternal();
    }

}
