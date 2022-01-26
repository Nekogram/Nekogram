package tw.nekomimi.nekogram.syntaxhighlight;

import android.text.Spannable;

import androidx.annotation.NonNull;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.AbsVisitor;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

class Prism4jSyntaxVisitor extends AbsVisitor {

    private final String language;
    private final Prism4jTheme theme;
    private final Spannable spannable;

    private int currentPos;

    Prism4jSyntaxVisitor(
            @NonNull String language,
            @NonNull Prism4jTheme theme,
            @NonNull Spannable spannable,
            int start) {
        this.language = language;
        this.theme = theme;
        this.spannable = spannable;

        currentPos = start;
    }

    @Override
    protected void visitText(@NonNull Prism4j.Text text) {
        currentPos += text.textLength();
    }

    @Override
    protected void visitSyntax(@NonNull Prism4j.Syntax syntax) {
        final int start = currentPos;
        visit(syntax.children());
        final int end = currentPos;

        if (end != start) {
            theme.apply(language, syntax, spannable, start, end);
        }
    }
}
