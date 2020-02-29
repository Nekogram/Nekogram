package tw.nekomimi.nekogram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class UnreadCountBadgeView extends View {

    public String countString;
    StaticLayout countLayout;
    int countWidth;
    private RectF rect = new RectF();

    public UnreadCountBadgeView(Context context, String countString) {
        super(context);
        this.countString = countString;
        countWidth = Math.max(AndroidUtilities.dp(12), (int) Math.ceil(Theme.dialogs_countTextPaint.measureText(countString)));
        countLayout = new StaticLayout(countString, Theme.dialogs_countTextPaint, countWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = Theme.dialogs_countPaint;
        paint.setAlpha(255);
        Theme.dialogs_countTextPaint.setAlpha(255);
        int countLeft = AndroidUtilities.dp(5.5f);
        int countTop = AndroidUtilities.dp(12.5f);
        int x = 0;
        rect.set(x, countTop, x + countWidth + AndroidUtilities.dp(11), countTop + AndroidUtilities.dp(23));
        canvas.drawRoundRect(rect, 11.5f * AndroidUtilities.density, 11.5f * AndroidUtilities.density, paint);

        if (countLayout != null) {
            canvas.save();
            canvas.translate(countLeft, countTop + AndroidUtilities.dp(4));
            countLayout.draw(canvas);
            canvas.restore();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(
                countWidth + AndroidUtilities.dp(29),
                AndroidUtilities.dp(48)
        );
    }

}
