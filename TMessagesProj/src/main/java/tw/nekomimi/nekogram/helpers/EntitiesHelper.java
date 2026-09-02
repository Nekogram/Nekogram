package tw.nekomimi.nekogram.helpers;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import org.telegram.messenger.CodeHighlighting;
import org.telegram.messenger.LinkifyPort;
import org.telegram.messenger.MediaDataController;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.URLSpanReplacement;

import java.util.ArrayList;
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
                var textStyleSpans = spannable.getSpans(start, end, TextStyleSpan.class);
                for (var textStyleSpan : textStyleSpans) {
                    if (!textStyleSpan.isMono()) {
                        continue;
                    }
                    int spanStart = spannable.getSpanStart(textStyleSpan);
                    int spanEnd = spannable.getSpanEnd(textStyleSpan);
                    if (spanStart < start + length || spanEnd > end - length) {
                        continue find;
                    }
                }
                var codeHighlightingSpans = spannable.getSpans(start, end, CodeHighlighting.Span.class);
                for (var codeHighlightingSpan : codeHighlightingSpans) {
                    int spanStart = spannable.getSpanStart(codeHighlightingSpan);
                    int spanEnd = spannable.getSpanEnd(codeHighlightingSpan);
                    if (spanStart < start + length || spanEnd > end - length) {
                        continue find;
                    }
                }

                var destination = new SpannableStringBuilder(spannable.subSequence(m.start(i == 0 ? 2 : 1), m.end(i == 0 ? 2 : 1)));
                if (destination.length() > 0) {
                    if (i == 0) {
                        if (destination.charAt(destination.length() - 1) == '\n') {
                            destination = (SpannableStringBuilder) destination.subSequence(0, destination.length() - 1);
                        }
                        destination.setSpan(new CodeHighlighting.Span(true, 0, null, m.group(1), destination.toString()), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (i < 8) {
                        var run = new TextStyleSpan.TextStyleRun();
                        switch (i) {
                            case 1:
                            case 2:
                            case 3:
                                run.flags |= TextStyleSpan.FLAG_STYLE_MONO;
                                break;
                            case 4:
                                run.flags |= TextStyleSpan.FLAG_STYLE_BOLD;
                                break;
                            case 5:
                                run.flags |= TextStyleSpan.FLAG_STYLE_ITALIC;
                                break;
                            case 6:
                                run.flags |= TextStyleSpan.FLAG_STYLE_STRIKE;
                                break;
                            case 7:
                                run.flags |= TextStyleSpan.FLAG_STYLE_SPOILER;
                                break;
                        }
                        MediaDataController.addStyleToText(new TextStyleSpan(run), 0, destination.length(), destination, true);
                    } else {
                        destination.setSpan(new URLSpanReplacement(m.group(2)), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
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

    public static CharSequence warpInLanguageSpan(CharSequence text, String language) {
        var spannable = new SpannableString(text);
        var codeHighlightingSpan = new CodeHighlighting.Span(false, 0, null, language, null);
        spannable.setSpan(codeHighlightingSpan, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
}
