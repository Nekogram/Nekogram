package tw.nekomimi.nekogram;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.ScaleStateListAnimator;
import org.telegram.ui.Components.StickerImageView;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.S)
public class AppLinkVerifyBottomSheet extends BottomSheet {

    @Override
    protected boolean canDismissWithSwipe() {
        return false;
    }

    @Override
    protected boolean canDismissWithTouchOutside() {
        return false;
    }

    public static void checkBottomSheet(BaseFragment fragment) {
        if (NekoConfig.verifyLinkTip) {
            return;
        }
        Context context = fragment.getParentActivity();
        DomainVerificationManager manager = context.getSystemService(DomainVerificationManager.class);
        DomainVerificationUserState userState = null;
        try {
            userState = manager.getDomainVerificationUserState(context.getPackageName());
        } catch (PackageManager.NameNotFoundException ignore) {

        }

        if (userState == null) {
            return;
        }

        boolean hasUnverified = false;
        Map<String, Integer> hostToStateMap = userState.getHostToStateMap();
        for (String key : hostToStateMap.keySet()) {
            Integer stateValue = hostToStateMap.get(key);
            if (stateValue == null || stateValue == DomainVerificationUserState.DOMAIN_STATE_VERIFIED || stateValue == DomainVerificationUserState.DOMAIN_STATE_SELECTED) {
                continue;
            }
            hasUnverified = true;
            break;
        }
        if (hasUnverified) {
            fragment.showDialog(new AppLinkVerifyBottomSheet(fragment));
        }
    }

    public AppLinkVerifyBottomSheet(BaseFragment fragment) {
        super(fragment.getParentActivity(), false);
        setCanceledOnTouchOutside(false);
        Context context = fragment.getParentActivity();

        FrameLayout frameLayout = new FrameLayout(context);

        ImageView closeView = new ImageView(context);
        closeView.setBackground(Theme.createSelectorDrawable(getThemedColor(Theme.key_listSelector)));
        closeView.setColorFilter(Theme.getColor(Theme.key_sheet_other));
        closeView.setImageResource(R.drawable.ic_layer_close);
        closeView.setOnClickListener((view) -> dismiss());
        int closeViewPadding = AndroidUtilities.dp(8);
        closeView.setPadding(closeViewPadding, closeViewPadding, closeViewPadding, closeViewPadding);
        ScaleStateListAnimator.apply(closeView);
        frameLayout.addView(closeView, LayoutHelper.createFrame(36, 36, Gravity.TOP | Gravity.END, 6, 8, 6, 0));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        frameLayout.addView(linearLayout);

        StickerImageView imageView = new StickerImageView(context, currentAccount);
        imageView.setStickerNum(3);
        imageView.getImageReceiver().setAutoRepeat(1);
        linearLayout.addView(imageView, LayoutHelper.createLinear(144, 144, Gravity.CENTER_HORIZONTAL, 0, 16, 0, 0));

        TextView title = new TextView(context);
        title.setGravity(Gravity.START);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        title.setText(LocaleController.getString("AppLinkNotVerifiedTitle", R.string.AppLinkNotVerifiedTitle));
        linearLayout.addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 30, 21, 0));

        TextView description = new TextView(context);
        description.setGravity(Gravity.START);
        description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        description.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        description.setText(AndroidUtilities.replaceTags(LocaleController.getString("AppLinkNotVerifiedMessage", R.string.AppLinkNotVerifiedMessage)));
        linearLayout.addView(description, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 15, 21, 16));

        ButtonWithCounterView buttonTextView = new ButtonWithCounterView(context, true, null);
        buttonTextView.setText(LocaleController.getString("GoToSettings", R.string.GoToSettings), false);
        linearLayout.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 14, 14, 14, 0));

        buttonTextView.setOnClickListener(view -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                        Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            } catch (Throwable t) {
                try {
                    Intent intent = new Intent("android.intent.action.MAIN", Uri.parse("package:" + context.getPackageName()));
                    intent.setClassName("com.android.settings", "com.android.settings.applications.InstalledAppOpenByDefaultActivity");
                    context.startActivity(intent);
                } catch (Throwable t2) {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + context.getPackageName()));
                    context.startActivity(intent);
                }
            }
        });

        ButtonWithCounterView textView = new ButtonWithCounterView(context, false, null);
        textView.setText(LocaleController.getString("DontAskAgain", R.string.DontAskAgain), false);
        linearLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 14, 14, 14, 0));

        textView.setOnClickListener(view -> {
            dismiss();
            NekoConfig.setVerifyLinkTip(true);
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(frameLayout);
        setCustomView(scrollView);
    }
}
