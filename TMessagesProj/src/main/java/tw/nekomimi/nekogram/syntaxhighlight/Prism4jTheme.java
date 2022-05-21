package tw.nekomimi.nekogram.syntaxhighlight;

import android.text.Spannable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import io.noties.prism4j.Syntax;

public interface Prism4jTheme {

    @ColorInt
    int textColor();

    void apply(
            @NonNull String language,
            @NonNull Syntax syntax,
            @NonNull Spannable spannable,
            int start,
            int end
    );
}
