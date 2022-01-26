package tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.grammar;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.pattern;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public abstract class Prism_markup {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        final Prism4j.Token entity = token("entity", pattern(compile("&#?[\\da-z]{1,8};", Pattern.CASE_INSENSITIVE)));
        return grammar(
                "markup",
                token("comment", pattern(compile("<!--[\\s\\S]*?-->"))),
                token("prolog", pattern(compile("<\\?[\\s\\S]+?\\?>"))),
                token("doctype", pattern(compile("<!DOCTYPE[\\s\\S]+?>", Pattern.CASE_INSENSITIVE))),
                token("cdata", pattern(compile("<!\\[CDATA\\[[\\s\\S]*?]]>", Pattern.CASE_INSENSITIVE))),
                token(
                        "tag",
                        pattern(
                                compile("<\\/?(?!\\d)[^\\s>\\/=$<%]+(?:\\s+[^\\s>\\/=]+(?:=(?:(\"|')(?:\\\\[\\s\\S]|(?!\\1)[^\\\\])*\\1|[^\\s'\">=]+))?)*\\s*\\/?>", Pattern.CASE_INSENSITIVE),
                                false,
                                true,
                                null,
                                grammar(
                                        "inside",
                                        token(
                                                "tag",
                                                pattern(
                                                        compile("^<\\/?[^\\s>\\/]+", Pattern.CASE_INSENSITIVE),
                                                        false,
                                                        false,
                                                        null,
                                                        grammar(
                                                                "inside",
                                                                token("punctuation", pattern(compile("^<\\/?"))),
                                                                token("namespace", pattern(compile("^[^\\s>\\/:]+:")))
                                                        )
                                                )
                                        ),
                                        token(
                                                "attr-value",
                                                pattern(
                                                        compile("=(?:(\"|')(?:\\\\[\\s\\S]|(?!\\1)[^\\\\])*\\1|[^\\s'\">=]+)", Pattern.CASE_INSENSITIVE),
                                                        false,
                                                        false,
                                                        null,
                                                        grammar(
                                                                "inside",
                                                                token(
                                                                        "punctuation",
                                                                        pattern(compile("^=")),
                                                                        pattern(compile("(^|[^\\\\])[\"']"), true)
                                                                ),
                                                                entity
                                                        )
                                                )
                                        ),
                                        token("punctuation", pattern(compile("\\/?>"))),
                                        token(
                                                "attr-name",
                                                pattern(
                                                        compile("[^\\s>\\/]+"),
                                                        false,
                                                        false,
                                                        null,
                                                        grammar(
                                                                "inside",
                                                                token("namespace", pattern(compile("^[^\\s>\\/:]+:")))
                                                        )
                                                )
                                        )
                                )
                        )
                ),
                entity
        );
    }

    private Prism_markup() {
    }
}
