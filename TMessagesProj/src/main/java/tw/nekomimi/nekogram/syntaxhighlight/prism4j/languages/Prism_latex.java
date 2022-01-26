package tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.grammar;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.pattern;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public class Prism_latex {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {

        final Pattern funcPattern = compile("\\\\(?:[^a-z()\\[\\]]|[a-z*]+)", CASE_INSENSITIVE);

        final Prism4j.Grammar insideEqu = grammar("inside",
                token("equation-command", pattern(funcPattern, false, false, "regex"))
        );

        return grammar("latex",
                token("comment", pattern(compile("%.*", MULTILINE))),
                token("cdata", pattern(
                        compile("(\\\\begin\\{((?:verbatim|lstlisting)\\*?)\\})[\\s\\S]*?(?=\\\\end\\{\\2\\})"),
                        true
                        )
                ),
                token("equation",
                        pattern(
                                compile("\\$(?:\\\\[\\s\\S]|[^\\\\$])*\\$|\\\\\\([\\s\\S]*?\\\\\\)|\\\\\\[[\\s\\S]*?\\\\\\]"),
                                false,
                                false,
                                "string",
                                insideEqu
                        ),
                        pattern(
                                compile("(\\\\begin\\{((?:equation|math|eqnarray|align|multline|gather)\\*?)\\})[\\s\\S]*?(?=\\\\end\\{\\2\\})"),
                                true,
                                false,
                                "string",
                                insideEqu
                        )
                ),
                token("keyword", pattern(
                        compile("(\\\\(?:begin|end|ref|cite|label|usepackage|documentclass)(?:\\[[^\\]]+\\])?\\{)[^}]+(?=\\})"),
                        true
                )),
                token("url", pattern(
                        compile("(\\\\url\\{)[^}]+(?=\\})"),
                        true
                )),
                token("headline", pattern(
                        compile("(\\\\(?:part|chapter|section|subsection|frametitle|subsubsection|paragraph|subparagraph|subsubparagraph|subsubsubparagraph)\\*?(?:\\[[^\\]]+\\])?\\{)[^}]+(?=\\}(?:\\[[^\\]]+\\])?)"),
                        true,
                        false,
                        "class-name"
                )),
                token("function", pattern(
                        funcPattern,
                        false,
                        false,
                        "selector"
                )),
                token("punctuation", pattern(compile("[\\[\\]{}&]")))
        );
    }
}
