package tw.nekomimi.nekogram.anim;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;

import androidx.annotation.Keep;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.MessageObject;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.CheckBoxBase;
import org.telegram.ui.Components.RadialProgress2;

import java.util.ArrayList;

public class PhotosTransitionOverlayView extends View {

    public ArrayList<RectF> initialPositions;
    public ArrayList<ChatMessageCell> messageCells;
    public ArrayList<ImageReceiver> imageReceivers;

    private float xProgress, yProgress, resizeProgress, iconTransitionProgress, timeAlpha;
    private final int[] loc = {0, 0};
    private final CheckBoxBase checkBox;
    private float checkOffsetX, checkOffsetY;
    private final Rect tmpRect = new Rect();

    private final View chatListView;
    private final ChatActivity chatActivity;
    private int additionalClipTop;

    public PhotosTransitionOverlayView(Context context, View chatListView, ChatActivity chatActivity) {
        super(context);
        checkBox = new CheckBoxBase(this);
        checkBox.setBackgroundType(7);
        checkBox.setColor(Theme.key_chat_attachCheckBoxBackground, Theme.key_chat_attachPhotoBackground, Theme.key_chat_attachCheckBoxCheck);
        checkBox.setChecked(true, false);
        this.chatListView = chatListView;
        this.chatActivity = chatActivity;
    }

    @Keep
    public float getXProgress() {
        return xProgress;
    }

    @Keep
    public void setXProgress(float xProgress) {
        this.xProgress = xProgress;
        invalidate();
    }

    @Keep
    public float getYProgress() {
        return yProgress;
    }

    @Keep
    public void setYProgress(float yProgress) {
        this.yProgress = yProgress;
        invalidate();
    }

    @Keep
    public float getResizeProgress() {
        return resizeProgress;
    }

    @Keep
    public void setResizeProgress(float resizeProgress) {
        this.resizeProgress = resizeProgress;
        invalidate();
    }

    @Keep
    public float getIconTransitionProgress() {
        return iconTransitionProgress;
    }

    @Keep
    public void setIconTransitionProgress(float iconTransitionProgress) {
        this.iconTransitionProgress = iconTransitionProgress;
        invalidate();
    }

    public void setCheckOffset(float x, float y) {
        checkOffsetX = x;
        checkOffsetY = y;
    }

    @Keep
    public float getTimeAlpha() {
        return timeAlpha;
    }

    @Keep
    public void setTimeAlpha(float timeAlpha) {
        this.timeAlpha = timeAlpha;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (messageCells.size() == 0)
            return;

        chatListView.getGlobalVisibleRect(tmpRect);
        tmpRect.top = Math.max(tmpRect.top, chatActivity.getActionBar().getHeight() + chatActivity.getPinnedMessageViewHeight());
        canvas.save();
        canvas.clipRect(0, Math.round(tmpRect.top * yProgress), getWidth(), getHeight());

        for (int i = 0; i < imageReceivers.size(); i++) {
            ImageReceiver receiver = imageReceivers.get(i);
            RectF initial = initialPositions.get(i);
            ChatMessageCell cell = messageCells.get(i);

            if (receiver == null || cell == null || initial == null)
                continue;

            float fromCX = initial.centerX();
            float fromCY = initial.centerY();
            float fromW = initial.width();
            float fromH = initial.height();

            cell.getLocationOnScreen(loc);
            float toCX = loc[0] + cell.getPhotoImage().getCenterX();
            float toCY = loc[1] + cell.getPhotoImage().getCenterY();
            float toW = cell.getPhotoImage().getImageWidth();
            float toH = cell.getPhotoImage().getImageHeight();

            float cx = fromCX * (1f - xProgress) + toCX * xProgress;
            float cy = fromCY * (1f - yProgress) + toCY * yProgress;
            float w = fromW * (1f - resizeProgress) + toW * resizeProgress;
            float h = fromH * (1f - resizeProgress) + toH * resizeProgress;

            int[] radius = cell.getPhotoImage().getRoundRadius();

            receiver.setImageCoords(cx - w / 2f, cy - h / 2f, w, h);
            receiver.setRoundRadius(Math.round(radius[0] * resizeProgress), Math.round(radius[1] * resizeProgress), Math.round(radius[2] * resizeProgress), Math.round(radius[3] * resizeProgress));
            receiver.draw(canvas);

            float checkCX = cx + (checkOffsetX / fromW * w) * (1f - iconTransitionProgress);
            float checkCY = cy + (checkOffsetY / fromH * h) * (1f - iconTransitionProgress);
            float scale1 = .576923077f + (1f - .576923077f) * iconTransitionProgress;
            float scale2 = 1f + .733333333f * iconTransitionProgress;

            RadialProgress2 cellProgress = cell.getRadialProgress();
            canvas.save();
            RectF progressRect = cellProgress.getProgressRect();
            canvas.translate(-progressRect.centerX() + checkCX, -progressRect.centerY() + checkCY);
            canvas.scale(scale1, scale1, progressRect.centerX(), progressRect.centerY());
            cellProgress.setOverrideAlpha(iconTransitionProgress);
            cellProgress.draw(canvas);
            cellProgress.setOverrideAlpha(1f);
            canvas.restore();

            canvas.save();
            canvas.scale(scale2, scale2, checkCX, checkCY);
            int cbSize = AndroidUtilities.dp(26);
            checkBox.setBounds((int) checkCX - cbSize / 2, (int) checkCY - cbSize / 2, cbSize, cbSize);
            checkBox.setNum(i);
            canvas.saveLayerAlpha(checkCX - cbSize / 2f, checkCY - cbSize / 2f, checkCX + cbSize / 2f, checkCY + cbSize / 2f, Math.round((1f - iconTransitionProgress) * 255f), Canvas.ALL_SAVE_FLAG);
            checkBox.draw(canvas);
            canvas.restore();
            canvas.restore();
        }

        ChatMessageCell lastCell = messageCells.get(messageCells.size() - 1);
        if (lastCell != null) {
            MessageObject.GroupedMessages group = lastCell.getCurrentMessagesGroup();
            if ((group != null && !group.hasCaption) || (group == null && !lastCell.hasCaptionLayout())) {
                canvas.save();
                lastCell.getLocationOnScreen(loc);
                canvas.translate(loc[0], loc[1]);
                lastCell.getTransitionParams().animateChangeProgress = 1f;
                lastCell.drawTime(canvas, timeAlpha, false);
                lastCell.getTransitionParams().animateChangeProgress = 0f;
                canvas.restore();
            }
        }

        canvas.restore();
    }
}
