package tw.nekomimi.nekogram.syntaxhighlight;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.GrammarLocator;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_brainfuck;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_c;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_clike;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_clojure;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_cpp;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_csharp;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_css;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_css_extras;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_dart;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_git;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_go;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_groovy;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_java;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_javascript;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_json;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_kotlin;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_latex;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_makefile;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_markdown;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_markup;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_python;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_scala;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_sql;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_swift;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages.Prism_yaml;

public class Prism4jGrammarLocator implements GrammarLocator {

    @SuppressWarnings("ConstantConditions")
    private static final Prism4j.Grammar NULL =
            new Prism4j.Grammar() {
                @NonNull
                @Override
                public String name() {
                    return null;
                }

                @NonNull
                @Override
                public List<Prism4j.Token> tokens() {
                    return null;
                }
            };

    private final Map<String, Prism4j.Grammar> cache = new HashMap<>(3);

    @Nullable
    @Override
    public Prism4j.Grammar grammar(@NonNull Prism4j prism4j, @NonNull String language) {

        final String name = realLanguageName(language);

        Prism4j.Grammar grammar = cache.get(name);
        if (grammar != null) {
            if (NULL == grammar) {
                grammar = null;
            }
            return grammar;
        }

        grammar = obtainGrammar(prism4j, name);
        if (grammar == null) {
            cache.put(name, NULL);
        } else {
            cache.put(name, grammar);
            triggerModify(prism4j, name);
        }

        return grammar;
    }

    @NonNull
    protected String realLanguageName(@NonNull String name) {
        final String out;
        switch (name) {
            case "js":
                out = "javascript";
                break;
            case "xml":
            case "html":
            case "mathml":
            case "svg":
                out = "markup";
                break;
            case "dotnet":
                out = "csharp";
                break;
            case "jsonp":
                out = "json";
                break;
            default:
                out = name;
        }
        return out;
    }

    @Nullable
    protected Prism4j.Grammar obtainGrammar(@NonNull Prism4j prism4j, @NonNull String name) {
        final Prism4j.Grammar grammar;
        switch (name) {
            case "brainfuck":
                grammar = Prism_brainfuck.create(prism4j);
                break;
            case "c":
                grammar = Prism_c.create(prism4j);
                break;
            case "clike":
                grammar = Prism_clike.create(prism4j);
                break;
            case "clojure":
                grammar = Prism_clojure.create(prism4j);
                break;
            case "cpp":
                grammar = Prism_cpp.create(prism4j);
                break;
            case "csharp":
                grammar = Prism_csharp.create(prism4j);
                break;
            case "css":
                grammar = Prism_css.create(prism4j);
                break;
            case "css-extras":
                grammar = Prism_css_extras.create(prism4j);
                break;
            case "dart":
                grammar = Prism_dart.create(prism4j);
                break;
            case "git":
                grammar = Prism_git.create(prism4j);
                break;
            case "go":
                grammar = Prism_go.create(prism4j);
                break;
            case "groovy":
                grammar = Prism_groovy.create(prism4j);
                break;
            case "java":
                grammar = Prism_java.create(prism4j);
                break;
            case "javascript":
                grammar = Prism_javascript.create(prism4j);
                break;
            case "json":
                grammar = Prism_json.create(prism4j);
                break;
            case "kotlin":
                grammar = Prism_kotlin.create(prism4j);
                break;
            case "latex":
                grammar = Prism_latex.create(prism4j);
                break;
            case "makefile":
                grammar = Prism_makefile.create(prism4j);
                break;
            case "markdown":
                grammar = Prism_markdown.create(prism4j);
                break;
            case "markup":
                grammar = Prism_markup.create(prism4j);
                break;
            case "python":
                grammar = Prism_python.create(prism4j);
                break;
            case "scala":
                grammar = Prism_scala.create(prism4j);
                break;
            case "sql":
                grammar = Prism_sql.create(prism4j);
                break;
            case "swift":
                grammar = Prism_swift.create(prism4j);
                break;
            case "yaml":
                grammar = Prism_yaml.create(prism4j);
                break;
            default:
                grammar = null;
        }
        return grammar;
    }

    protected void triggerModify(@NonNull Prism4j prism4j, @NonNull String name) {
        switch (name) {
            case "markup":
                prism4j.grammar("css");
                prism4j.grammar("javascript");
                break;
            case "css":
                prism4j.grammar("css-extras");
                break;
        }
    }

    @Override
    @NonNull
    public Set<String> languages() {
        final Set<String> set = new HashSet<>(25);
        set.add("brainfuck");
        set.add("c");
        set.add("clike");
        set.add("clojure");
        set.add("cpp");
        set.add("csharp");
        set.add("css");
        set.add("css-extras");
        set.add("dart");
        set.add("git");
        set.add("go");
        set.add("groovy");
        set.add("java");
        set.add("javascript");
        set.add("json");
        set.add("kotlin");
        set.add("latex");
        set.add("makefile");
        set.add("markdown");
        set.add("markup");
        set.add("python");
        set.add("scala");
        set.add("sql");
        set.add("swift");
        set.add("yaml");
        return set;
    }
}
