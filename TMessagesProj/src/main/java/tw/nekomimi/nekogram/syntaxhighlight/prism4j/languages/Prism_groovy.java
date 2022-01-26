package tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.pattern;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.GrammarUtils;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public class Prism_groovy {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {

        final Prism4j.Grammar groovy = GrammarUtils.extend(
                GrammarUtils.require(prism4j, "clike"),
                "groovy",
                token("keyword", pattern(compile("\\b(?:as|def|in|abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|trait|transient|try|void|volatile|while)\\b"))),
                token("string",
                        pattern(
                                compile("(\"\"\"|''')[\\s\\S]*?\\1|(?:\\$\\/)(?:\\$\\/\\$|[\\s\\S])*?\\/\\$"), false, true
                        ),
                        pattern(
                                compile("([\"'\\/])(?:\\\\.|(?!\\1)[^\\\\\\r\\n])*\\1"), false, true
                        )
                ),
                token("number",
                        pattern(
                                compile("\\b(?:0b[01_]+|0x[\\da-f_]+(?:\\.[\\da-f_p\\-]+)?|[\\d_]+(?:\\.[\\d_]+)?(?:e[+-]?[\\d]+)?)[glidf]?\\b", CASE_INSENSITIVE)
                        )
                ),
                token("operator",
                        pattern(
                                compile("(^|[^.])(?:~|==?~?|\\?[.:]?|\\*(?:[.=]|\\*=?)?|\\.[@&]|\\.\\.<|\\.{1,2}(?!\\.)|-[-=>]?|\\+[+=]?|!=?|<(?:<=?|=>?)?|>(?:>>?=?|=)?|&[&=]?|\\|[|=]?|\\/=?|\\^=?|%=?)"),
                                true
                        )
                ),
                token("punctuation",
                        pattern(compile("\\.+|[{}\\[\\];(),:$]"))
                )
        );

        GrammarUtils.insertBeforeToken(groovy, "string",
                token("shebang", pattern(
                        compile("#!.+"),
                        false,
                        false,
                        "comment"
                ))
        );

        GrammarUtils.insertBeforeToken(groovy, "punctuation",
                token("spock-block", pattern(
                        compile("\\b(?:setup|given|when|then|and|cleanup|expect|where):")
                ))
        );

        GrammarUtils.insertBeforeToken(groovy, "function",
                token("annotation", pattern(
                        compile("(^|[^.])@\\w+"),
                        true,
                        false,
                        "punctuation"
                ))
        );

        // no string templates :(

        return groovy;
    }
}
