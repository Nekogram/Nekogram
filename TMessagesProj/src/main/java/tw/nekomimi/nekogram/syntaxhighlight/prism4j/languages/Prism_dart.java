package tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.pattern;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.GrammarUtils;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public class Prism_dart {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {

        final Prism4j.Grammar dart = GrammarUtils.extend(
                GrammarUtils.require(prism4j, "clike"),
                "dart",
                token("string",
                        pattern(compile("r?(\"\"\"|''')[\\s\\S]*?\\1"), false, true),
                        pattern(compile("r?(\"|')(?:\\\\.|(?!\\1)[^\\\\\\r\\n])*\\1"), false, true)
                ),
                token("keyword",
                        pattern(compile("\\b(?:async|sync|yield)\\*")),
                        pattern(compile("\\b(?:abstract|assert|async|await|break|case|catch|class|const|continue|default|deferred|do|dynamic|else|enum|export|external|extends|factory|final|finally|for|get|if|implements|import|in|library|new|null|operator|part|rethrow|return|set|static|super|switch|this|throw|try|typedef|var|void|while|with|yield)\\b"))
                ),
                token("operator", pattern(compile("\\bis!|\\b(?:as|is)\\b|\\+\\+|--|&&|\\|\\||<<=?|>>=?|~(?:\\/=?)?|[+\\-*\\/%&^|=!<>]=?|\\?")))
        );

        GrammarUtils.insertBeforeToken(dart, "function",
                token("metadata", pattern(compile("@\\w+"), false, false, "symbol"))
        );

        return dart;
    }
}
