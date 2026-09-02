package tw.nekomimi.nekogram.translator.deepl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.ContextProgressView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.function.Consumer;

public class DeepLOAuthWebviewActivity extends BaseFragment {

    private WebView webView;
    private ContextProgressView progressView;

    private final String url;
    private final Consumer<String> callback;

    public DeepLOAuthWebviewActivity(String url, Consumer<String> callback) {
        super();
        this.url = url;
        this.callback = callback;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        AndroidUtilities.checkAndroidTheme(getContext(), false);
        webView.setLayerType(View.LAYER_TYPE_NONE, null);
        try {
            var parent = webView.getParent();
            if (parent != null) {
                ((FrameLayout) parent).removeView(webView);
            }
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.BotAuthLogin));

        var menu = actionBar.createMenu();
        var loadingItem = menu.addItemWithWidth(1, 0, AndroidUtilities.dp(56), LocaleController.getString(R.string.Loading));
        loadingItem.setEnabled(false);
        progressView = new ContextProgressView(context, 3);
        progressView.setAlpha(1.0f);
        progressView.setScaleX(1.0f);
        progressView.setScaleY(1.0f);
        progressView.setVisibility(View.VISIBLE);
        loadingItem.addView(progressView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        AndroidUtilities.checkAndroidTheme(context, true);
        webView = new WebView(context);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);

        fragmentView = new FrameLayout(context);
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        webView.setWebViewClient(new WebViewClient() {

            private boolean isRedirectUrl(String url) {
                if (DeepLOAuth.isRedirectUrl(url)) {
                    callback.accept(url);
                    finishFragment();
                    return true;
                }
                return false;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                if (isRedirectUrl(url)) {
                    return;
                }
                super.onLoadResource(view, url);
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return isRedirectUrl(url) || super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (progressView != null && progressView.getVisibility() == View.VISIBLE) {
                    var animatorSet = new AnimatorSet();
                    animatorSet.playTogether(
                            ObjectAnimator.ofFloat(progressView, View.SCALE_X, 1.0f, 0.1f),
                            ObjectAnimator.ofFloat(progressView, View.SCALE_Y, 1.0f, 0.1f),
                            ObjectAnimator.ofFloat(progressView, View.ALPHA, 1.0f, 0.0f));
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            progressView.setVisibility(View.GONE);
                        }
                    });
                    animatorSet.setDuration(150);
                    animatorSet.start();
                }
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                actionBar.setTitleAnimated(title, false, 220);
            }
        });
        frameLayout.addView(webView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        return fragmentView;
    }

    @Override
    public void onTransitionAnimationEnd(boolean isOpen, boolean backward) {
        if (isOpen && !backward && webView != null) {
            webView.loadUrl(url);
        }
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return false;
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (webView != null && webView.canGoBack()) {
            if (invoked) webView.goBack();
            return false;
        }
        return super.onBackPressed(invoked);
    }
}
