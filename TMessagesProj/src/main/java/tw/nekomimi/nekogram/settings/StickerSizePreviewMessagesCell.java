package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.BackgroundGradientDrawable;
import org.telegram.ui.Components.LayoutHelper;

import tw.nekomimi.nekogram.NekoConfig;

@SuppressLint("ViewConstructor")
public class StickerSizePreviewMessagesCell extends LinearLayout {

    private BackgroundGradientDrawable.Disposable backgroundGradientDisposable;
    private BackgroundGradientDrawable.Disposable oldBackgroundGradientDisposable;
    private Drawable backgroundDrawable;
    private Drawable oldBackgroundDrawable;
    private ChatMessageCell[] cells = new ChatMessageCell[2];
    private MessageObject[] messageObjects = new MessageObject[2];
    private Drawable shadowDrawable;
    private ActionBarLayout parentLayout;

    public StickerSizePreviewMessagesCell(Context context, ActionBarLayout layout) {
        super(context);

        parentLayout = layout;

        setWillNotDraw(false);
        setOrientation(LinearLayout.VERTICAL);
        setPadding(0, AndroidUtilities.dp(11), 0, AndroidUtilities.dp(11));

        shadowDrawable = Theme.getThemedDrawable(context, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow);

        int date = (int) (System.currentTimeMillis() / 1000) - 60 * 60;
        TLRPC.TL_message message = new TLRPC.TL_message();
        message.date = date + 10;
        message.dialog_id = 1;
        message.flags = 257;
        message.from_id = new TLRPC.TL_peerUser();
        message.from_id.user_id = UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId();
        message.id = 1;
        message.media = new TLRPC.TL_messageMediaDocument();
        message.media.flags = 1;
        message.media.document = new TLRPC.TL_document();
        message.media.document.mime_type = "image/webp";
        message.media.document.file_reference = new byte[0];
        message.media.document.access_hash = 0;
        message.media.document.date = date;
        TLRPC.TL_documentAttributeSticker attributeSticker = new TLRPC.TL_documentAttributeSticker();
        attributeSticker.alt = "🐱";
        message.media.document.attributes.add(attributeSticker);
        TLRPC.TL_documentAttributeImageSize attributeImageSize = new TLRPC.TL_documentAttributeImageSize();
        attributeImageSize.h = 512;
        attributeImageSize.w = 512;
        message.media.document.attributes.add(attributeImageSize);
        message.message = "";
        message.out = true;
        message.peer_id = new TLRPC.TL_peerUser();
        message.peer_id.user_id = 0;
        messageObjects[0] = new MessageObject(UserConfig.selectedAccount, message, true, false);
        messageObjects[0].useCustomPhoto = true;

        message = new TLRPC.TL_message();
        message.message = LocaleController.getString("StickerSizeDialogMessageReplyTo", R.string.StickerSizeDialogMessageReplyTo);
        message.date = date + 1270;
        message.dialog_id = -1;
        message.flags = 259;
        message.id = 2;
        message.media = new TLRPC.TL_messageMediaEmpty();
        message.out = false;
        message.peer_id = new TLRPC.TL_peerUser();
        message.peer_id.user_id = 1;
        messageObjects[0].customReplyName = LocaleController.getString("StickerSizeDialogName", R.string.StickerSizeDialogName);
        messageObjects[0].replyMessageObject = new MessageObject(UserConfig.selectedAccount, message, true, false);


        message = new TLRPC.TL_message();
        message.message = NekoConfig.stickerSize < 9 ? LocaleController.getString("StickerSizeDialogMessageSmallOne", R.string.StickerSizeDialogMessageSmallOne) : LocaleController.getString("StickerSizeDialogMessageBigOne", R.string.StickerSizeDialogMessageBigOne);
        message.date = date + 1270;
        message.dialog_id = -1;
        message.flags = 259;
        message.id = 2;
        message.media = new TLRPC.TL_messageMediaEmpty();
        message.out = false;
        message.peer_id = new TLRPC.TL_peerUser();
        message.peer_id.user_id = 1;
        messageObjects[1] = new MessageObject(UserConfig.selectedAccount, message, true, false);
        TLRPC.User currentUser = MessagesController.getInstance(UserConfig.selectedAccount).getUser(UserConfig.getInstance(UserConfig.selectedAccount).getClientUserId());
        messageObjects[1].customReplyName = ContactsController.formatName(currentUser.first_name, currentUser.last_name);
        messageObjects[1].replyMessageObject = messageObjects[0];

        for (int a = 0; a < cells.length; a++) {
            cells[a] = new ChatMessageCell(context);
            cells[a].setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
            });
            cells[a].isChat = false;
            cells[a].setFullyDraw(true);
            cells[a].setMessageObject(messageObjects[a], null, false, false);
            addView(cells[a], LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        for (int a = 0; a < cells.length; a++) {
            if (a == 1) {
                messageObjects[a].messageOwner.message = NekoConfig.stickerSize < 9 ? LocaleController.getString("StickerSizeDialogMessageSmallOne", R.string.StickerSizeDialogMessageSmallOne) : LocaleController.getString("StickerSizeDialogMessageBigOne", R.string.StickerSizeDialogMessageBigOne);
                messageObjects[a].applyNewText();
                messageObjects[a].resetLayout();
            }
            cells[a].setMessageObject(messageObjects[a], null, false, false);
            cells[a].invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable newDrawable = Theme.getCachedWallpaperNonBlocking();
        if (newDrawable != backgroundDrawable && newDrawable != null) {
            if (Theme.isAnimatingColor()) {
                oldBackgroundDrawable = backgroundDrawable;
                oldBackgroundGradientDisposable = backgroundGradientDisposable;
            } else if (backgroundGradientDisposable != null) {
                backgroundGradientDisposable.dispose();
                backgroundGradientDisposable = null;
            }
            backgroundDrawable = newDrawable;
        }
        float themeAnimationValue = parentLayout.getThemeAnimationValue();
        for (int a = 0; a < 2; a++) {
            Drawable drawable = a == 0 ? oldBackgroundDrawable : backgroundDrawable;
            if (drawable == null) {
                continue;
            }
            if (a == 1 && oldBackgroundDrawable != null && parentLayout != null) {
                drawable.setAlpha((int) (255 * themeAnimationValue));
            } else {
                drawable.setAlpha(255);
            }
            if (drawable instanceof ColorDrawable || drawable instanceof GradientDrawable) {
                drawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                if (drawable instanceof BackgroundGradientDrawable) {
                    final BackgroundGradientDrawable backgroundGradientDrawable = (BackgroundGradientDrawable) drawable;
                    backgroundGradientDisposable = backgroundGradientDrawable.drawExactBoundsSize(canvas, this);
                } else {
                    drawable.draw(canvas);
                }
            } else if (drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                if (bitmapDrawable.getTileModeX() == Shader.TileMode.REPEAT) {
                    canvas.save();
                    float scale = 2.0f / AndroidUtilities.density;
                    canvas.scale(scale, scale);
                    drawable.setBounds(0, 0, (int) Math.ceil(getMeasuredWidth() / scale), (int) Math.ceil(getMeasuredHeight() / scale));
                } else {
                    int viewHeight = getMeasuredHeight();
                    float scaleX = (float) getMeasuredWidth() / (float) drawable.getIntrinsicWidth();
                    float scaleY = (float) (viewHeight) / (float) drawable.getIntrinsicHeight();
                    float scale = Math.max(scaleX, scaleY);
                    int width = (int) Math.ceil(drawable.getIntrinsicWidth() * scale);
                    int height = (int) Math.ceil(drawable.getIntrinsicHeight() * scale);
                    int x = (getMeasuredWidth() - width) / 2;
                    int y = (viewHeight - height) / 2;
                    canvas.save();
                    canvas.clipRect(0, 0, width, getMeasuredHeight());
                    drawable.setBounds(x, y, x + width, y + height);
                }
                drawable.draw(canvas);
                canvas.restore();
            }
            if (a == 0 && oldBackgroundDrawable != null && themeAnimationValue >= 1.0f) {
                if (oldBackgroundGradientDisposable != null) {
                    oldBackgroundGradientDisposable.dispose();
                    oldBackgroundGradientDisposable = null;
                }
                oldBackgroundDrawable = null;
                invalidate();
            }
        }
        shadowDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
        shadowDrawable.draw(canvas);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (backgroundGradientDisposable != null) {
            backgroundGradientDisposable.dispose();
            backgroundGradientDisposable = null;
        }
        if (oldBackgroundGradientDisposable != null) {
            oldBackgroundGradientDisposable.dispose();
            oldBackgroundGradientDisposable = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return false;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {

    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
