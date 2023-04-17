package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.widget.FrameLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;

public class BuyMeACoffeeCell extends FrameLayout {

    private final ImageView imageView;

    public BuyMeACoffeeCell(Context context) {
        super(context);

        imageView = new ImageView(context);
        imageView.setImageResource(R.drawable.ic_bmc_button);
        imageView.setBackgroundColor(0xFF5F7FFF);
        addView(imageView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        imageView.setAlpha(enabled ? 1.0f : 0.5f);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(50), MeasureSpec.EXACTLY));
    }
}