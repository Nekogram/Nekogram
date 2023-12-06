package tw.nekomimi.nekogram.settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CheckBox2;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RadialProgressView;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.remote.EmojiHelper;

@SuppressLint("ViewConstructor")
public class EmojiSetCell extends FrameLayout {
    private final TextView textView;
    private final AnimatedTextView valueTextView;
    private final BackupImageView imageView;

    private ImageView optionsButton;
    private RadialProgressView radialProgress;
    private CheckBox2 checkBox;

    private EmojiHelper.EmojiPackBase pack;
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

            radialProgress = new RadialProgressView(context);
            radialProgress.setNoProgress(false);
            radialProgress.setProgressColor(Theme.getColor(Theme.key_featuredStickers_addedIcon, resourcesProvider));
            radialProgress.setStrokeWidth(2.8F);
            radialProgress.setSize(AndroidUtilities.dp(30));
            radialProgress.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(40), AndroidUtilities.dp(40)));
            radialProgress.setVisibility(INVISIBLE);
            addView(radialProgress, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT) | Gravity.TOP, (LocaleController.isRTL ? 10 : 0), 9, (LocaleController.isRTL ? 0 : 10), 0));
        }

        textView = new TextView(context) {
            @Override
            public void setText(CharSequence text, BufferType type) {
                text = Emoji.replaceEmoji(text, getPaint().getFontMetricsInt(), AndroidUtilities.dp(14), false);
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

    public void setData(EmojiHelper.EmojiPackBase emojiPackInfo, boolean animated, boolean divider) {
        needDivider = divider;
        if (selection) {
            textView.setText(emojiPackInfo.getPackName());
            pack = emojiPackInfo;
            int version;
            if (emojiPackInfo instanceof EmojiHelper.EmojiPackInfo) {
                version = ((EmojiHelper.EmojiPackInfo) emojiPackInfo).getPackVersion();
            } else {
                version = -1;
            }
            boolean installed;
            if (emojiPackInfo instanceof EmojiHelper.EmojiPackInfo) {
                installed = EmojiHelper.getInstance().isPackInstalled((EmojiHelper.EmojiPackInfo) pack);
            } else {
                installed = true;
            }
            if ("default".equals(pack.getPackId())) {
                valueTextView.setText(LocaleController.getString(R.string.Default), animated);
            } else if (installed || version == -1) {
                valueTextView.setText(LocaleController.getString(R.string.InstalledEmojiSet), animated);
            } else {
                String status;
                if (EmojiHelper.getInstance().isInstalledOldVersion(pack.getPackId(), version)) {
                    status = LocaleController.formatString(R.string.UpdateEmojiSet, AndroidUtilities.formatFileSize(pack.getFileSize()));
                } else {
                    status = LocaleController.formatString(R.string.DownloadEmojiSet, AndroidUtilities.formatFileSize(pack.getFileSize()));
                }
                valueTextView.setText(status, animated);
            }
            setPackPreview(pack);
            if (!animated) checkDownloaded(false);
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

    private void setPackPreview(EmojiHelper.EmojiPackBase pack) {
        if (pack instanceof EmojiHelper.EmojiPackInfo) {
            var document = ((EmojiHelper.EmojiPackInfo) pack).getPreviewDocument();
            var thumbs = document.thumbs;
            BitmapDrawable strippedThumb = null;
            for (int a = 0, N = thumbs.size(); a < N; a++) {
                TLRPC.PhotoSize photoSize = thumbs.get(a);
                if (photoSize instanceof TLRPC.TL_photoStrippedSize) {
                    strippedThumb = new BitmapDrawable(ApplicationLoader.applicationContext.getResources(), ImageLoader.getStrippedPhotoBitmap(photoSize.bytes, "b"));
                    break;
                }
            }
            if (strippedThumb != null) {
                imageView.setImage(ImageLocation.getForDocument(document), "146_146", strippedThumb, pack);
            } else {
                TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 146);
                imageView.setImage(ImageLocation.getForDocument(document), "146_146", ImageLocation.getForDocument(thumb, document), "146_146_B", thumb.size, pack);
            }
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

    public void setProgress(boolean visible, boolean animated) {
        if (animated) {
            radialProgress.animate().cancel();
            radialProgress.animate().setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (!visible) {
                        radialProgress.setVisibility(INVISIBLE);
                    }
                }

                @Override
                public void onAnimationStart(Animator animation) {
                    if (visible) {
                        radialProgress.setVisibility(VISIBLE);
                    }
                }
            }).alpha(visible ? 1 : 0).setDuration(150).start();
        } else {
            radialProgress.setVisibility(visible ? VISIBLE : INVISIBLE);
        }
    }

    public void setProgress(float percentage, long downBytes, boolean animated) {
        radialProgress.setProgress(percentage);
        valueTextView.setText(LocaleController.formatString(
                "AccDescrDownloadProgress",
                R.string.AccDescrDownloadProgress,
                AndroidUtilities.formatFileSize(downBytes),
                AndroidUtilities.formatFileSize(pack.getFileSize())
        ), animated);
    }

    public void checkDownloaded(boolean animated) {
        if ("default".equals(pack.getPackId())) return;
        if (pack instanceof EmojiHelper.EmojiPackInfo) {
            EmojiHelper.EmojiPackInfo packInfo = (EmojiHelper.EmojiPackInfo) pack;
            if (EmojiHelper.getInstance().isPackDownloaded(packInfo)) {
                setProgress(false, animated);
                if (EmojiHelper.getInstance().isPackInstalled(packInfo)) {
                    valueTextView.setText(LocaleController.getString(R.string.InstalledEmojiSet), animated);
                } else {
                    valueTextView.setText(LocaleController.getString(R.string.InstallingEmojiSet), animated);
                }
            } else if (EmojiHelper.getInstance().isEmojiPackDownloading(packInfo)) {
                setProgress(true, animated);
                setChecked(false, animated);
            } else {
                setProgress(false, animated);
                setChecked(false, animated);
                String status;
                if (EmojiHelper.getInstance().isInstalledOldVersion(packInfo.getPackId(), packInfo.getPackVersion())) {
                    status = LocaleController.formatString(R.string.UpdateEmojiSet, AndroidUtilities.formatFileSize(packInfo.getFileSize()));
                } else {
                    status = LocaleController.formatString(R.string.DownloadEmojiSet, AndroidUtilities.formatFileSize(packInfo.getFileSize()));
                }
                valueTextView.setText(status, animated);
            }
        }
    }

    public EmojiHelper.EmojiPackBase getPack() {
        return pack;
    }
}