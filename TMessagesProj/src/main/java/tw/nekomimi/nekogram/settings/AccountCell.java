package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;

public class AccountCell extends FrameLayout {

    private final TextView textView;
    private final BackupImageView imageView;
    private final ImageView checkImageView;
    private final AvatarDrawable avatarDrawable;
    private boolean needDivider;

    private int accountNumber;

    public AccountCell(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);

        avatarDrawable = new AvatarDrawable();
        avatarDrawable.setTextSize(AndroidUtilities.dp(12));

        imageView = new BackupImageView(context);
        imageView.setRoundRadius(AndroidUtilities.dp(18));
        addView(imageView, LayoutHelper.createFrame(36, 36, Gravity.LEFT | Gravity.TOP, 16, 10, 0, 0));

        textView = new TextView(context);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.LEFT | Gravity.TOP, 68, 0, 56, 0));

        checkImageView = new ImageView(context);
        checkImageView.setImageResource(R.drawable.account_check);
        checkImageView.setScaleType(ImageView.ScaleType.CENTER);
        checkImageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_switchTrackChecked, resourcesProvider), PorterDuff.Mode.MULTIPLY));
        addView(checkImageView, LayoutHelper.createFrame(40, LayoutHelper.MATCH_PARENT, Gravity.RIGHT | Gravity.TOP, 0, 0, 6, 0));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(68), getMeasuredHeight() - 1, getMeasuredWidth(), getMeasuredHeight() - 1, Theme.dividerPaint);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        imageView.setAlpha(enabled ? 1.0f : 0.5f);
        textView.setAlpha(enabled ? 1.0f : 0.5f);
        checkImageView.setAlpha(enabled ? 1.0f : 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(56) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public void setAccount(int account, boolean check, boolean divider) {
        accountNumber = account;
        TLRPC.User user = UserConfig.getInstance(accountNumber).getCurrentUser();
        avatarDrawable.setInfo(user);
        textView.setText(Emoji.replaceEmoji(ContactsController.formatName(user.first_name, user.last_name), textView.getPaint().getFontMetricsInt(), false));
        imageView.getImageReceiver().setCurrentAccount(account);
        imageView.setForUserOrChat(user, avatarDrawable);
        checkImageView.setVisibility(check ? VISIBLE : INVISIBLE);
        needDivider = divider;
        setWillNotDraw(!divider);
    }

    public int getAccountNumber() {
        return accountNumber;
    }
}