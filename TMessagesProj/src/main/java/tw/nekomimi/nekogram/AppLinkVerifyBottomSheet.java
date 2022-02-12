package tw.nekomimi.nekogram;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.verify.domain.DomainVerificationManager;
import android.content.pm.verify.domain.DomainVerificationUserState;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.StickerImageView;

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
        title.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        title.setText(LocaleController.getString("AppLinkNotVerifiedTitle", R.string.AppLinkNotVerifiedTitle));
        linearLayout.addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 30, 21, 0));

        TextView description = new TextView(context);
        description.setGravity(Gravity.START);
        description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        description.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        description.setText(AndroidUtilities.replaceTags(LocaleController.getString("AppLinkNotVerifiedMessage", R.string.AppLinkNotVerifiedMessage)));
        linearLayout.addView(description, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 15, 21, 16));

        TextView buttonTextView = new TextView(context);
        buttonTextView.setPadding(AndroidUtilities.dp(34), 0, AndroidUtilities.dp(34), 0);
        buttonTextView.setGravity(Gravity.CENTER);
        buttonTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        buttonTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        buttonTextView.setText(LocaleController.getString("GoToSettings", R.string.GoToSettings));

        buttonTextView.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        buttonTextView.setBackground(Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(6), Theme.getColor(Theme.key_featuredStickers_addButton), ColorUtils.setAlphaComponent(Theme.getColor(Theme.key_windowBackgroundWhite), 120)));

        linearLayout.addView(buttonTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 15, 16, 8));

        buttonTextView.setOnClickListener(view -> {
            Intent intent = new Intent(android.provider.Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
                    Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        });

        TextView textView = new TextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        textView.setText(LocaleController.getString("DontAskAgain", R.string.DontAskAgain));
        textView.setTextColor(Theme.getColor(Theme.key_featuredStickers_addButton));

        linearLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, 0, 16, 0, 16, 0));

        textView.setOnClickListener(view -> {
            dismiss();
            NekoConfig.setVerifyLinkTip(true);
        });

        ScrollView scrollView = new ScrollView(context);
        scrollView.addView(frameLayout);
        setCustomView(scrollView);
    }
}
