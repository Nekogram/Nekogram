package tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages;

import static java.util.regex.Pattern.MULTILINE;
import static java.util.regex.Pattern.compile;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.grammar;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.pattern;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public class Prism_makefile {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("makefile",
                token("comment", pattern(
                        compile("(^|[^\\\\])#(?:\\\\(?:\\r\\n|[\\s\\S])|[^\\\\\\r\\n])*"),
                        true
                )),
                token("string", pattern(
                        compile("([\"'])(?:\\\\(?:\\r\\n|[\\s\\S])|(?!\\1)[^\\\\\\r\\n])*\\1"),
                        false,
                        true
                )),
                token("builtin", pattern(compile("\\.[A-Z][^:#=\\s]+(?=\\s*:(?!=))"))),
                token("symbol", pattern(
                        compile("^[^:=\\r\\n]+(?=\\s*:(?!=))", MULTILINE),
                        false,
                        false,
                        null,
                        grammar("inside",
                                token("variable", pattern(compile("\\$+(?:[^(){}:#=\\s]+|(?=[({]))")))
                        )
                )),
                token("variable", pattern(compile("\\$+(?:[^(){}:#=\\s]+|\\([@*%<^+?][DF]\\)|(?=[({]))"))),
                token("keyword",
                        pattern(compile("-include\\b|\\b(?:define|else|endef|endif|export|ifn?def|ifn?eq|include|override|private|sinclude|undefine|unexport|vpath)\\b")),
                        pattern(
                                compile("(\\()(?:addsuffix|abspath|and|basename|call|dir|error|eval|file|filter(?:-out)?|findstring|firstword|flavor|foreach|guile|if|info|join|lastword|load|notdir|or|origin|patsubst|realpath|shell|sort|strip|subst|suffix|value|warning|wildcard|word(?:s|list)?)(?=[ \\t])"),
                                true
                        )
                ),
                token("operator", pattern(compile("(?:::|[?:+!])?=|[|@]"))),
                token("punctuation", pattern(compile("[:;(){}]")))
        );
    }
}
