package tw.nekomimi.nekogram.helpers;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.LocaleSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;

import org.telegram.messenger.LinkifyPort;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.NekoConfig;

public class EntitiesHelper {
    private static final Pattern[] PATTERNS = new Pattern[]{
            Pattern.compile("^`{3}(.*?)[\\n\\r](.*?[\\n\\r]?)`{3}", Pattern.MULTILINE | Pattern.DOTALL), // pre
            Pattern.compile("^`{3}[\\n\\r]?(.*?)[\\n\\r]?`{3}", Pattern.MULTILINE | Pattern.DOTALL), // pre
            Pattern.compile("[`]{3}([^`]+)[`]{3}"), // pre
            Pattern.compile("[`]([^`\\n]+)[`]"), // code
            Pattern.compile("[*]{2}([^*\\n]+)[*]{2}"), // bold
            Pattern.compile("[_]{2}([^_\\n]+)[_]{2}"), // italic
            Pattern.compile("[~]{2}([^~\\n]+)[~]{2}"), // strike
            Pattern.compile("[|]{2}([^|\\n]+)[|]{2}"), // spoiler
            Pattern.compile("\\[([^]]+?)]\\(" + LinkifyPort.WEB_URL_REGEX + "\\)")}; // link

    public static CharSequence parseMarkdown(CharSequence text) {
        var message = new CharSequence[]{text};
        parseMarkdown(message, true);
        return message[0];
    }

    // TODO: refactor to use tg spans
    public static void parseMarkdown(CharSequence[] message, boolean allowStrike) {
        var spannable = message[0] instanceof Spannable ? (Spannable) message[0] : Spannable.Factory.getInstance().newSpannable(message[0]);
        for (int i = 0; i < PATTERNS.length; i++) {
            if (!allowStrike && i == 6 || !NekoConfig.markdownParseLinks && i == 8) {
                continue;
            }
            var m = PATTERNS[i].matcher(spannable);
            var sources = new ArrayList<String>();
            var destinations = new ArrayList<CharSequence>();
            find:
            while (m.find()) {
                var start = m.start();
                var end = m.end();
                var length = i < 3 ? 3 : i > 3 && i != 8 ? 2 : 1;
                var typefaceSpans = spannable.getSpans(start, end, TypefaceSpan.class);
                for (var typefaceSpan : typefaceSpans) {
                    if (!"monospace".equals(typefaceSpan.getFamily())) {
                        continue;
                    }
                    int spanStart = spannable.getSpanStart(typefaceSpan);
                    int spanEnd = spannable.getSpanEnd(typefaceSpan);
                    if (spanStart < start + length || spanEnd > end - length) {
                        continue find;
                    }
                }

                var destination = new SpannableStringBuilder(spannable.subSequence(m.start(i == 0 ? 2 : 1), m.end(i == 0 ? 2 : 1)));
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        if (i != 3) {
                            destination.setSpan(new LocaleSpan(languageToLocale(i == 0 ? m.group(1) : "")), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        destination.setSpan(new TypefaceSpan("monospace"), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 4:
                        destination.setSpan(new StyleSpan(Typeface.BOLD), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 5:
                        destination.setSpan(new StyleSpan(Typeface.ITALIC), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 6:
                        destination.setSpan(new StrikethroughSpan(), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 7:
                        destination.setSpan(new BackgroundColorSpan(Theme.getColor(Theme.key_chats_archivePullDownBackground)), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 8:
                        destination.setSpan(new URLSpan(m.group(2)), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                }
                sources.add(m.group(0));
                destinations.add(destination);
            }
            for (int j = 0; j < sources.size(); j++) {
                spannable = (Spannable) TextUtils.replace(spannable, new String[]{sources.get(j)}, new CharSequence[]{destinations.get(j)});
            }
        }
        message[0] = spannable;
    }

    private static Locale languageToLocale(String language) {
        return Locale.forLanguageTag("ng-SH-x-lvariant-lang-" + (TextUtils.isEmpty(language) ? "none" : language));
    }
}
