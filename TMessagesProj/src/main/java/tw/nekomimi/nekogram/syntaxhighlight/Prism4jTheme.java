package tw.nekomimi.nekogram.syntaxhighlight;

import android.text.Spannable;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public interface Prism4jTheme {

    @ColorInt
    int textColor();

    void apply(
            @NonNull String language,
            @NonNull Prism4j.Syntax syntax,
            @NonNull Spannable spannable,
            int start,
            int end
    );
}
