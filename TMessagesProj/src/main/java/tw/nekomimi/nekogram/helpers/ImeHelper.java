package tw.nekomimi.nekogram.helpers;

import android.content.ClipData;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;

import androidx.core.os.BundleCompat;
import androidx.core.view.ContentInfoCompat;
import androidx.core.view.ViewCompat;

public class ImeHelper {
    private static final String EXTRA_ENABLE_CHINESE_INLINE = "enable_chinese_inline";

    private static final String EXTRA_SOGOU_EXPRESSION = "SOGOU_EXPRESSION";
    private static final String EXTRA_SOGOU_EXPRESSION_WEBP = "SOGOU_EXPRESSION_WEBP";
    private static final String EXTRA_SUPPORT_SOGOU_EXPRESSION = "SUPPORT_SOGOU_EXPRESSION";
    private static final String ACTION_SOGOU_EXPRESSION_COMMIT = "com.sogou.inputmethod.exp.commit";
    private static final String KEY_SOGOU_EXPRESSION_URI = "EXP_PATH_URI";

    public static void enableChineseInline(EditText editText) {
        var extras = editText.getInputExtras(true);
        extras.putBoolean(EXTRA_ENABLE_CHINESE_INLINE, true);
    }

    public static void enableSogouExpression(EditText editText) {
        var extras = editText.getInputExtras(true);
        extras.putInt(EXTRA_SOGOU_EXPRESSION, 1);
        extras.putInt(EXTRA_SOGOU_EXPRESSION_WEBP, 1);
        extras.putInt(EXTRA_SUPPORT_SOGOU_EXPRESSION, 1);
    }

    public static boolean handlePrivateIMECommand(EditText editText, String action, Bundle data) {
        if (ACTION_SOGOU_EXPRESSION_COMMIT.equals(action) && data != null) {
            var uri = BundleCompat.getParcelable(data, KEY_SOGOU_EXPRESSION_URI, Uri.class);
            if (uri == null) {
                return false;
            }
            var clipData = ClipData.newUri(editText.getContext().getContentResolver(), "Sogou Expression", uri);
            var contentInfo = new ContentInfoCompat.Builder(clipData, ContentInfoCompat.SOURCE_INPUT_METHOD).build();
            ViewCompat.performReceiveContent(editText, contentInfo);
            return true;
        }
        return false;
    }
}
