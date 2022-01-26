package tw.nekomimi.nekogram.syntaxhighlight.prism4j;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class Cloner {

    @NonNull
    abstract Prism4j.Grammar clone(@NonNull Prism4j.Grammar grammar);

    @NonNull
    abstract Prism4j.Token clone(@NonNull Prism4j.Token token);

    @NonNull
    abstract Prism4j.Pattern clone(@NonNull Prism4j.Pattern pattern);

    @NonNull
    static Cloner create() {
        return new Impl();
    }

    static class Impl extends Cloner {

        interface Context {

            @Nullable
            Prism4j.Grammar grammar(@NonNull Prism4j.Grammar origin);

            @Nullable
            Prism4j.Token token(@NonNull Prism4j.Token origin);

            @Nullable
            Prism4j.Pattern pattern(@NonNull Prism4j.Pattern origin);


            void save(@NonNull Prism4j.Grammar origin, @NonNull Prism4j.Grammar clone);

            void save(@NonNull Prism4j.Token origin, @NonNull Prism4j.Token clone);

            void save(@NonNull Prism4j.Pattern origin, @NonNull Prism4j.Pattern clone);
        }

        @NonNull
        @Override
        Prism4j.Grammar clone(@NonNull Prism4j.Grammar grammar) {
            return clone(new ContextImpl(), grammar);
        }

        @NonNull
        @Override
        Prism4j.Token clone(@NonNull Prism4j.Token token) {
            return clone(new ContextImpl(), token);
        }

        @NonNull
        @Override
        Prism4j.Pattern clone(@NonNull Prism4j.Pattern pattern) {
            return clone(new ContextImpl(), pattern);
        }

        @NonNull
        private Prism4j.Grammar clone(@NonNull Context context, @NonNull Prism4j.Grammar grammar) {

            Prism4j.Grammar clone = context.grammar(grammar);
            if (clone != null) {
                return clone;
            }

            final List<Prism4j.Token> tokens = grammar.tokens();
            final List<Prism4j.Token> out = new ArrayList<>(tokens.size());

            clone = new GrammarImpl(grammar.name(), out);
            context.save(grammar, clone);

            for (Prism4j.Token token : tokens) {
                out.add(clone(context, token));
            }

            return clone;
        }

        @NonNull
        private Prism4j.Token clone(@NonNull Context context, @NonNull Prism4j.Token token) {

            Prism4j.Token clone = context.token(token);
            if (clone != null) {
                return clone;
            }

            final List<Prism4j.Pattern> patterns = token.patterns();
            final List<Prism4j.Pattern> out = new ArrayList<>(patterns.size());

            clone = new TokenImpl(token.name(), out);
            context.save(token, clone);

            for (Prism4j.Pattern pattern : patterns) {
                out.add(clone(context, pattern));
            }

            return clone;
        }

        @NonNull
        private Prism4j.Pattern clone(@NonNull Context context, @NonNull Prism4j.Pattern pattern) {

            Prism4j.Pattern clone = context.pattern(pattern);
            if (clone != null) {
                return clone;
            }

            final Prism4j.Grammar inside = pattern.inside();

            clone = new PatternImpl(
                    pattern.regex(),
                    pattern.lookbehind(),
                    pattern.greedy(),
                    pattern.alias(),
                    inside != null ? clone(context, inside) : null
            );

            context.save(pattern, clone);

            return clone;
        }

        private static class ContextImpl implements Context {

            private final Map<Integer, Object> cache = new HashMap<>(3);

            @Nullable
            @Override
            public Prism4j.Grammar grammar(@NonNull Prism4j.Grammar origin) {
                return (Prism4j.Grammar) cache.get(key(origin));
            }

            @Nullable
            @Override
            public Prism4j.Token token(@NonNull Prism4j.Token origin) {
                return (Prism4j.Token) cache.get(key(origin));
            }

            @Nullable
            @Override
            public Prism4j.Pattern pattern(@NonNull Prism4j.Pattern origin) {
                return (Prism4j.Pattern) cache.get(key(origin));
            }

            @Override
            public void save(@NonNull Prism4j.Grammar origin, @NonNull Prism4j.Grammar clone) {
                cache.put(key(origin), clone);
            }

            @Override
            public void save(@NonNull Prism4j.Token origin, @NonNull Prism4j.Token clone) {
                cache.put(key(origin), clone);
            }

            @Override
            public void save(@NonNull Prism4j.Pattern origin, @NonNull Prism4j.Pattern clone) {
                cache.put(key(origin), clone);
            }

            private static int key(@NonNull Object o) {
                return System.identityHashCode(o);
            }
        }
    }
}
