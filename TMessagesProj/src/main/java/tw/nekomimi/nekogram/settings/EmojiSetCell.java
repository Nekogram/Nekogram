package tw.nekomimi.nekogram.settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
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
import org.telegram.messenger.Emoji;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.EmojiHelper;

@SuppressLint("ViewConstructor")
public class EmojiSetCell extends FrameLayout {
    private final TextView textView;
    private final AnimatedTextView valueTextView;
    private final BackupImageView imageView;

    private ImageView optionsButton;
    private CheckBox2 checkBox;

    private EmojiHelper.EmojiPack pack;
    private boolean needDivider;
    private final boolean selection;

    public EmojiSetCell(Context context, boolean selection, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.selection = selection;

        imageView = new BackupImageView(context);
        imageView.setAspectFit(true);
        imageView.setLayerNum(1);
        imageView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(8));
        boolean rtl = selection == LocaleController.isRTL;
        addView(imageView, LayoutHelper.createFrame(40, 40, (rtl ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, rtl ? 0 : !selection ? 21 : 13, 9, rtl ? !selection ? 21 : 13 : 0, 0));

        if (selection) {
            optionsButton = new ImageView(context);
            optionsButton.setFocusable(false);
            optionsButton.setScaleType(ImageView.ScaleType.CENTER);
            optionsButton.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_featuredStickers_addedIcon, resourcesProvider), PorterDuff.Mode.MULTIPLY));
            optionsButton.setImageResource(R.drawable.floating_check);
            optionsButton.setVisibility(GONE);
            addView(optionsButton, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, (LocaleController.isRTL ? 10 : 0), 9, (LocaleController.isRTL ? 0 : 10), 0));
        }

        textView = new TextView(context) {
            @Override
            public void setText(CharSequence text, BufferType type) {
                text = Emoji.replaceEmoji(text, getPaint().getFontMetricsInt(), false);
                super.setText(text, type);
            }
        };
        NotificationCenter.listenEmojiLoading(textView);
        textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        if (selection) {
            textView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        }
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setGravity(LayoutHelper.getAbsoluteGravityStart());
        addView(textView, LayoutHelper.createFrameRelatively(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START, !selection ? 21 : 71, 9, 70, 0));

        valueTextView = new AnimatedTextView(context);
        valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        valueTextView.setAnimationProperties(.55f, 0, 320, CubicBezierInterpolator.EASE_OUT_QUINT);
        valueTextView.setTextSize(AndroidUtilities.dp(13));
        valueTextView.setGravity(LayoutHelper.getAbsoluteGravityStart());
        addView(valueTextView, LayoutHelper.createFrameRelatively(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.START, !selection ? 21 : 71, 25, 70, 0));

        if (selection) {
            checkBox = new CheckBox2(context, 21);
            checkBox.setColor(-1, Theme.key_windowBackgroundWhite, Theme.key_checkboxCheck);
            checkBox.setDrawUnchecked(false);
            checkBox.setDrawBackgroundAsArc(3);
            addView(checkBox, LayoutHelper.createFrameRelatively(24, 24, Gravity.START, 34, 30, 0, 0));
        }
    }

    public void setData(EmojiHelper.EmojiPack emojiPackInfo, boolean animated, boolean divider) {
        needDivider = divider;
        if (selection) {
            textView.setText(emojiPackInfo.getPackName());
            pack = emojiPackInfo;
            if ("default".equals(pack.getPackId())) {
                valueTextView.setText(LocaleController.getString(R.string.Default), animated);
            } else {
                valueTextView.setText(LocaleController.getString(R.string.InstalledEmojiSet), animated);
            }
            setPackPreview(pack);
        } else {
            textView.setText(LocaleController.getString(R.string.EmojiSets));
            if (NekoConfig.useSystemEmoji) {
                valueTextView.setText(EmojiHelper.getInstance().getSelectedPackName(), animated);
                imageView.setImageBitmap(EmojiHelper.getInstance().getSystemEmojiPreview());
            } else if (emojiPackInfo == null) {
                valueTextView.setText(EmojiHelper.getInstance().getSelectedPackName(), animated);
                imageView.setImageBitmap(null);
            } else {
                valueTextView.setText(emojiPackInfo.getPackName(), animated);
                setPackPreview(emojiPackInfo);
            }
        }
    }

    private void setPackPreview(EmojiHelper.EmojiPack pack) {
        if ("default".equals(pack.getPackId())) {
            imageView.setImageDrawable(getContext().getDrawable(R.drawable.apple));
        } else {
            imageView.setImage(pack.getPreview(), null, null);
        }
    }

    public void setChecked(boolean checked, boolean animated) {
        if (animated) {
            optionsButton.animate().cancel();
            optionsButton.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!checked) {
                        optionsButton.setVisibility(INVISIBLE);
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    if (checked) {
                        optionsButton.setVisibility(VISIBLE);
                    }
                }
            }).alpha(checked ? 1 : 0).scaleX(checked ? 1 : 0.1f).scaleY(checked ? 1 : 0.1f).setDuration(150).start();
        } else {
            optionsButton.setVisibility(checked ? VISIBLE : INVISIBLE);
            optionsButton.setScaleX(checked ? 1f : 0.1f);
            optionsButton.setScaleY(checked ? 1f : 0.1f);
            optionsButton.setAlpha(checked ? 1f : 0f);
        }
    }

    public void setSelected(boolean selected, boolean animated) {
        checkBox.setChecked(selected, animated);
    }

    public boolean isChecked() {
        return optionsButton.getVisibility() == VISIBLE;
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (needDivider) {
            canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(!selection ? 21 : 71), getHeight() - 1, getWidth() - getPaddingRight() - (LocaleController.isRTL ? AndroidUtilities.dp(!selection ? 21 : 71) : 0), getHeight() - 1, Theme.dividerPaint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(58) + (needDivider ? 1 : 0), MeasureSpec.EXACTLY));
    }

    public EmojiHelper.EmojiPack getPack() {
        return pack;
    }
}