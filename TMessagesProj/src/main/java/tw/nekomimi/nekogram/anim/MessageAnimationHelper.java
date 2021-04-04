package tw.nekomimi.nekogram.anim;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.ContextLinkCell;
import org.telegram.ui.Cells.PhotoAttachPhotoCell;
import org.telegram.ui.Cells.StickerEmojiCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.ChatActivityEnterView;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.VoiceMessageEnterTransition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;

public class MessageAnimationHelper {
    private final ChatActivity chatActivity;
    private ChatActivityEnterView input;

    private int nextMessageOffsetY;
    private int inputX;
    private int inputY;
    private int inputBarY;
    private int inputBarH;
    private int nextMessageScrollY;
    private int msgY;
    private MessageType nextMessageType;
    private boolean needAnimateList;
    private StickerEmojiCell nextMessageStickerCell;
    private ContextLinkCell nextMessageGifCell;
    private RecyclerListView chatListView;

    private ArrayList<PhotoAttachPhotoCell> nextPhotoMessageAttachCells;
    private AnimatorSet attachSheetDismissAnim;
    private int runningAnimationsCount = 0;

    private final Canvas fakeCanvas = new Canvas();

    public MessageAnimationHelper(ChatActivity chatActivity) {
        this.chatActivity = chatActivity;
    }

    public void setInput(ChatActivityEnterView input) {
        this.input = input;
        input.setMessageAnimationHelper(this);
    }

    public void setChatListView(RecyclerListView chatListView) {
        this.chatListView = chatListView;
    }

    public void animateMessage(ChatMessageCell cell) {
        if (!isSupportedMessageType(cell.getMessageObject())) {
            if (attachSheetDismissAnim != null && !attachSheetDismissAnim.isStarted()) {
                attachSheetDismissAnim.start();
            }
            return;
        }
        needAnimateList = true;
        cell.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                cell.getViewTreeObserver().removeOnPreDrawListener(this);

                if (runningAnimationsCount == 0)
                    enableInputOverlaying(true);
                runningAnimationsCount++;

                int[] loc = {0, 0};
                cell.setTranslationY(0);
                cell.getLocationInWindow(loc);
                msgY = loc[1];

                cell.draw(fakeCanvas); // needed to calculate internal layout

                cell.setHasTransientState(true);
                cell.setAlpha(1f);

                ArrayList<Animator> animators = new ArrayList<>();
                AnimatorSet set = new AnimatorSet();
                ArrayList<ChatMessageCell> additionalCells = new ArrayList<>();

                if (cell.getMessageObject().type == 0)
                    animateTextMessage(cell, animators);
                else if (cell.getMessageObject().isAnimatedEmoji() || cell.getMessageObject().isDice())
                    animateAnimatedEmojiMessage(cell, animators);
                else if (cell.getMessageObject().isSticker() || cell.getMessageObject().isAnimatedSticker()) {
                    animateStickerMessage(cell, animators, nextMessageStickerCell);
                    nextMessageStickerCell = null;
                } else if (cell.getMessageObject().isGif()) {
                    animateGifMessage(cell, animators, nextMessageGifCell);
                    nextMessageGifCell = null;
                } else if (cell.getMessageObject().isVoice())
                    animateVoiceMessage(cell, animators);
                else if (cell.getMessageObject().isPhoto() || cell.getMessageObject().isVideo()) {
                    if (cell.getCurrentMessagesGroup() == null) {
                        animatePhotoMessages(Collections.singletonList(cell), animators, set);
                    } else {
                        ArrayList<ChatMessageCell> cells = new ArrayList<>();
                        long groupID = cell.getCurrentMessagesGroup().groupId;
                        for (int i = chatListView.getChildAdapterPosition(cell); i >= 0; i--) {
                            RecyclerView.ViewHolder holder = chatListView.findViewHolderForAdapterPosition(i);
                            if (holder != null && holder.itemView instanceof ChatMessageCell) {
                                ChatMessageCell listCell = (ChatMessageCell) holder.itemView;
                                if (listCell.getCurrentMessagesGroup() != null && listCell.getCurrentMessagesGroup().groupId == groupID) {
                                    cells.add(listCell);
                                    if (listCell != cell) {
                                        listCell.draw(fakeCanvas);
                                        listCell.setHasTransientState(true);
                                        listCell.setTranslationY(0);
                                        listCell.setAlpha(1f);
                                        additionalCells.add(listCell);
                                    }
                                }
                            }
                        }
                        animatePhotoMessages(cells, animators, set);
                    }
                }

                if (cell.getMessageObject().isReply()) {
                    animateReplyLayout(cell, animators);
                }

                set.playTogether(animators);
                set.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        cell.setHasTransientState(false);
                        cell.getTransitionParams().resetAnimation();
                        for (ChatMessageCell c : additionalCells) {
                            c.setHasTransientState(false);
                            c.getTransitionParams().resetAnimation();
                        }
                        attachSheetDismissAnim = null;
                        runningAnimationsCount--;
                        if (runningAnimationsCount == 0)
                            enableInputOverlaying(false);
                    }
                });
                set.start();

                return true;
            }
        });
    }

    private void animateTextMessage(ChatMessageCell cell, List<Animator> outAnimators) {
        AnimationSettings.BubbleMessageAnimationParameters params;
        switch (nextMessageType) {
            case WEB_PREVIEW:
                params = AnimationSettings.linkPreviewMessageParams;
                break;
            case LONG_TEXT:
                params = AnimationSettings.longTextMessageParams;
                break;
            case SHORT_TEXT:
            default:
                params = AnimationSettings.shortTextMessageParams;
                break;
        }

        cell.getTransitionParams().deltaLeft = -cell.getBackgroundDrawableLeft() + inputX - (cell.getTextX() - cell.getBackgroundDrawableLeft());
        animateTranslationYFromInput(cell, outAnimators, params, true);
        if (cell.getMessageObject().getEmojiOnlyCount() == 0)
            cell.getTransitionParams().textScale = input.getEditField().getTextSize() / AndroidUtilities.dp(SharedConfig.fontSize);
        else {
            cell.setTranslationY(cell.getTranslationY() + AndroidUtilities.dp(3.5f));
            cell.getTransitionParams().textScale = (input.getEditField().getPaint().descent() - input.getEditField().getPaint().ascent()) / (float) cell.getMessageObject().textLayoutBlocks.get(0).height;
        }

        cell.getTransitionParams().deltaTop = -(input.getEditField().getTotalPaddingTop() + input.getEditField().getTop()) + AndroidUtilities.dp(7) + nextMessageScrollY;
        if (cell.getMessageObject().isReply())
            cell.getTransitionParams().deltaTop -= AndroidUtilities.dp(6);
        cell.getTransitionParams().deltaBottom = inputBarH - cell.getHeight() + nextMessageScrollY;
        cell.getTransitionParams().animateDrawingTimeAlpha = true;
        cell.getTransitionParams().clipTextToBgBounds = true;
        cell.getTransitionParams().overrideTextColor = true;
        cell.getTransitionParams().overlayBubbleWithColor = true;
        cell.getTransitionParams().animateChangeProgress = 0f;
        cell.getTransitionParams().bubbleOverlayColor = Theme.getColor(Theme.key_chat_messagePanelBackground);
        cell.getTransitionParams().bubbleColorOverlayAlpha = 1f;

        ValueAnimator bubbleShape = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofInt("deltaTop", cell.getTransitionParams().deltaTop, 0),
                PropertyValuesHolder.ofInt("deltaBottom", cell.getTransitionParams().deltaBottom, 0));
        bubbleShape.addUpdateListener(animation -> {
            cell.getTransitionParams().deltaTop = (Integer) animation.getAnimatedValue("deltaTop");
            cell.getTransitionParams().deltaBottom = (Integer) animation.getAnimatedValue("deltaBottom");
            cell.invalidate();
        });
        ValueAnimator bubbleX = ValueAnimator.ofInt(cell.getTransitionParams().deltaLeft, 0);
        bubbleX.addUpdateListener(animation -> {
            cell.getTransitionParams().deltaLeft = (Integer) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator bubbleTextSize = ValueAnimator.ofFloat(cell.getTransitionParams().textScale, 1f);
        bubbleTextSize.addUpdateListener(animation -> {
            cell.getTransitionParams().textScale = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator timeAppear = ValueAnimator.ofFloat(0f, 1f);
        timeAppear.addUpdateListener(animation -> {
            cell.getTransitionParams().animateChangeProgress = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        PropertyValuesHolder pvh;
        ValueAnimator colorChange = ValueAnimator.ofPropertyValuesHolder(pvh = PropertyValuesHolder.ofInt("textColor", input.getEditField().getCurrentTextColor(), Theme.getColor(Theme.key_chat_messageTextOut)),
                PropertyValuesHolder.ofFloat("bubbleColor", 1f, 0f));
        pvh.setEvaluator(new ArgbEvaluator());
        colorChange.addUpdateListener(animation -> {
            cell.getTransitionParams().textColor = (Integer) animation.getAnimatedValue("textColor");
            cell.getTransitionParams().bubbleColorOverlayAlpha = (Float) animation.getAnimatedValue("bubbleColor");
            cell.invalidate();
        });

        outAnimators.add(setupAnimator(bubbleX, params.xPositionTiming, params.duration));
        outAnimators.add(setupAnimator(bubbleShape, params.bubbleShapeTiming, params.duration));
        outAnimators.add(setupAnimator(bubbleTextSize, params.scaleTiming, params.duration));
        outAnimators.add(setupAnimator(timeAppear, params.timeAppearTiming, params.duration));
        outAnimators.add(setupAnimator(colorChange, params.colorChangeTiming, params.duration));
    }

    private void animateAnimatedEmojiMessage(ChatMessageCell cell, List<Animator> outAnimators) {
        float inputTextH = input.getEditField().getPaint().descent() - input.getEditField().getPaint().ascent();
        float scale = inputTextH / cell.getPhotoImage().getImageHeight() * 1.15f;
        cell.getTransitionParams().transformStickerImage = true;
        cell.getTransitionParams().stickerImageScale = scale;
        cell.getTransitionParams().stickerImageTranslateX = -(cell.getPhotoImage().getImageX() - inputX) - cell.getPhotoImage().getImageWidth() / 2f + cell.getPhotoImage().getImageWidth() * scale / 2f - AndroidUtilities.dp(2);
        cell.getTransitionParams().stickerImageTranslateY = inputY - msgY + nextMessageOffsetY - cell.getPhotoImage().getImageHeight() / 2f + cell.getPhotoImage().getImageHeight() * scale / 2f - AndroidUtilities.dp(3);

        ValueAnimator scaleAnim = ValueAnimator.ofFloat(scale, 1f);
        scaleAnim.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageScale = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator transX = ValueAnimator.ofFloat(cell.getTransitionParams().stickerImageTranslateX, 0f);
        transX.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageTranslateX = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator transY = ValueAnimator.ofFloat(cell.getTransitionParams().stickerImageTranslateY, 0f);
        transY.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageTranslateY = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });

        AnimationSettings.StickerMessageAnimationParameters params = AnimationSettings.emojiMessageParams;

        outAnimators.add(setupAnimator(scaleAnim, params.scaleTiming, params.duration));
        outAnimators.add(setupAnimator(transX, params.xPositionTiming, params.duration));
        outAnimators.add(setupAnimator(transY, params.yPositionTiming, params.duration));

        Emoji.EmojiSpan[] spans = ((Spannable) cell.getMessageObject().messageText).getSpans(0, cell.getMessageObject().messageText.length(), Emoji.EmojiSpan.class);
        Emoji.EmojiDrawable drawable = new Emoji.EmojiDrawable(((Emoji.EmojiDrawable) spans[0].getDrawable()).getDrawableInfo());
        drawable.setForceDraw(true);
        cell.getTransitionParams().stickerPlaceholder = drawable;
        cell.getTransitionParams().stickerCrossfadeAlpha = 0;

        ValueAnimator placeholderCrossfade = ValueAnimator.ofInt(0, 255);
        placeholderCrossfade.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerCrossfadeAlpha = (Integer) animation.getAnimatedValue();
            cell.invalidate();
        });
        outAnimators.add(setupAnimator(placeholderCrossfade, params.placeholderCrossfadeTiming, params.duration));

        cell.getTransitionParams().animateDrawingTimeAlpha = true;
        cell.getTransitionParams().animateChangeProgress = 0f;
        ValueAnimator timeAppear = ValueAnimator.ofFloat(0f, 1f);
        timeAppear.addUpdateListener(animation -> {
            cell.getTransitionParams().animateChangeProgress = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        outAnimators.add(setupAnimator(timeAppear, params.timeAppearTiming, params.duration));

        input.getEditField().setAlpha(0f);
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(input.getEditField(), View.ALPHA, 1f), params.reappearTiming, params.duration));

        cell.getPhotoImage().setCrossfadeDuration(1);
    }

    private void animateStickerMessage(ChatMessageCell cell, List<Animator> outAnimators, StickerEmojiCell stickerCell) {
        float scale = Math.min(stickerCell.getImageView().getHeight() / cell.getPhotoImage().getImageHeight(), stickerCell.getImageView().getWidth() / cell.getPhotoImage().getImageWidth());
        cell.getTransitionParams().transformStickerImage = true;
        cell.getTransitionParams().stickerImageScale = scale;

        int[] loc = {0, 0};
        stickerCell.getLocationInWindow(loc);
        float viewCenterX = (float) loc[0] + stickerCell.getWidth() / 2f;
        float viewCenterY = (float) loc[1] + stickerCell.getHeight() / 2f;
        chatListView.getLocationInWindow(loc);
        viewCenterX -= loc[0];

        cell.getTransitionParams().stickerImageTranslateX = -(cell.getPhotoImage().getCenterX() - viewCenterX);
        cell.getTransitionParams().stickerImageTranslateY = viewCenterY - (cell.getPhotoImage().getCenterY() + msgY);

        ValueAnimator scaleAnim = ValueAnimator.ofFloat(scale, 1f);
        scaleAnim.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageScale = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator transX = ValueAnimator.ofFloat(cell.getTransitionParams().stickerImageTranslateX, 0f);
        transX.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageTranslateX = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator transY = ValueAnimator.ofFloat(cell.getTransitionParams().stickerImageTranslateY, 0f);
        transY.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageTranslateY = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });

        AnimationSettings.StickerMessageAnimationParameters params = AnimationSettings.stickerMessageParams;

        outAnimators.add(setupAnimator(scaleAnim, params.scaleTiming, params.duration));
        outAnimators.add(setupAnimator(transX, params.xPositionTiming, params.duration));
        outAnimators.add(setupAnimator(transY, params.yPositionTiming, params.duration));

        cell.getPhotoImage().setCrossfadeDuration(1);
        Bitmap bmp = stickerCell.getImageView().getImageReceiver().getBitmap();
        if (bmp != null) {
            cell.getTransitionParams().stickerPlaceholder = new BitmapDrawable(bmp);
            cell.getTransitionParams().stickerCrossfadeAlpha = 0;
            ValueAnimator placeholderCrossfade = ValueAnimator.ofInt(0, 255);
            placeholderCrossfade.addUpdateListener(animation -> {
                cell.getTransitionParams().stickerCrossfadeAlpha = (Integer) animation.getAnimatedValue();
                cell.invalidate();
            });
            outAnimators.add(setupAnimator(placeholderCrossfade, params.placeholderCrossfadeTiming, params.duration));
        }

        cell.getTransitionParams().animateDrawingTimeAlpha = true;
        cell.getTransitionParams().animateChangeProgress = 0f;
        ValueAnimator timeAppear = ValueAnimator.ofFloat(0f, 1f);
        timeAppear.addUpdateListener(animation -> {
            cell.getTransitionParams().animateChangeProgress = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        outAnimators.add(setupAnimator(timeAppear, params.timeAppearTiming, params.duration));

        stickerCell.setAlpha(0f);
        stickerCell.setScaleX(.5f);
        stickerCell.setScaleY(.5f);
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(stickerCell, View.ALPHA, 1f), params.reappearTiming, params.duration));
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(stickerCell, View.SCALE_X, 1f), params.reappearTiming, params.duration));
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(stickerCell, View.SCALE_Y, 1f), params.reappearTiming, params.duration));
    }

    private void animateGifMessage(ChatMessageCell cell, List<Animator> outAnimators, ContextLinkCell gifCell) {
        float scale = Math.min(gifCell.getHeight() / cell.getPhotoImage().getImageHeight(), gifCell.getWidth() / cell.getPhotoImage().getImageWidth());
        cell.getTransitionParams().transformStickerImage = true;
        cell.getTransitionParams().stickerImageScale = scale;

        int[] loc = {0, 0};
        gifCell.getLocationInWindow(loc);
        float viewCenterX = (float) loc[0] + gifCell.getWidth() / 2f;
        float viewCenterY = (float) loc[1] + gifCell.getHeight() / 2f;
        chatListView.getLocationInWindow(loc);
        viewCenterX -= loc[0];

        cell.getTransitionParams().stickerImageTranslateX = -(cell.getPhotoImage().getCenterX() - viewCenterX);
        cell.getTransitionParams().stickerImageTranslateY = viewCenterY - (cell.getPhotoImage().getCenterY() + msgY);

        ValueAnimator scaleAnim = ValueAnimator.ofFloat(scale, 1f);
        scaleAnim.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageScale = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator transX = ValueAnimator.ofFloat(cell.getTransitionParams().stickerImageTranslateX, 0f);
        transX.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageTranslateX = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator transY = ValueAnimator.ofFloat(cell.getTransitionParams().stickerImageTranslateY, 0f);
        transY.addUpdateListener(animation -> {
            cell.getTransitionParams().stickerImageTranslateY = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });

        AnimationSettings.GifMessageAnimationParameters params = AnimationSettings.gifMessageAnimationParameters;

        outAnimators.add(setupAnimator(scaleAnim, params.scaleTiming, params.duration));
        outAnimators.add(setupAnimator(transX, params.xPositionTiming, params.duration));
        outAnimators.add(setupAnimator(transY, params.yPositionTiming, params.duration));

        cell.getPhotoImage().setCrossfadeDuration(1);
        Bitmap bmp = gifCell.getPhotoImage().getBitmap();
        if (bmp != null) {
            cell.getTransitionParams().stickerPlaceholder = new BitmapDrawable(bmp);
            cell.getTransitionParams().stickerCrossfadeAlpha = 0;
            ValueAnimator placeholderCrossfade = ValueAnimator.ofInt(0, 255);
            placeholderCrossfade.addUpdateListener(animation -> {
                cell.getTransitionParams().stickerCrossfadeAlpha = (Integer) animation.getAnimatedValue();
                cell.invalidate();
            });
            outAnimators.add(setupAnimator(placeholderCrossfade, params.placeholderCrossfadeTiming, params.duration));
        }

        cell.getTransitionParams().animateDrawingTimeAlpha = true;
        cell.getTransitionParams().animateChangeProgress = 0f;
        ValueAnimator timeAppear = ValueAnimator.ofFloat(0f, 1f);
        timeAppear.addUpdateListener(animation -> {
            cell.getTransitionParams().animateChangeProgress = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        outAnimators.add(setupAnimator(timeAppear, params.timeAppearTiming, params.duration));

        gifCell.setAlpha(0f);
        gifCell.setScaleX(.5f);
        gifCell.setScaleY(.5f);
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(gifCell, View.ALPHA, 1f), params.reappearTiming, params.duration));
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(gifCell, View.SCALE_X, 1f), params.reappearTiming, params.duration));
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(gifCell, View.SCALE_Y, 1f), params.reappearTiming, params.duration));
    }

    private void animateTranslationYFromInput(ChatMessageCell cell, List<Animator> outAnimators, AnimationSettings.MessageAnimationParameters params, boolean hasText) {
        if (hasText)
            cell.setTranslationY(inputY - msgY + nextMessageOffsetY - cell.getTextY() - AndroidUtilities.dpf2(.5f));
        else
            cell.setTranslationY(inputY - msgY);
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(cell, View.TRANSLATION_Y, 0f), params.yPositionTiming, params.duration));
    }

    private void animateReplyLayout(ChatMessageCell cell, List<Animator> outAnimators) {
        cell.getTransitionParams().animateReplyLayout = true;

        input.getTopView().setAlpha(0f);
        cell.getTransitionParams().replyIconDrawable = cell.getContext().getResources().getDrawable(R.drawable.msg_panel_reply).mutate();
        cell.getTransitionParams().replyIconDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_chat_replyPanelIcons), PorterDuff.Mode.MULTIPLY));
        cell.getTransitionParams().replyNameColor = Theme.getColor(Theme.key_chat_replyPanelName);
        cell.getTransitionParams().replyTextColor = Theme.getColor(Theme.key_chat_replyPanelMessage);
        cell.getTransitionParams().replyBgOffsetX = AndroidUtilities.dp(-28);
        int finalTextColor, finalNameColor;
        AnimationSettings.TimingParameters timing;
        long duration;

        if (cell.getMessageObject().shouldDrawWithoutBackground()) {
            cell.getTransitionParams().replyOffsetX = AndroidUtilities.dp(19);
            cell.getTransitionParams().replyOffsetY = inputBarY - msgY - AndroidUtilities.dp(4);
            finalTextColor = Theme.getColor(Theme.key_chat_stickerReplyMessageText);
            finalNameColor = Theme.getColor(Theme.key_chat_stickerReplyNameText);

            duration = getParamsForNextMessage().duration;
            timing = getParamsForNextMessage().yPositionTiming;
        } else {
            cell.getTransitionParams().replyOffsetX = cell.getTransitionParams().deltaLeft - AndroidUtilities.dp(11);
            cell.getTransitionParams().replyOffsetY = AndroidUtilities.dp(-15) + nextMessageScrollY;
            if (cell.getMessageObject().hasValidReplyMessageObject() && (cell.getMessageObject().replyMessageObject.type == 0 || !TextUtils.isEmpty(cell.getMessageObject().replyMessageObject.caption)) && !(cell.getMessageObject().replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGame || cell.getMessageObject().replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice))
                finalTextColor = Theme.getColor(Theme.key_chat_outReplyMessageText);
            else
                finalTextColor = Theme.getColor(Theme.key_chat_outReplyMediaMessageText);
            finalNameColor = Theme.getColor(Theme.key_chat_outReplyNameText);

            AnimationSettings.BubbleMessageAnimationParameters params = (AnimationSettings.BubbleMessageAnimationParameters) getParamsForNextMessage();
            timing = params.bubbleShapeTiming;
            duration = params.duration;
        }

        ValueAnimator animatorOfEverything = ValueAnimator.ofPropertyValuesHolder(
                PropertyValuesHolder.ofFloat("offsetY", cell.getTransitionParams().replyOffsetY, 0f),
                PropertyValuesHolder.ofFloat("offsetX", cell.getTransitionParams().replyOffsetX, 0f),
                PropertyValuesHolder.ofInt("bgOffsetX", cell.getTransitionParams().replyBgOffsetX, 0),
                PropertyValuesHolder.ofFloat("iconProgress", 0f, 1f),
                animateARGB(PropertyValuesHolder.ofInt("nameColor", cell.getTransitionParams().replyNameColor, finalNameColor)),
                animateARGB(PropertyValuesHolder.ofInt("textColor", cell.getTransitionParams().replyTextColor, finalTextColor))
        );
        animatorOfEverything.addUpdateListener(animation -> {
            cell.getTransitionParams().replyOffsetX = (Float) animation.getAnimatedValue("offsetX");
            cell.getTransitionParams().replyOffsetY = (Float) animation.getAnimatedValue("offsetY");
            cell.getTransitionParams().replyBgOffsetX = (Integer) animation.getAnimatedValue("bgOffsetX");
            cell.getTransitionParams().replyIconTransitionProgress = (Float) animation.getAnimatedValue("iconProgress");
            cell.getTransitionParams().replyNameColor = (Integer) animation.getAnimatedValue("nameColor");
            cell.getTransitionParams().replyTextColor = (Integer) animation.getAnimatedValue("textColor");
            cell.invalidate();
        });
        animatorOfEverything.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                input.getTopView().setAlpha(1f);
            }
        });
        outAnimators.add(setupAnimator(animatorOfEverything, timing, duration));
    }

    private void animateVoiceMessage(ChatMessageCell cell, List<Animator> outAnimators) {
        AnimationSettings.BubbleMessageAnimationParameters params = AnimationSettings.voiceMessageParams;
        VoiceMessageEnterTransition transition = new VoiceMessageEnterTransition((FrameLayout) chatActivity.getFragmentView(), cell, input, chatListView);

        outAnimators.add(setupAnimator(transition.getAnimator(), params.scaleTiming, params.duration));

        cell.getTransitionParams().deltaLeft = -cell.getBackgroundDrawableLeft();
        animateTranslationYFromInput(cell, outAnimators, params, false);

        if (cell.getMessageObject().isReply())
            cell.getTransitionParams().deltaTop -= AndroidUtilities.dp(6);
        cell.getTransitionParams().deltaBottom = inputBarH - cell.getHeight() + nextMessageScrollY;
        cell.getTransitionParams().animateDrawingTimeAlpha = true;
        cell.getTransitionParams().overlayBubbleWithColor = true;
        cell.getTransitionParams().animateChangeProgress = 0f;
        cell.getTransitionParams().bubbleOverlayColor = Theme.getColor(Theme.key_chat_messagePanelBackground);
        cell.getTransitionParams().bubbleColorOverlayAlpha = 1f;

        ValueAnimator bubbleShape = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofInt("deltaTop", cell.getTransitionParams().deltaTop, 0),
                PropertyValuesHolder.ofInt("deltaBottom", cell.getTransitionParams().deltaBottom, 0));
        bubbleShape.addUpdateListener(animation -> {
            cell.getTransitionParams().deltaTop = (Integer) animation.getAnimatedValue("deltaTop");
            cell.getTransitionParams().deltaBottom = (Integer) animation.getAnimatedValue("deltaBottom");
            cell.invalidate();
        });
        ValueAnimator bubbleX = ValueAnimator.ofInt(cell.getTransitionParams().deltaLeft, 0);
        bubbleX.addUpdateListener(animation -> {
            cell.getTransitionParams().deltaLeft = (Integer) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator timeAppear = ValueAnimator.ofFloat(0f, 1f);
        timeAppear.addUpdateListener(animation -> {
            cell.getTransitionParams().animateChangeProgress = (Float) animation.getAnimatedValue();
            cell.invalidate();
        });
        ValueAnimator colorChange = ValueAnimator.ofPropertyValuesHolder(PropertyValuesHolder.ofFloat("bubbleColor", 1f, 0f));
        colorChange.addUpdateListener(animation -> {
            cell.getTransitionParams().bubbleColorOverlayAlpha = (Float) animation.getAnimatedValue("bubbleColor");
            cell.invalidate();
        });

        outAnimators.add(setupAnimator(bubbleX, params.xPositionTiming, params.duration));
        outAnimators.add(setupAnimator(bubbleShape, params.bubbleShapeTiming, params.duration));
        outAnimators.add(setupAnimator(timeAppear, params.timeAppearTiming, params.duration));
        outAnimators.add(setupAnimator(colorChange, params.colorChangeTiming, params.duration));
    }

    private void animatePhotoMessages(List<ChatMessageCell> cells, List<Animator> outAnimators, AnimatorSet animatorSet) {
        AnimationSettings.BubbleMessageAnimationParameters params = AnimationSettings.photoMessageParams;
        MessageObject.GroupedMessages group = cells.get(0).getCurrentMessagesGroup();
        ChatMessageCell.TransitionParams transitionParams = cells.get(cells.size() - 1).getTransitionParams();

        PhotosTransitionOverlayView overlay = new PhotosTransitionOverlayView(chatActivity.getParentActivity(), chatListView, chatActivity);
        WindowManager wm = (WindowManager) chatActivity.getParentActivity().getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.LAST_APPLICATION_WINDOW);
        lp.width = lp.height = WindowManager.LayoutParams.MATCH_PARENT;
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
        wm.addView(overlay, lp);

        overlay.initialPositions = new ArrayList<>();
        overlay.imageReceivers = new ArrayList<>();
        overlay.messageCells = new ArrayList<>();
        int[] loc = {0, 0};
        int i = 0;
        boolean setOffset = false;
        for (PhotoAttachPhotoCell cell : nextPhotoMessageAttachCells) {
            if (cell != null) {
                if (cells.size() <= i)
                    break;
                cell.getImageView().setAlpha(0f);
                cell.getCheckBox().setAlpha(0f);
                cell.getCheckFrame().setAlpha(0f);
                cell.getImageView().getLocationOnScreen(loc);
                RectF pos = new RectF(loc[0], loc[1], loc[0] + cell.getImageView().getWidth() * cell.getScale(), loc[1] + cell.getImageView().getHeight() * cell.getScale());
                if (!setOffset) {
                    setOffset = true;
                    cell.getCheckBox().getLocationOnScreen(loc);
                    overlay.setCheckOffset((loc[0] + cell.getCheckBox().getWidth() / 2f) - pos.centerX(), (loc[1] + cell.getCheckBox().getWidth() / 2f) - pos.centerY());
                }
                overlay.initialPositions.add(pos);
                ImageReceiver receiver = new ImageReceiver(overlay);
                ImageReceiver srcReceiver = cell.getImageView().getImageReceiver();
                receiver.setCrossfadeDuration(1);
                receiver.setImage(srcReceiver.getImageLocation(), srcReceiver.getImageFilter(), srcReceiver.getThumbLocation(), srcReceiver.getThumbFilter(), srcReceiver.getExt(), srcReceiver.getParentObject(), srcReceiver.getCacheType());
                overlay.imageReceivers.add(receiver);
                overlay.messageCells.add(cells.get(i));
            } else {
                overlay.initialPositions.add(null);
                overlay.imageReceivers.add(null);
                overlay.messageCells.add(null);
            }
            i++;
        }

        int minTop = Integer.MAX_VALUE, maxBottom = Integer.MIN_VALUE;
        for (ChatMessageCell cell : cells) {
            if (cell.getTop() < minTop)
                minTop = cell.getTop();
            if (cell.getBottom() > maxBottom)
                maxBottom = cell.getBottom();
        }
        int totalHeight = maxBottom - minTop;
        for (ChatMessageCell cell : overlay.messageCells) {
            if (cell == null)
                continue;
            cell.getPhotoImage().setAlpha(0f);
            cell.setVoiceTransitionInProgress(true);
        }
        for (ChatMessageCell cell : cells) {
            cell.setTranslationY(totalHeight);
            outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(cell, View.TRANSLATION_Y, 0f), params.yPositionTiming, params.duration));
        }

        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(overlay, "xProgress", 0f, 1f), params.xPositionTiming, params.duration));
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(overlay, "yProgress", 0f, 1f), params.yPositionTiming, params.duration));
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(overlay, "resizeProgress", 0f, 1f), params.bubbleShapeTiming, params.duration));
        outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(overlay, "iconTransitionProgress", 0f, 1f), params.scaleTiming, params.duration));

        boolean hasCaption;
        if (group == null)
            hasCaption = cells.get(0).hasCaptionLayout();
        else
            hasCaption = group.hasCaption;

        transitionParams.animateDrawingTimeAlpha = true;
        if (hasCaption) {
            transitionParams.animateChangeProgress = 0f;
            ValueAnimator timeAppear = ValueAnimator.ofFloat(0f, 1f);
            timeAppear.addUpdateListener(animation -> {
                transitionParams.animateChangeProgress = (Float) animation.getAnimatedValue();
                chatListView.invalidate();
            });
            outAnimators.add(setupAnimator(timeAppear, params.timeAppearTiming, params.duration));
        } else {
            outAnimators.add(setupAnimator(ObjectAnimator.ofFloat(overlay, "timeAlpha", 0f, 1f), params.timeAppearTiming, params.duration));
        }

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                wm.removeView(overlay);
                for (PhotoAttachPhotoCell cell : nextPhotoMessageAttachCells) {
                    if (cell != null) {
                        cell.getImageView().setAlpha(1f);
                        cell.getCheckBox().setAlpha(1f);
                        cell.getCheckFrame().setAlpha(1f);
                    }
                }
                for (ChatMessageCell cell : cells) {
                    cell.getPhotoImage().setAlpha(1f);
                    cell.setVoiceTransitionInProgress(false);
                }
            }
        });

        if (attachSheetDismissAnim != null) {
            outAnimators.add(setupAnimator(attachSheetDismissAnim, params.colorChangeTiming, params.duration));
        }
    }

    private PropertyValuesHolder animateARGB(PropertyValuesHolder holder) {
        holder.setEvaluator(new ArgbEvaluator());
        return holder;
    }

    private boolean isSupportedMessageType(MessageObject msg) {
        if (Build.VERSION.SDK_INT < 21) {
            return false;
        }
        if (!NekoConfig.messageAnimation) {
            return false;
        }
        if (msg.type == 0 && nextMessageType != null) // text message
            return true;
        if (msg.isAnimatedEmoji() || msg.isDice())
            return true;
        if ((msg.isSticker() || msg.isAnimatedSticker()) && nextMessageStickerCell != null)
            return true;
        if (msg.isGif() && nextMessageGifCell != null)
            return true;
        if (msg.isVoice())
            return true;
        return (msg.isPhoto() || msg.isVideo()) && nextPhotoMessageAttachCells != null;
    }

    private Animator setupAnimator(Animator anim, AnimationSettings.TimingParameters timing, long duration) {
        anim.setInterpolator(timing.getInterpolator());
        anim.setDuration(timing.scaledDuration(duration));
        anim.setStartDelay(timing.scaledStartDelay(duration));
        return anim;
    }

    public void beforeSendTextMessage(CharSequence message) {
        updateInputBounds();

        if (message == null)
            return;

        EditText edit = input.getEditField();
        if (input.hasWebPage()) {
            nextMessageType = MessageType.WEB_PREVIEW;
        } else if (edit.canScrollVertically(1) || edit.canScrollVertically(-1)) {
            nextMessageType = MessageType.LONG_TEXT;
        } else {
            nextMessageType = MessageType.SHORT_TEXT;
            if (message instanceof Spannable) {
                Spannable spannable = (Spannable) message;
                CharacterStyle[] spans = spannable.getSpans(0, message.length(), CharacterStyle.class);
                if (spans.length == 1 && spans[0] instanceof Emoji.EmojiSpan && spannable.getSpanStart(spans[0]) == 0 && spannable.getSpanEnd(spans[0]) == message.length()) {
                    TLRPC.Document sticker = MediaDataController.getInstance(chatActivity.getCurrentAccount()).getEmojiAnimatedSticker(message);
                    if (sticker != null) {
                        nextMessageType = MessageType.ANIMATED_EMOJI;
                    }
                }
            }
        }
    }

    public void beforeSendStickerMessage(View view) {
        if (view instanceof StickerEmojiCell) {
            nextMessageType = MessageType.STICKER;
            nextMessageStickerCell = (StickerEmojiCell) view;
        }
    }

    public void beforeSendGifMessage(View view) {
        if (view instanceof ContextLinkCell) {
            nextMessageType = MessageType.GIF;
            nextMessageGifCell = (ContextLinkCell) view;
        }
    }

    public void beforeSendPhotos(ArrayList<PhotoAttachPhotoCell> cells) {
        nextMessageType = MessageType.PHOTO;

        nextPhotoMessageAttachCells = cells;
    }

    public void beforeSendVoiceMessage() {
        nextMessageType = MessageType.VOICE;
        updateInputBounds();
    }

    public void setupInputDownsizeAnimation(ValueAnimator anim) {
        AnimationSettings.MessageAnimationParameters params = getParamsForNextMessage();
        setupAnimator(anim, params.yPositionTiming, params.duration);
    }

    public void setupExistingMessagesSlideUpAnimation(AnimatorSet anim) {
        if (needAnimateList) {
            AnimationSettings.MessageAnimationParameters params = getParamsForNextMessage();
            setupAnimator(anim, params.yPositionTiming, params.duration);
        }
    }

    public void setupAttachAlertCloseAnimation(AnimatorSet anim) {
        attachSheetDismissAnim = anim;
    }

    public void doneSettingUpListAnimations() {
        needAnimateList = false;
    }

    private void updateInputBounds() {
        EditText edit = input.getEditField();
        nextMessageOffsetY = -edit.getScrollY() + edit.getTotalPaddingTop();
        nextMessageScrollY = edit.getScrollY();
        int[] loc = {0, 0};
        edit.getLocationInWindow(loc);
        inputX = loc[0] - (int) edit.getTranslationX();
        inputY = loc[1];
        input.getLocationInWindow(loc);
        inputBarY = loc[1];
        inputBarH = input.getHeight();

        chatListView.getLocationInWindow(loc);
        inputX -= loc[0];
    }

    private void enableInputOverlaying(boolean enable) {
        if (Build.VERSION.SDK_INT >= 21) {
            input.setTranslationZ(enable ? -10 : 0);
            if (input.getEmojiView() != null) {
                input.getEmojiView().setTranslationZ(enable ? -10 : 0);
            }
        }
    }

    private AnimationSettings.MessageAnimationParameters getParamsForNextMessage() {
        if (nextMessageType == null) {
            return AnimationSettings.shortTextMessageParams;
        }
        switch (nextMessageType) {
            case LONG_TEXT:
                return AnimationSettings.longTextMessageParams;
            case WEB_PREVIEW:
                return AnimationSettings.linkPreviewMessageParams;
            case ANIMATED_EMOJI:
                return AnimationSettings.emojiMessageParams;
            case STICKER:
                return AnimationSettings.stickerMessageParams;
            case GIF:
                return AnimationSettings.gifMessageAnimationParameters;
            case VOICE:
                return AnimationSettings.voiceMessageParams;
            case PHOTO:
                return AnimationSettings.photoMessageParams;
            case SHORT_TEXT:
            default:
                return AnimationSettings.shortTextMessageParams;
        }
    }

    private enum MessageType {
        SHORT_TEXT,
        LONG_TEXT,
        WEB_PREVIEW,
        ANIMATED_EMOJI,
        STICKER,
        VOICE,
        PHOTO,
        GIF
    }
}
