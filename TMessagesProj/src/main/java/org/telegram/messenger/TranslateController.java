package org.telegram.messenger;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.Collator;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.util.Pair;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.Nullable;

//import com.google.mlkit.common.model.RemoteModelManager;
//import com.google.mlkit.common.sdkinternal.MlKitContext;
//import com.google.mlkit.common.sdkinternal.SharedPrefManager;
//import com.google.mlkit.nl.translate.TranslateLanguage;
//import com.google.mlkit.nl.translate.TranslateRemoteModel;
//import com.google.mlkit.nl.translate.Translation;
//import com.google.mlkit.nl.translate.Translator;
//import com.google.mlkit.nl.translate.TranslatorOptions;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.InputSerializedData;
import org.telegram.tgnet.OutputSerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.tgnet.tl.TL_stories;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.RestrictedLanguagesSelectActivity;
import org.telegram.ui.Stories.DarkThemeResourceProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import app.nekogram.translator.Http429Exception;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.MessageHelper;
import tw.nekomimi.nekogram.translator.Translator;

public class TranslateController extends BaseController {

    public static final String UNKNOWN_LANGUAGE = "und";

    private static final int REQUIRED_TOTAL_MESSAGES_CHECKED = 6;
    private static final int REQUIRED_TOTAL_MESSAGES_CHECKED_AUTOTRANSLATE = 2;
    private static final float REQUIRED_PERCENTAGE_MESSAGES_TRANSLATABLE = .60F;
    private static final float REQUIRED_MIN_MESSAGES_TRANSLATABLE_AUTOTRANSLATE = 2;
    private static final float REQUIRED_MIN_PERCENTAGE_MESSAGES_UNKNOWN = .65F;
    private static final float REQUIRED_MIN_PERCENTAGE_MESSAGES_UNKNOWN_AUTOTRANSLATE = .80F;

    private static final int MAX_SYMBOLS_PER_REQUEST = 25000;
    private static final int MAX_MESSAGES_PER_REQUEST = 20;
    private static final int GROUPING_TRANSLATIONS_TIMEOUT = 80;

    private final LongSparseArray<Boolean> translatingDialogs = new LongSparseArray<>();
    private final Set<Long> translatableDialogs = new HashSet<>();
    private final HashMap<Long, TranslatableDecision> translatableDialogMessages = new HashMap<>();
    private final HashMap<Long, String> translateDialogLanguage = new HashMap<>();
    private final HashMap<Long, String> detectedDialogLanguage = new HashMap<>();
    private final HashMap<Long, HashMap<Integer, MessageObject>> keptReplyMessageObjects = new HashMap<>();
    private final Set<Long> hideTranslateDialogs = new HashSet<>();

    static class TranslatableDecision {
        Set<Integer> certainlyTranslatable = new HashSet<>();
        Set<Integer> unknown = new HashSet<>();
        Set<Integer> certainlyNotTranslatable = new HashSet<>();
    }

    private MessagesController messagesController;

    public TranslateController(MessagesController messagesController) {
        super(messagesController.currentAccount);
        this.messagesController = messagesController;

        AndroidUtilities.runOnUIThread(this::loadTranslatingDialogsCached, 150);
    }

    public boolean isFeatureAvailable() {
        return NekoConfig.autoTranslate && NekoConfig.transType != NekoConfig.TRANS_TYPE_EXTERNAL;
    }

    public boolean isFeatureAvailable(long dialogId) {
        return isFeatureAvailable();
    }

    private Boolean chatTranslateEnabled;
    private Boolean contextTranslateEnabled;

    public boolean isChatTranslateEnabled() {
        if (!getMessagesController().isTranslationsAutoEnabled()) {
            return false;
        }
        if (chatTranslateEnabled == null) {
            chatTranslateEnabled = messagesController.getMainSettings().getBoolean("translate_chat_button", true);
        }
        return chatTranslateEnabled;
    }

    public boolean isContextTranslateEnabled() {
        /*if (!getMessagesController().isTranslationsManualEnabled()) {
            return false;
        }
        if (contextTranslateEnabled == null) {
            contextTranslateEnabled = messagesController.getMainSettings().getBoolean("translate_button", MessagesController.getGlobalMainSettings().getBoolean("translate_button", false));
        }*/
        return true;
    }

    public void setContextTranslateEnabled(boolean enable) {
        messagesController.getMainSettings().edit().putBoolean("translate_button", contextTranslateEnabled = enable).apply();
    }

    public void setChatTranslateEnabled(boolean enable) {
        messagesController.getMainSettings().edit().putBoolean("translate_chat_button", chatTranslateEnabled = enable).apply();
    }

    public static boolean isSummarizable(MessageObject messageObject) {
        return (
            messageObject != null &&
            messageObject.messageOwner != null &&
            (BuildVars.DEBUG_PRIVATE_VERSION || messageObject.messageOwner.summary_from_language != null) &&
            !messageObject.isOutOwner() &&
            !messageObject.isRestrictedMessage &&
            !messageObject.isSponsored() &&
            (
                messageObject.type == MessageObject.TYPE_TEXT ||
                messageObject.type == MessageObject.TYPE_VIDEO ||
                messageObject.type == MessageObject.TYPE_PHOTO ||
//                messageObject.type == MessageObject.TYPE_VOICE ||
//                messageObject.type == MessageObject.TYPE_ROUND_VIDEO ||
                messageObject.type == MessageObject.TYPE_FILE ||
                messageObject.type == MessageObject.TYPE_MUSIC ||
                messageObject.type == MessageObject.TYPE_POLL
            ) && (
                !TextUtils.isEmpty(messageObject.messageOwner.message) &&
                messageObject.messageOwner.message.length() > 100
            )
        );
    }

    public static boolean isTranslatable(MessageObject messageObject) {
        return isTranslatable(messageObject, false);
    }

    public static boolean isTranslatable(MessageObject messageObject, boolean manually) {
        return (
            messageObject != null &&
            messageObject.messageOwner != null &&
            (!messageObject.isOutOwner() || manually) &&
            !messageObject.isRestrictedMessage &&
            !messageObject.isSponsored() &&
            (
                messageObject.type == MessageObject.TYPE_TEXT ||
                messageObject.type == MessageObject.TYPE_VIDEO ||
                messageObject.type == MessageObject.TYPE_PHOTO ||
                messageObject.type == MessageObject.TYPE_VOICE ||
                messageObject.type == MessageObject.TYPE_ROUND_VIDEO ||
                messageObject.type == MessageObject.TYPE_FILE ||
                messageObject.type == MessageObject.TYPE_MUSIC ||
                messageObject.type == MessageObject.TYPE_POLL
            ) && (
                !TextUtils.isEmpty(messageObject.messageOwner.message) && !MessageHelper.isLinkOrEmojiOnlyMessage(messageObject) ||
                MessageObject.getMedia(messageObject) instanceof TLRPC.TL_messageMediaPoll ||
                (
                    messageObject.messageOwner.voiceTranscriptionOpen &&
                    !TextUtils.isEmpty(messageObject.messageOwner.voiceTranscription) &&
                    messageObject.messageOwner.voiceTranscriptionFinal
                )
            )
        );
    }

    public boolean isDialogTranslatable(long dialogId) {
        return (
            translatableDialogs.contains(dialogId) &&
            isFeatureAvailable(dialogId) &&
            !DialogObject.isEncryptedDialog(dialogId) &&
            getUserConfig().getClientUserId() != dialogId
            /* DialogObject.isChatDialog(dialogId) &&*/
        );
    }

    public boolean isTranslateDialogHidden(long dialogId) {
        if (hideTranslateDialogs.contains(dialogId)) {
            return true;
        }
        /*TLRPC.ChatFull chatFull = getMessagesController().getChatFull(-dialogId);
        if (chatFull != null) {
            return chatFull.translations_disabled;
        }
        TLRPC.UserFull userFull = getMessagesController().getUserFull(dialogId);
        if (userFull != null) {
            return userFull.translations_disabled;
        }*/
        return false;
    }

    private boolean isChatAutoTranslated(long dialogId) {
        if (!isDialogTranslatable(dialogId)) {
            return false;
        }
        final TLRPC.Chat chat = getMessagesController().getChat(-dialogId);
        return chat != null && chat.autotranslation;
    }

    public boolean isTranslatingDialog(long dialogId) {
        return isFeatureAvailable(dialogId) && translatingDialogs.get(dialogId, isChatAutoTranslated(dialogId));
    }

    public void toggleTranslatingDialog(long dialogId) {
        toggleTranslatingDialog(dialogId, !isTranslatingDialog(dialogId));
    }

    public boolean toggleTranslatingDialog(long dialogId, boolean value) {
        boolean currentValue = isTranslatingDialog(dialogId), notified = false;
        if (value && !currentValue) {
            translatingDialogs.put(dialogId, true);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, true);
            notified = true;
        } else if (!value && currentValue) {
            translatingDialogs.put((Long) dialogId, false);
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, false);
            cancelTranslations(dialogId);
            notified = true;
        }
        saveTranslatingDialogsCache();
        return notified;
    }

    private int hash(MessageObject messageObject) {
        if (messageObject == null) {
            return 0;
        }
        return Objects.hash(messageObject.getDialogId(), messageObject.getId());
    }

    private String currentLanguage() {
        String lang = LocaleController.getInstance().getCurrentLocaleInfo().pluralLangCode;
        if (lang != null) {
            lang = lang.split("_")[0];
        }
        return lang;
    }

    public String getDialogTranslateTo(long dialogId) {
        String lang = translateDialogLanguage.get(dialogId);
        if (lang == null) {
            lang = NekoConfig.translationTarget;
            if (lang == null || Translator.getTargetLanguage(lang).equals(getDialogDetectedLanguage(dialogId))) {
                lang = currentLanguage();
            }
        }
        if ("nb".equals(lang)) {
            lang = "no";
        }
        return lang;
    }

    public void setDialogTranslateTo(long dialogId, String language) {
        if (TextUtils.equals(getDialogTranslateTo(dialogId), language)) {
            return;
        }

        boolean wasTranslating = isTranslatingDialog(dialogId);

        if (wasTranslating) {
            AndroidUtilities.runOnUIThread(() -> {
                synchronized (TranslateController.this) {
                    translateDialogLanguage.put(dialogId, language);
                    translatingDialogs.put(dialogId, true);
                    saveTranslatingDialogsCache();
                }
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, true);
            }, 150);
        } else {
            synchronized (TranslateController.this) {
                translateDialogLanguage.put(dialogId, language);
            }
        }

        cancelTranslations(dialogId);
        synchronized (this) {
            translatingDialogs.put(dialogId, false);
        }
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, false);

        //TranslateAlert2.setToLanguage(language);
    }

    public void updateDialogFull(long dialogId) {
        if (true || !isFeatureAvailable(dialogId) || !isDialogTranslatable(dialogId)) {
            return;
        }

        final boolean wasHidden = hideTranslateDialogs.contains(dialogId);

        boolean hidden = false;
        TLRPC.ChatFull chatFull = getMessagesController().getChatFull(-dialogId);
        if (chatFull != null) {
            hidden = chatFull.translations_disabled;
        } else {
            TLRPC.UserFull userFull = getMessagesController().getUserFull(dialogId);
            if (userFull != null) {
                hidden = userFull.translations_disabled;
            }
        }

        synchronized (this) {
            if (hidden) {
                hideTranslateDialogs.add(dialogId);
                translatingDialogs.remove(dialogId);
            } else {
                hideTranslateDialogs.remove(dialogId);
            }
        }

        if (wasHidden != hidden) {
            saveTranslatingDialogsCache();
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, isTranslatingDialog(dialogId));
        }
    }

    public void setHideTranslateDialog(long dialogId, boolean hide) {
        setHideTranslateDialog(dialogId, hide, false);
    }

    public void setHideTranslateDialog(long dialogId, boolean hide, boolean doNotNotify) {
        /*TLRPC.TL_messages_togglePeerTranslations req = new TLRPC.TL_messages_togglePeerTranslations();
        req.peer = getMessagesController().getInputPeer(dialogId);
        req.disabled = hide;
        getConnectionsManager().sendRequest(req, null);

        TLRPC.ChatFull chatFull = getMessagesController().getChatFull(-dialogId);
        if (chatFull != null) {
            chatFull.translations_disabled = hide;
            getMessagesStorage().updateChatInfo(chatFull, true);
        }
        TLRPC.UserFull userFull = getMessagesController().getUserFull(dialogId);
        if (userFull != null) {
            userFull.translations_disabled = hide;
            getMessagesStorage().updateUserInfo(userFull, true);
        }*/

        synchronized (this) {
            if (hide) {
                hideTranslateDialogs.add(dialogId);
                translatingDialogs.remove(dialogId);
            } else {
                hideTranslateDialogs.remove(dialogId);
            }
        }
        saveTranslatingDialogsCache();

        if (!doNotNotify) {
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, isTranslatingDialog(dialogId));
        }
    }

    private static final List<String> languagesOrder = Arrays.asList(
        "en", "ar", "zh", "fr", "de", "it", "ja", "ko", "pt", "ru", "es", "uk"
    );

    private static final List<String> allLanguages = Arrays.asList(
        "af", "sq", "am", "ar", "hy", "az", "eu", "be", "bn", "bs", "bg", "ca",
        "ceb", "zh-cn", "zh", "zh-tw", "co", "hr", "cs", "da", "nl", "en", "eo",
        "et", "fi", "fr", "fy", "gl", "ka", "de", "el", "gu", "ht", "ha", "haw",
        "he", "hi", "hmn", "hu", "is", "ig", "id", "ga", "it", "ja", "jv",
        "kn", "kk", "km", "rw", "ko", "ku", "ky", "lo", "la", "lv", "lt", "lb",
        "mk", "mg", "ms", "ml", "mt", "mi", "mr", "mn", "my", "ne", "no", "ny",
        "or", "ps", "fa", "pl", "pt", "pa", "ro", "ru", "sm", "gd", "sr", "st",
        "sn", "sd", "si", "sk", "sl", "so", "es", "su", "sw", "sv", "tl", "tg",
        "ta", "tt", "te", "th", "tr", "tk", "uk", "ur", "ug", "uz", "vi", "cy",
        "xh", "yi", "yo", "zu"
    );

    public static class Language {
        public String code;
        public String displayName;
        public String ownDisplayName;

        public String q;
    }

    public static ArrayList<Language> getLanguages() {
        ArrayList<String> targetLanguages = new ArrayList<>(Translator.getCurrentTargetLanguages());
        ArrayList<Language> result = new ArrayList<>();
        for (int i = 0; i < targetLanguages.size(); ++i) {
            Language language = new Language();
            language.code = targetLanguages.get(i);
            if ("no".equals(language.code)) {
                language.code = "nb";
            }
            language.displayName = TranslateAlert2.capitalFirst(TranslateAlert2.languageName(language.code));
            language.ownDisplayName = TranslateAlert2.capitalFirst(TranslateAlert2.systemLanguageName(language.code, true));
            if (language.displayName == null) {
                continue;
            }
            language.q = (language.displayName + " " + (language.ownDisplayName == null ? "" : language.ownDisplayName)).toLowerCase();
            result.add(language);
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Collator collator = Collator.getInstance(Locale.getDefault());
            Collections.sort(result, (lng1, lng2) -> collator.compare(lng1.displayName, lng2.displayName));
        } else {
            Collections.sort(result, Comparator.comparing(lng -> lng.displayName));
        }
        return result;
    }

    private static LinkedHashSet<String> suggestedLanguageCodes = null;
    public static void invalidateSuggestedLanguageCodes() {
        suggestedLanguageCodes = null;
    }
    public static void analyzeSuggestedLanguageCodes() {
        LinkedHashSet<String> langs = new LinkedHashSet<>();
        try {
            langs.add(Resources.getSystem().getConfiguration().locale.getLanguage());
        } catch (Exception e2) {
            FileLog.e(e2);
        }
        try {
            langs.addAll(Translator.getRestrictedLanguages());
        } catch (Exception e3) {
            FileLog.e(e3);
        }
        try {
            InputMethodManager imm = (InputMethodManager) ApplicationLoader.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE);
            List<InputMethodInfo> ims = imm.getEnabledInputMethodList();
            for (InputMethodInfo method : ims) {
                List<InputMethodSubtype> submethods = imm.getEnabledInputMethodSubtypeList(method, true);
                for (InputMethodSubtype submethod : submethods) {
                    if ("keyboard".equals(submethod.getMode())) {
                        String currentLocale = submethod.getLocale();
                        if (currentLocale != null && currentLocale.contains("_")) {
                            currentLocale = currentLocale.split("_")[0];
                        }
                        if (TranslateAlert2.languageName(currentLocale) != null) {
                            langs.add(currentLocale);
                        }
                    }
                }
            }
        } catch (Exception e4) {
            FileLog.e(e4);
        }
        suggestedLanguageCodes = langs;
    }

    public static ArrayList<Language> getSuggestedLanguages(String except) {
        ArrayList<Language> result = new ArrayList<>();
        if (suggestedLanguageCodes == null) {
            analyzeSuggestedLanguageCodes();
            if (suggestedLanguageCodes == null) {
                return result;
            }
        }
        Iterator<String> i = suggestedLanguageCodes.iterator();
        while (i.hasNext()) {
            final String code = i.next();
            if (TextUtils.equals(code, except) || "no".equals(except) && "nb".equals(code) || "nb".equals(except) && "no".equals(code)) {
                continue;
            }
            Language language = new Language();
            language.code = code;
            language.displayName = code.equals("app") ? LocaleController.getString(R.string.TranslationTargetApp) : TranslateAlert2.capitalFirst(TranslateAlert2.languageName(language.code));
            if ("no".equals(language.code)) {
                language.code = "nb";
            }
            if (language.displayName == null) {
                continue;
            }
            language.q = (language.displayName + " " + language.ownDisplayName).toLowerCase();
            result.add(language);
        }
        Language language = new Language();
        language.code = "app";
        language.displayName = LocaleController.getString(R.string.TranslationTargetApp);
        result.add(0, language);
        return result;
    }

    public static ArrayList<LocaleController.LocaleInfo> getLocales() {
        HashMap<String, LocaleController.LocaleInfo> languages = LocaleController.getInstance().languagesDict;
        ArrayList<LocaleController.LocaleInfo> locales = new ArrayList<>(languages.values());
        for (int i = 0; i < locales.size(); ++i) {
            LocaleController.LocaleInfo locale = locales.get(i);
            if (locale == null || locale.shortName != null && locale.shortName.endsWith("_raw") || !"remote".equals(locale.pathToFile)) {
                locales.remove(i);
                i--;
            }
        }

        final LocaleController.LocaleInfo currentLocale = LocaleController.getInstance().getCurrentLocaleInfo();
        Comparator<LocaleController.LocaleInfo> comparator = (o, o2) -> {
            if (o == currentLocale) {
                return -1;
            } else if (o2 == currentLocale) {
                return 1;
            }
            final int index1 = languagesOrder.indexOf(o.pluralLangCode);
            final int index2 = languagesOrder.indexOf(o2.pluralLangCode);
            if (index1 >= 0 && index2 >= 0) {
                return index1 - index2;
            } else if (index1 >= 0) {
                return -1;
            } else if (index2 >= 0) {
                return 1;
            }
            if (o.serverIndex == o2.serverIndex) {
                return o.name.compareTo(o2.name);
            }
            if (o.serverIndex > o2.serverIndex) {
                return 1;
            } else if (o.serverIndex < o2.serverIndex) {
                return -1;
            }
            return 0;
        };
        Collections.sort(locales, comparator);

        return locales;
    }

    public void checkRestrictedLanguagesUpdate() {
        synchronized (this) {
            translatableDialogMessages.clear();

            ArrayList<Long> toNotify = new ArrayList<>();
            for (long dialogId : translatableDialogs) {
                String language = detectedDialogLanguage.get(dialogId);
                if (language != null && Translator.isLanguageRestricted(language)) {
                    cancelTranslations(dialogId);
                    translatingDialogs.remove(dialogId);
                    toNotify.add(dialogId);
                }
            }
            translatableDialogs.clear();
            saveTranslatingDialogsCache();

            for (long dialogId : toNotify) {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogTranslate, dialogId, false);
            }
        }
    }

    @Nullable
    public String getDialogDetectedLanguage(long dialogId) {
        return detectedDialogLanguage.get(dialogId);
    }

    public void checkTranslation(MessageObject messageObject, boolean onScreen) {
        checkTranslation(messageObject, onScreen, false);
    }

    private void checkTranslation(MessageObject messageObject, boolean onScreen, boolean keepReply) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }

        final long dialogId = messageObject.getDialogId();

        if (onScreen && messageObject.messageOwner.summarizedOpen && messageObject.messageOwner.summaryText == null && !isTranslatingDialog(messageObject.getDialogId())) {
            final MessageObject finalMessageObject = messageObject;
            pushToSummarize(finalMessageObject, null, (text) -> {
                finalMessageObject.messageOwner.summaryText = text;

                getMessagesStorage().updateMessageCustomParams(dialogId, finalMessageObject.messageOwner);
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, finalMessageObject, true);
            });
        }

        if (!isFeatureAvailable(dialogId)) {
            return;
        }

        if (!keepReply && messageObject.replyMessageObject != null) {
            checkTranslation(messageObject.replyMessageObject, onScreen, true);
        }

        if (!isTranslatable(messageObject)) {
            return;
        }

        if (!isTranslatingDialog(dialogId)) {
            checkLanguage(messageObject);
            return;
        }

        if (isTranslateDialogHidden(dialogId)) {
            return;
        }

        final String language = Translator.getTargetLanguage(getDialogTranslateTo(dialogId));
        MessageObject potentialReplyMessageObject;
        if (!keepReply && (
                (messageObject.messageOwner.voiceTranscriptionOpen && messageObject.messageOwner.voiceTranscriptionFinal ? messageObject.messageOwner.translatedVoiceTranscription : messageObject.messageOwner.translatedText) == null && messageObject.messageOwner.translatedPoll == null ||
                messageObject.messageOwner.translatedPoll != null && !PollText.isFullyTranslated(messageObject, messageObject.messageOwner.translatedPoll) ||
                !language.equals(messageObject.messageOwner.translatedToLanguage)
            ) &&
            (potentialReplyMessageObject = findReplyMessageObject(dialogId, messageObject.getId())) != null
        ) {
            messageObject.messageOwner.translatedToLanguage = potentialReplyMessageObject.messageOwner.translatedToLanguage;
            messageObject.messageOwner.translatedText = potentialReplyMessageObject.messageOwner.translatedText;
            messageObject.messageOwner.translatedPoll = potentialReplyMessageObject.messageOwner.translatedPoll;
            messageObject = potentialReplyMessageObject;
        }

        if (onScreen && isTranslatingDialog(dialogId)) {
            final MessageObject finalMessageObject = messageObject;
            if (finalMessageObject.messageOwner.summarizedOpen) {
                if (finalMessageObject.messageOwner.translatedSummaryText == null || !language.equals(finalMessageObject.messageOwner.translatedSummaryLanguage)) {
                    pushToSummarize(finalMessageObject, language, text -> {
                        finalMessageObject.messageOwner.translatedSummaryLanguage = language;
                        finalMessageObject.messageOwner.translatedSummaryText = text;

                        getMessagesStorage().updateMessageCustomParams(dialogId, finalMessageObject.messageOwner);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, finalMessageObject, true);
                    });
                }
            } else if (
                (finalMessageObject.messageOwner.voiceTranscriptionOpen && finalMessageObject.messageOwner.voiceTranscriptionFinal ? finalMessageObject.messageOwner.translatedVoiceTranscription : finalMessageObject.messageOwner.translatedText) == null &&
                finalMessageObject.messageOwner.translatedPoll == null ||
                finalMessageObject.messageOwner.translatedPoll != null && !PollText.isFullyTranslated(finalMessageObject, finalMessageObject.messageOwner.translatedPoll) ||
                !language.equals(finalMessageObject.messageOwner.translatedToLanguage)
            ) {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslating, finalMessageObject);
                if (MessageObject.getMedia(finalMessageObject) instanceof TLRPC.TL_messageMediaPoll) {
                    pushPollToTranslate(finalMessageObject, language, (poll, sLang, lang) -> {
                        finalMessageObject.messageOwner.originalLanguage = sLang;
                        if (sLang != null && (sLang.equals(lang) || Translator.isLanguageRestricted(sLang))) {
                            getMessagesStorage().updateMessageCustomParams(dialogId, finalMessageObject.messageOwner);
                            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, finalMessageObject);
                            return;
                        }
                        finalMessageObject.messageOwner.translatedToLanguage = lang;
                        finalMessageObject.messageOwner.translatedText = null;
                        finalMessageObject.messageOwner.translatedVoiceTranscription = null;
                        finalMessageObject.messageOwner.translatedPoll = poll;
                        if (keepReply) {
                            keepReplyMessage(finalMessageObject);
                        }

                        getMessagesStorage().updateMessageCustomParams(dialogId, finalMessageObject.messageOwner);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, finalMessageObject);

                        ArrayList<MessageObject> dialogMessages = messagesController.dialogMessage.get(dialogId);
                        if (dialogMessages != null) {
                            for (int i = 0; i < dialogMessages.size(); ++i) {
                                MessageObject dialogMessage = dialogMessages.get(i);
                                if (dialogMessage != null && dialogMessage.getId() == finalMessageObject.getId()) {
                                    dialogMessage.messageOwner.translatedToLanguage = lang;
                                    dialogMessage.messageOwner.translatedText = null;
                                    dialogMessage.messageOwner.translatedVoiceTranscription = null;
                                    dialogMessage.messageOwner.translatedPoll = poll;
                                    if (dialogMessage.updateTranslation()) {
                                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, 0);
                                    }
                                    break;
                                }
                            }
                        }
                    });
                } else {
                    pushToTranslate(finalMessageObject, language, (isTranscription, text, sLang, lang) -> {
                        finalMessageObject.messageOwner.originalLanguage = sLang;
                        if (sLang != null && (sLang.equals(lang) || Translator.isLanguageRestricted(sLang))) {
                            getMessagesStorage().updateMessageCustomParams(dialogId, finalMessageObject.messageOwner);
                            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, finalMessageObject);
                            return;
                        }
                        finalMessageObject.messageOwner.translatedToLanguage = lang;
                        if (isTranscription) {
                            finalMessageObject.messageOwner.translatedVoiceTranscription = text;
                        } else {
                            finalMessageObject.messageOwner.translatedText = text;
                        }
                        finalMessageObject.messageOwner.translatedPoll = null;
                        if (keepReply) {
                            keepReplyMessage(finalMessageObject);
                        }

                        getMessagesStorage().updateMessageCustomParams(dialogId, finalMessageObject.messageOwner);
                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, finalMessageObject);

                        ArrayList<MessageObject> dialogMessages = messagesController.dialogMessage.get(dialogId);
                        if (dialogMessages != null) {
                            for (int i = 0; i < dialogMessages.size(); ++i) {
                                MessageObject dialogMessage = dialogMessages.get(i);
                                if (dialogMessage != null && dialogMessage.getId() == finalMessageObject.getId()) {
                                    dialogMessage.messageOwner.originalLanguage = sLang;
                                    dialogMessage.messageOwner.translatedToLanguage = lang;
                                    if (isTranscription) {
                                        dialogMessage.messageOwner.translatedVoiceTranscription = text;
                                    } else {
                                        dialogMessage.messageOwner.translatedText = text;
                                    }
                                    dialogMessage.messageOwner.translatedPoll = null;
                                    if (dialogMessage.updateTranslation()) {
                                        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, 0);
                                    }
                                    break;
                                }
                            }
                        }
                    });
                }
            } else if (keepReply) {
                keepReplyMessage(messageObject);
            }
        }
    }

    public void invalidateTranslation(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        final long dialogId = messageObject.getDialogId();
        if (!isFeatureAvailable(dialogId)) {
            return;
        }
        messageObject.messageOwner.translatedToLanguage = null;
        messageObject.messageOwner.translatedText = null;
        messageObject.messageOwner.translatedVoiceTranscription = null;
        messageObject.messageOwner.translatedPoll = null;
        messageObject.messageOwner.summaryText = null;
        messageObject.messageOwner.translatedSummaryText = null;
        messageObject.messageOwner.translatedSummaryLanguage = null;
        getMessagesStorage().updateMessageCustomParams(dialogId, messageObject.messageOwner);
        AndroidUtilities.runOnUIThread(() -> {
            NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.messageTranslated, messageObject, false, isTranslatingDialog(dialogId));
        });
    }

    public void checkDialogMessage(long dialogId) {
        if (isFeatureAvailable(dialogId)) {
            checkDialogMessageSure(dialogId);
        }
    }

    public void checkDialogMessageSure(long dialogId) {
        if (!translatingDialogs.get(dialogId, isChatAutoTranslated(dialogId))) {
            return;
        }
        getMessagesStorage().getStorageQueue().postRunnable(() -> {
            final ArrayList<MessageObject> dialogMessages = messagesController.dialogMessage.get(dialogId);
            if (dialogMessages == null) {
                return;
            }
            ArrayList<TLRPC.Message> customProps = new ArrayList<>();
            for (int i = 0; i < dialogMessages.size(); ++i) {
                MessageObject dialogMessage = dialogMessages.get(i);
                if (dialogMessage == null || dialogMessage.messageOwner == null) {
                    customProps.add(null);
                    continue;
                }
                customProps.add(getMessagesStorage().getMessageWithCustomParamsOnlyInternal(dialogMessage.getId(), dialogMessage.getDialogId()));
            }
            AndroidUtilities.runOnUIThread(() -> {
                boolean updated = false;
                for (int i = 0; i < Math.min(customProps.size(), dialogMessages.size()); ++i) {
                    MessageObject dialogMessage = dialogMessages.get(i);
                    TLRPC.Message props = customProps.get(i);
                    if (dialogMessage == null || dialogMessage.messageOwner == null || props == null) {
                        continue;
                    }
                    dialogMessage.messageOwner.translatedText = props.translatedText;
                    dialogMessage.messageOwner.translatedPoll = props.translatedPoll;
                    dialogMessage.messageOwner.translatedToLanguage = props.translatedToLanguage;
                    if (dialogMessage.updateTranslation(false)) {
                        updated = true;
                    }
                }
                if (updated) {
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.updateInterfaces, 0);
                }
            });
        });
    }


    public void cleanup() {
        cancelAllTranslations();
        resetTranslatingDialogsCache();

        translatingDialogs.clear();
        translatableDialogs.clear();
        translatableDialogMessages.clear();
        translateDialogLanguage.clear();
        detectedDialogLanguage.clear();
        keptReplyMessageObjects.clear();
        hideTranslateDialogs.clear();
        loadingTranslations.clear();
        loadingTranscriptionTranslations.clear();
    }

    public void reset() {
        translatableDialogMessages.clear();
        detectedDialogLanguage.clear();
    }

    private ArrayList<Integer> pendingLanguageChecks = new ArrayList<>();
    private void checkLanguage(MessageObject messageObject) {
        if (!LanguageDetector.hasSupport()) {
            return;
        }
        if (!isTranslatable(messageObject) || messageObject.messageOwner == null || TextUtils.isEmpty(messageObject.messageOwner.message)) {
            return;
        }
        if (messageObject.messageOwner.originalLanguage != null) {
            checkDialogTranslatable(messageObject);
            return;
        }

        final long dialogId = messageObject.getDialogId();
        final int hash = hash(messageObject);
        if (isDialogTranslatable(dialogId)) {
            return;
        }
        if (pendingLanguageChecks.contains(hash)) {
            return;
        }

        pendingLanguageChecks.add(hash);

        Utilities.stageQueue.postRunnable(() -> {
            LanguageDetector.detectLanguage(messageObject.messageOwner.message, lng -> AndroidUtilities.runOnUIThread(() -> {
                String detectedLanguage = lng;
                if (detectedLanguage == null) {
                    detectedLanguage = UNKNOWN_LANGUAGE;
                }
                messageObject.messageOwner.originalLanguage = detectedLanguage;
                getMessagesStorage().updateMessageCustomParams(dialogId, messageObject.messageOwner);
                pendingLanguageChecks.remove((Integer) hash);
                checkDialogTranslatable(messageObject);
            }), err -> AndroidUtilities.runOnUIThread(() -> {
                messageObject.messageOwner.originalLanguage = UNKNOWN_LANGUAGE;
                getMessagesStorage().updateMessageCustomParams(dialogId, messageObject.messageOwner);
                pendingLanguageChecks.remove((Integer) hash);
            }));
        });
    }

    private void checkDialogTranslatable(MessageObject messageObject) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }

        final long dialogId = messageObject.getDialogId();
        TranslatableDecision translatableMessages = translatableDialogMessages.get(dialogId);
        if (translatableMessages == null) {
            translatableDialogMessages.put(dialogId, translatableMessages = new TranslatableDecision());
        }

        final boolean isUnknown = isTranslatable(messageObject) && (
            messageObject.messageOwner.originalLanguage == null ||
            UNKNOWN_LANGUAGE.equals(messageObject.messageOwner.originalLanguage)
        );
        final boolean translatable = (
            isTranslatable(messageObject) &&
            messageObject.messageOwner.originalLanguage != null &&
            !UNKNOWN_LANGUAGE.equals(messageObject.messageOwner.originalLanguage) &&
            !Translator.isLanguageRestricted(messageObject.messageOwner.originalLanguage)
        );

        if (isUnknown) {
            translatableMessages.unknown.add(messageObject.getId());
        } else {
            (translatable ? translatableMessages.certainlyTranslatable : translatableMessages.certainlyNotTranslatable).add(messageObject.getId());
        }

        if (!isUnknown) {
            detectedDialogLanguage.put(dialogId, messageObject.messageOwner.originalLanguage);
        }

        final int translatableCount = translatableMessages.certainlyTranslatable.size();
        final int unknownCount = translatableMessages.unknown.size();
        final int notTranslatableCount = translatableMessages.certainlyNotTranslatable.size();
        final int totalCount = translatableCount + unknownCount + notTranslatableCount;
        final boolean autotranslation = isChatAutoTranslated(dialogId);
        if (
            totalCount >= (autotranslation ? REQUIRED_TOTAL_MESSAGES_CHECKED_AUTOTRANSLATE : REQUIRED_TOTAL_MESSAGES_CHECKED) &&
            (autotranslation ?
                translatableCount >= REQUIRED_MIN_MESSAGES_TRANSLATABLE_AUTOTRANSLATE :
                (translatableCount / (float) (translatableCount + notTranslatableCount)) >= REQUIRED_PERCENTAGE_MESSAGES_TRANSLATABLE
            ) &&
            (unknownCount / (float) totalCount) < (autotranslation ? REQUIRED_MIN_PERCENTAGE_MESSAGES_UNKNOWN_AUTOTRANSLATE : REQUIRED_MIN_PERCENTAGE_MESSAGES_UNKNOWN)
        ) {
            translatableDialogs.add(dialogId);
            translatableDialogMessages.remove((Long) dialogId);
            AndroidUtilities.runOnUIThread(() -> {
                NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogIsTranslatable, dialogId);
            }, 450);
        }
    }

    private final Set<Integer> loadingTranslations = new HashSet<>();
    private final Set<Integer> loadingTranscriptionTranslations = new HashSet<>();
    private final HashMap<Long, ArrayList<PendingTranslation>> pendingTranslations = new HashMap<>();
    private final HashMap<Long, ArrayList<PendingTranslation>> pendingTranscriptionsTranslations = new HashMap<>();

    private final HashSet<Integer> loadingSummarizations = new HashSet<>();

    private void pushToSummarize(
        MessageObject message,
        @Nullable String language,
        Utilities.Callback<TLRPC.TL_textWithEntities> callback
    ) {
        final int id = Objects.hash(message.getDialogId(), message.getId(), language != null ? 1 : 0);
        if (loadingSummarizations.contains(id)) return;

        loadingSummarizations.add(id);

        final TLRPC.TL_messages_summarizeText req = new TLRPC.TL_messages_summarizeText();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(message.getDialogId());
        req.id = message.getId();
        if (language != null) {
            req.flags |= TLObject.FLAG_0;
            req.to_lang = language;
        }
        ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, (res, err) -> AndroidUtilities.runOnUIThread(() -> {
            if (res != null) {
                loadingSummarizations.remove(id);
                callback.run(res);
            }
        }));
    }

    private static class PendingTranslation {
        Runnable runnable;
        ArrayList<Integer> messageIds = new ArrayList<>();
        ArrayList<TLRPC.TL_textWithEntities> messageTexts = new ArrayList<>();
        ArrayList<Utilities.Callback4<Boolean, Integer, TLRPC.TL_textWithEntities, String>> callbacks = new ArrayList<>();
        String language;

        int delay = GROUPING_TRANSLATIONS_TIMEOUT;
        int symbolsCount;

        int reqId = -1;
    }

    private void pushToTranslate(
        MessageObject message,
        String language,
        Utilities.Callback4<Boolean, TLRPC.TL_textWithEntities, String, String> callback
    ) {
        if (message == null || message.messageOwner == null || message.getId() < 0 || callback == null) {
            return;
        }

        final boolean isTranscription = message.messageOwner.voiceTranscription != null && message.messageOwner.voiceTranscriptionFinal && message.messageOwner.voiceTranscriptionOpen;
        final long dialogId = message.getDialogId();

        final TLRPC.TL_textWithEntities source = new TLRPC.TL_textWithEntities();
        if (isTranscription) {
            source.text = message.messageOwner.voiceTranscription;
            if (TextUtils.isEmpty(source.text)) return;
        } else {
            source.text = message.messageOwner.message;
            source.entities = message.messageOwner.entities;
        }

        Translator.translate(source, null, message.messageOwner.originalLanguage, language, new Translator.TranslateCallBack() {
            @Override
            public void onSuccess(TLRPC.TL_textWithEntities translation, String sourceLanguage, String targetLanguage) {
                callback.run(isTranscription, translation, sourceLanguage, targetLanguage);
            }

            @Override
            public void onError(Throwable t) {
                toggleTranslatingDialog(dialogId, false);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_ERROR, LocaleController.getString(t instanceof Http429Exception ? R.string.TranslationFailedAlert1 : R.string.TranslationFailedAlert2));
            }
        });
    }

//    private void systemTranslate(String text, String fromLanguage, String toLanguage, Utilities.Callback<String> done) {
//        if (done == null) return;
//        if (fromLanguage == null) {
//            LanguageDetector.detectLanguage(text, lng -> {
//                systemTranslate(text, lng, toLanguage, done);
//            }, e -> {
//                systemTranslate(text, "en", toLanguage, done);
//            });
//            return;
//        }
//
//        checkModelInstalled(fromLanguage, () -> {
//            checkModelInstalled(toLanguage, () -> {
//
//                TranslatorOptions.Builder options = new TranslatorOptions.Builder();
//                options.setSourceLanguage(fromLanguage);
//                options.setTargetLanguage(toLanguage);
//                final Translator translator = Translation.getClient(options.build());
//
//                FileLog.d("[mlkit] downloadModelIfNeeded started");
//                final long start = System.currentTimeMillis();
//                final int[] triesCount = new int[1];
//                final Runnable[] tryDownloadModelIfNeeded = new Runnable[1];
//                tryDownloadModelIfNeeded[0] = () -> {
//                    translator.downloadModelIfNeeded()
//                        .addOnSuccessListener(v -> {
//                            forceModelInstalled(fromLanguage);
//                            forceModelInstalled(toLanguage);
//
//                            final String[] lines = text.split("\n");
//                            final String[] result = new String[lines.length];
//                            final boolean[] sent = new boolean[1];
//                            final Utilities.Callback0Return<Boolean> isDone = () -> {
//                                boolean allDone = true;
//                                for (int i = 0; i < result.length; ++i) {
//                                    if (result[i] == null) {
//                                        allDone = false;
//                                        break;
//                                    }
//                                }
//                                return allDone;
//                            };
//
//                            for (int i = 0; i < lines.length; ++i) {
//                                final int index = i;
//                                final String line = lines[i];
//                                if (TextUtils.isEmpty(line) || line.replaceAll("[\\t\\n\\r]", "").isEmpty()) {
//                                    result[i] = line;
//                                    if (isDone.run() && !sent[0]) {
//                                        sent[0] = true;
//                                        AndroidUtilities.runOnUIThread(() -> {
//                                            done.run(TextUtils.join("\n", result));
//                                        });
//                                    }
//                                } else {
//                                    final long start2 = System.currentTimeMillis();
//                                    translator.translate(line)
//                                        .addOnSuccessListener(res -> {
//                                            FileLog.d("[mlkit] translate success took " + (System.currentTimeMillis() - start2) + "ms");
//                                            AndroidUtilities.runOnUIThread(() -> {
//                                                result[index] = res;
//                                                if (isDone.run() && !sent[0]) {
//                                                    sent[0] = true;
//                                                    AndroidUtilities.runOnUIThread(() -> {
//                                                        done.run(TextUtils.join("\n", result));
//                                                    });
//                                                }
//                                            });
//                                        })
//                                        .addOnFailureListener(err -> {
//                                            FileLog.d("[mlkit] translate fail took " + (System.currentTimeMillis() - start2) + "ms");
//                                            FileLog.e("[mlkit] translate failed", err);
//                                            if (!sent[0]) {
//                                                sent[0] = true;
//                                                done.run(null);
//                                            }
//                                        });
//                                }
//                            }
//                        })
//                        .addOnFailureListener(err -> {
//                            FileLog.d("[mlkit] downloadModelIfNeeded fail took " + (System.currentTimeMillis() - start) + "ms");
//                            FileLog.e("[mlkit] downloadModelIfNeeded failed", err);
//                            if (triesCount[0] < 3) {
//                                FileLog.d("[mlkit] downloadModelIfNeeded trying again (try count = "+triesCount[0]+")");
//                                triesCount[0]++;
//                                AndroidUtilities.runOnUIThread(tryDownloadModelIfNeeded[0], 250);
//                                return;
//                            }
//                            done.run(null);
//                        });
//                };
//                tryDownloadModelIfNeeded[0].run();
//            });
//        });
//    }

    private final HashMap<Long, ArrayList<PendingPollTranslation>> pendingPollTranslations = new HashMap<>();

    private static class PendingPollTranslation {
        Runnable runnable;
        ArrayList<Integer> messageIds = new ArrayList<>();
        ArrayList<Pair<PollText, PollText>> messageTexts = new ArrayList<>();
        ArrayList<Utilities.Callback3<PollText, String, String>> callbacks = new ArrayList<>();
        String language;

        int delay = GROUPING_TRANSLATIONS_TIMEOUT;
        int symbolsCount;

        int reqId = -1;
    }

    private void pushPollToTranslate(
        MessageObject message,
        String language,
        Utilities.Callback3<PollText, String, String> callback
    ) {
        if (message == null || message.getId() < 0 || callback == null) {
            return;
        }

        long dialogId = message.getDialogId();

        final TLRPC.MessageMedia media = MessageObject.getMedia(message);
        if (!(media instanceof TLRPC.TL_messageMediaPoll)) {
            return;
        }
        final TLRPC.TL_messageMediaPoll mediaPoll = (TLRPC.TL_messageMediaPoll) media;
        final PollText pollText = PollText.fromPoll(mediaPoll);

        Translator.translate(null, pollText, message.messageOwner.originalLanguage, language, new Translator.TranslateCallBack() {

            @Override
            public void onSuccess(TLRPC.TL_textWithEntities translation, PollText poll, String sourceLanguage, String targetLanguage) {
                callback.run(poll, sourceLanguage, targetLanguage);
            }

            @Override
            public void onError(Throwable t) {
                toggleTranslatingDialog(dialogId, false);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.showBulletin, Bulletin.TYPE_ERROR, LocaleController.getString(t instanceof Http429Exception ? R.string.TranslationFailedAlert1 : R.string.TranslationFailedAlert2));
            }
        });
    }

    public boolean isTranslating(MessageObject messageObject) {
        if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.summarizedOpen)
            return loadingSummarizations.contains(Objects.hash(messageObject.getDialogId(), messageObject.getId(), isTranslatingDialog(messageObject.getDialogId()) ? 1 : 0));
        synchronized (this) {
            return (
                messageObject != null &&
                messageObject.messageOwner != null &&
                (messageObject.messageOwner.voiceTranscriptionOpen && messageObject.messageOwner.voiceTranscriptionFinal ?
                    loadingTranscriptionTranslations : loadingTranslations
                ).contains(messageObject.getId()) &&
                isTranslatingDialog(messageObject.getDialogId())
            );
        }
    }

    public boolean isTranslating(MessageObject messageObject, MessageObject.GroupedMessages group) {
        if (messageObject == null) {
            return false;
        }
        if (!isTranslatingDialog(messageObject.getDialogId())) {
            return false;
        }
        final boolean isTranscription = messageObject.messageOwner != null && messageObject.messageOwner.voiceTranscriptionOpen && messageObject.messageOwner.voiceTranscriptionFinal;
        synchronized (this) {
            if ((isTranscription ? loadingTranscriptionTranslations : loadingTranslations).contains(messageObject.getId())) {
                return true;
            }
            if (group != null) {
                for (MessageObject message : group.messages) {
                    if ((isTranscription ? loadingTranscriptionTranslations : loadingTranslations).contains(message.getId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void cancelAllTranslations() {
        synchronized (this) {
            for (ArrayList<PendingTranslation> translations : pendingTranslations.values()) {
                if (translations != null) {
                    for (PendingTranslation pendingTranslation : translations) {
                        AndroidUtilities.cancelRunOnUIThread(pendingTranslation.runnable);
                        if (pendingTranslation.reqId != -1) {
                            getConnectionsManager().cancelRequest(pendingTranslation.reqId, true);
                            for (Integer messageId : pendingTranslation.messageIds) {
                                loadingTranslations.remove(messageId);
                            }
                        }
                    }
                }
            }
            for (ArrayList<PendingTranslation> translations : pendingTranscriptionsTranslations.values()) {
                if (translations != null) {
                    for (PendingTranslation pendingTranslation : translations) {
                        AndroidUtilities.cancelRunOnUIThread(pendingTranslation.runnable);
                        if (pendingTranslation.reqId != -1) {
                            getConnectionsManager().cancelRequest(pendingTranslation.reqId, true);
                            for (Integer messageId : pendingTranslation.messageIds) {
                                loadingTranscriptionTranslations.remove(messageId);
                            }
                        }
                    }
                }
            }
            for (ArrayList<PendingPollTranslation> translations : pendingPollTranslations.values()) {
                if (translations != null) {
                    for (PendingPollTranslation pendingTranslation : translations) {
                        AndroidUtilities.cancelRunOnUIThread(pendingTranslation.runnable);
                        if (pendingTranslation.reqId != -1) {
                            getConnectionsManager().cancelRequest(pendingTranslation.reqId, true);
                            for (Integer messageId : pendingTranslation.messageIds) {
                                loadingTranslations.remove(messageId);
                            }
                        }
                    }
                }
            }
        }
    }

    public void cancelTranslations(long dialogId) {
        synchronized (this) {
            ArrayList<PendingTranslation> translations = pendingTranslations.get(dialogId);
            if (translations != null) {
                for (PendingTranslation pendingTranslation : translations) {
                    AndroidUtilities.cancelRunOnUIThread(pendingTranslation.runnable);
                    if (pendingTranslation.reqId != -1) {
                        getConnectionsManager().cancelRequest(pendingTranslation.reqId, true);
                        for (Integer messageId : pendingTranslation.messageIds) {
                            loadingTranslations.remove(messageId);
                        }
                    }
                }
                pendingTranslations.remove((Long) dialogId);
            }
            translations = pendingTranscriptionsTranslations.get(dialogId);
            if (translations != null) {
                for (PendingTranslation pendingTranslation : translations) {
                    AndroidUtilities.cancelRunOnUIThread(pendingTranslation.runnable);
                    if (pendingTranslation.reqId != -1) {
                        getConnectionsManager().cancelRequest(pendingTranslation.reqId, true);
                        for (Integer messageId : pendingTranslation.messageIds) {
                            loadingTranscriptionTranslations.remove(messageId);
                        }
                    }
                }
                pendingTranscriptionsTranslations.remove((Long) dialogId);
            }

            ArrayList<PendingPollTranslation> pollTranslations = pendingPollTranslations.get(dialogId);
            if (pollTranslations != null) {
                for (PendingPollTranslation pendingTranslation : pollTranslations) {
                    AndroidUtilities.cancelRunOnUIThread(pendingTranslation.runnable);
                    if (pendingTranslation.reqId != -1) {
                        getConnectionsManager().cancelRequest(pendingTranslation.reqId, true);
                        for (Integer messageId : pendingTranslation.messageIds) {
                            loadingTranslations.remove(messageId);
                        }
                    }
                }
                pendingPollTranslations.remove((Long) dialogId);
            }
        }
    }

    private void keepReplyMessage(MessageObject messageObject) {
        if (messageObject == null) {
            return;
        }
        HashMap<Integer, MessageObject> map = keptReplyMessageObjects.get(messageObject.getDialogId());
        if (map == null) {
            keptReplyMessageObjects.put(messageObject.getDialogId(), map = new HashMap<>());
        }
        map.put(messageObject.getId(), messageObject);
    }

    public MessageObject findReplyMessageObject(long dialogId, int messageId) {
        HashMap<Integer, MessageObject> map = keptReplyMessageObjects.get(dialogId);
        if (map == null) {
            return null;
        }
        return map.get(messageId);
    }

    private void clearAllKeptReplyMessages(long dialogId) {
        keptReplyMessageObjects.remove(dialogId);
    }

    private boolean isLanguageRestricted(String lng) {
        if (getUserConfig().isPremium()) {
            return RestrictedLanguagesSelectActivity.getRestrictedLanguages().contains(lng);
        }
        try {
            return TextUtils.equals(LocaleController.getInstance().getCurrentLocaleInfo().pluralLangCode, lng);
        } catch (Exception ignore) {
            return false;
        }
    }

    private void loadTranslatingDialogsCached() {
        String translatingDialogsCache = messagesController.getMainSettings().getString("translating_dialog_languages2", null);
        if (translatingDialogsCache == null) {
            return;
        }
        String[] dialogs = translatingDialogsCache.split(";");

        for (int i = 0; i < dialogs.length; ++i) {
            String[] keyval = dialogs[i].split("=");
            if (keyval.length < 2) {
                continue;
            }
            long did = Long.parseLong(keyval[0]);
            String[] langs = keyval[1].split(">");
            if (langs.length != 2) {
                continue;
            }
            String from = langs[0], to = langs[1];
            boolean disabled = false;
            if (to.length() > 0 && to.charAt(to.length() - 1) == '!') {
                disabled = true;
                to = to.substring(0, to.length() - 1);
            }
            if ("null".equals(from)) from = null;
            if ("null".equals(to)) to = null;
            if (from != null) {
                detectedDialogLanguage.put(did, from);
                if (!Translator.isLanguageRestricted(from)) {
                    translatingDialogs.put(did, !disabled);
                    translatableDialogs.add(did);
                }
                if (to != null) {
                    translateDialogLanguage.put(did, to);
                }
            }
        }

        Set<String> hidden = messagesController.getMainSettings().getStringSet("hidden_translation_at", null);
        if (hidden != null) {
            Iterator<String> i = hidden.iterator();
            while (i.hasNext()) {
                try {
                    hideTranslateDialogs.add(Long.parseLong(i.next()));
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }
    }

    private void saveTranslatingDialogsCache() {
        StringBuilder langset = new StringBuilder();

        boolean first = true;
        for (int i = 0; i < translatingDialogs.size(); ++i) {
            try {
                final long did = translatingDialogs.keyAt(i);
                if (!first) {
                    langset.append(";");
                }
                if (first) {
                    first = false;
                }
                String lang = detectedDialogLanguage.get(did);
                if (lang == null) {
                    lang = "null";
                }
                String tolang = getDialogTranslateTo(did);
                if (tolang == null) {
                    tolang = "null";
                }
                langset.append(did).append("=").append(lang).append(">").append(tolang);
                if (!translatingDialogs.valueAt(i)) {
                    langset.append("!");
                }
            } catch (Exception e) {}
        }

        Set<String> hidden = new HashSet<>();
        Iterator<Long> i = hideTranslateDialogs.iterator();
        while (i.hasNext()) {
            try {
                hidden.add("" + i.next());
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        MessagesController.getMainSettings(currentAccount).edit().putString("translating_dialog_languages2", langset.toString()).putStringSet("hidden_translation_at", hidden).apply();
    }

    private void resetTranslatingDialogsCache() {
        MessagesController.getMainSettings(currentAccount).edit().remove("translating_dialog_languages2").remove("hidden_translation_at").apply();
    }

    private final HashSet<StoryKey> detectingStories = new HashSet<>();
    private final HashSet<StoryKey> translatingStories = new HashSet<>();

    // ensure dialogId in storyItem is valid
    public void detectStoryLanguage(TL_stories.StoryItem storyItem) {
        if (storyItem == null || storyItem.detectedLng != null || storyItem.caption == null || storyItem.caption.length() == 0 || !LanguageDetector.hasSupport()) {
            return;
        }

        final StoryKey key = new StoryKey(storyItem);
        if (detectingStories.contains(key)) {
            return;
        }
        detectingStories.add(key);

        LanguageDetector.detectLanguage(storyItem.caption, lng -> AndroidUtilities.runOnUIThread(() -> {
            storyItem.detectedLng = lng;
            getMessagesController().getStoriesController().getStoriesStorage().putStoryInternal(storyItem.dialogId, storyItem);
            detectingStories.remove(key);
        }), err -> AndroidUtilities.runOnUIThread(() -> {
            storyItem.detectedLng = UNKNOWN_LANGUAGE;
            getMessagesController().getStoriesController().getStoriesStorage().putStoryInternal(storyItem.dialogId, storyItem);
            detectingStories.remove(key);
        }));
    }

    public boolean canTranslateStory(TL_stories.StoryItem storyItem) {
        return storyItem != null && !TextUtils.isEmpty(storyItem.caption) && !MessageHelper.isLinkOnlyMessage(storyItem.caption, storyItem.entities) && (
            storyItem.detectedLng == null && storyItem.translatedText != null && TextUtils.equals(storyItem.translatedLng, TranslateAlert2.getToLanguage()) ||
            storyItem.detectedLng != null && !Translator.isLanguageRestricted(storyItem.detectedLng)
        );
    }

    public void translateStory(Context context, TL_stories.StoryItem storyItem, Runnable done) {
        if (storyItem == null) {
            return;
        }

        final StoryKey key = new StoryKey(storyItem);

        String toLang = TranslateAlert2.getToLanguage();

        if (storyItem.translatedText != null && TextUtils.equals(storyItem.translatedLng, toLang)) {
            if (done != null) {
                done.run();
            }
            return;
        }
        if (translatingStories.contains(key)) {
            if (done != null) {
                done.run();
            }
            return;
        }

        translatingStories.add(key);

        Translator.translate(storyItem.caption, storyItem.entities, null, storyItem.detectedLng, null, new Translator.TranslateCallBack() {
            @Override
            public void onSuccess(TLRPC.TL_textWithEntities translation, String sourceLanguage, String targetLanguage) {
                storyItem.translatedLng = targetLanguage;
                storyItem.translatedText = translation;
                getMessagesController().getStoriesController().getStoriesStorage().putStoryInternal(storyItem.dialogId, storyItem);
                translatingStories.remove(key);
                if (done != null) {
                    done.run();
                }
            }

            @Override
            public void onError(Throwable t) {
                Translator.handleTranslationError(context, t, () -> translateStory(context, storyItem, done), new DarkThemeResourceProvider());
                translatingStories.remove(key);
                if (done != null) {
                    done.run();
                }
            }
        });
    }

    public boolean isTranslatingStory(TL_stories.StoryItem storyItem) {
        if (storyItem == null) {
            return false;
        }
        return translatingStories.contains(new StoryKey(storyItem));
    }

    private static class StoryKey {
        public long dialogId;
        public int storyId;

        public StoryKey(TL_stories.StoryItem storyItem) {
            dialogId = storyItem.dialogId;
            storyId = storyItem.id;
        }
    }

    private final HashSet<MessageKey> detectingPhotos = new HashSet<>();
    private final HashSet<MessageKey> translatingPhotos = new HashSet<>();

    public void detectPhotoLanguage(MessageObject messageObject, Utilities.Callback<String> done) {
        if (messageObject == null || messageObject.messageOwner == null || !LanguageDetector.hasSupport() || TextUtils.isEmpty(messageObject.messageOwner.message)) {
            return;
        }
        if (!TextUtils.isEmpty(messageObject.messageOwner.originalLanguage)) {
            if (done != null) {
                done.run(messageObject.messageOwner.originalLanguage);
            }
            return;
        }

        MessageKey key = new MessageKey(messageObject);
        if (detectingPhotos.contains(key)) {
            return;
        }
        detectingPhotos.add(key);

        LanguageDetector.detectLanguage(messageObject.messageOwner.message, lng -> AndroidUtilities.runOnUIThread(() -> {
            messageObject.messageOwner.originalLanguage = lng;
            getMessagesStorage().updateMessageCustomParams(key.dialogId, messageObject.messageOwner);
            detectingPhotos.remove(key);
            if (done != null) {
                done.run(lng);
            }
        }), err -> AndroidUtilities.runOnUIThread(() -> {
            messageObject.messageOwner.originalLanguage = UNKNOWN_LANGUAGE;
            getMessagesStorage().updateMessageCustomParams(key.dialogId, messageObject.messageOwner);
            detectingPhotos.remove(key);
            if (done != null) {
                done.run(UNKNOWN_LANGUAGE);
            }
        }));
    }

    public boolean canTranslatePhoto(MessageObject messageObject, String detectedLanguage) {
        if (messageObject != null && messageObject.messageOwner != null && messageObject.messageOwner.originalLanguage != null) {
            detectedLanguage = messageObject.messageOwner.originalLanguage;
        }
        return messageObject != null && messageObject.messageOwner != null && !TextUtils.isEmpty(messageObject.messageOwner.message) && (
            detectedLanguage == null && messageObject.messageOwner.translatedText != null && TextUtils.equals(messageObject.messageOwner.translatedToLanguage, TranslateAlert2.getToLanguage()) ||
            detectedLanguage != null && !Translator.isLanguageRestricted(messageObject.messageOwner.originalLanguage) &&
            !MessageHelper.isLinkOrEmojiOnlyMessage(messageObject)
        ) && !messageObject.translated;
    }

    public void translatePhoto(Context context, MessageObject messageObject, String captionDetectedLanguage, Runnable done) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }

        final MessageKey key = new MessageKey(messageObject);

        String toLang = TranslateAlert2.getToLanguage();

        if (messageObject.messageOwner.translatedText != null && TextUtils.equals(messageObject.messageOwner.translatedToLanguage, toLang)) {
            if (done != null) {
                done.run();
            }
            return;
        }
        if (translatingPhotos.contains(key)) {
            if (done != null) {
                done.run();
            }
            return;
        }

        translatingPhotos.add(key);

        final long start = System.currentTimeMillis();
        Translator.translate(messageObject.messageOwner.message, messageObject.messageOwner.entities, null, captionDetectedLanguage, null, new Translator.TranslateCallBack() {
            @Override
            public void onSuccess(TLRPC.TL_textWithEntities translation, String sourceLanguage, String targetLanguage) {
                messageObject.messageOwner.originalLanguage = sourceLanguage;
                messageObject.messageOwner.translatedToLanguage = targetLanguage;
                messageObject.messageOwner.translatedText = translation;
                getMessagesStorage().updateMessageCustomParams(key.dialogId, messageObject.messageOwner);
                translatingPhotos.remove(key);
                if (done != null) {
                    AndroidUtilities.runOnUIThread(done, Math.max(0, 400L - (System.currentTimeMillis() - start)));
                }
            }

            @Override
            public void onError(Throwable t) {
                Translator.handleTranslationError(context, t, () -> translatePhoto(context, messageObject, captionDetectedLanguage, done), new DarkThemeResourceProvider());
                translatingPhotos.remove(key);
                if (done != null) {
                    AndroidUtilities.runOnUIThread(done, Math.max(0, 400L - (System.currentTimeMillis() - start)));
                }
            }
        });
    }

    private static class MessageKey {
        public long dialogId;
        public int id;

        public MessageKey(MessageObject msg) {
            dialogId = msg.getDialogId();
            id = msg.getId();
        }
    }

    public static class PollText extends TLObject {
        public static final int constructor = 0x24953ab8;

        public TLRPC.TL_textWithEntities question;
        public ArrayList<TLRPC.PollAnswer> answers = new ArrayList<>();
        public TLRPC.TL_textWithEntities solution;

        public static PollText TLdeserialize(InputSerializedData stream, int constructor, boolean exception) {
            final PollText result = PollText.constructor != constructor ? null : new PollText();
            return TLdeserialize(PollText.class, result, stream, constructor, exception);
        }

        @Override
        public void readParams(InputSerializedData stream, boolean exception) {
            int flags = stream.readInt32(exception);
            if ((flags & 1) != 0) {
                question = TLRPC.TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
            if ((flags & 2) != 0) {
                answers = Vector.deserialize(stream, TLRPC.PollAnswer::TLdeserialize, exception);
            }
            if ((flags & 4) != 0) {
                solution = TLRPC.TL_textWithEntities.TLdeserialize(stream, stream.readInt32(exception), exception);
            }
        }

        @Override
        public void serializeToStream(OutputSerializedData stream) {
            stream.writeInt32(constructor);
            int flags = 0;
            if (question != null) {
                flags |= 1;
            }
            if (answers != null && !answers.isEmpty()) {
                flags |= 2;
            }
            if (solution != null) {
                flags |= 4;
            }
            stream.writeInt32(flags);
            if ((flags & 1) != 0) {
                question.serializeToStream(stream);
            }
            if ((flags & 2) != 0) {
                Vector.serialize(stream, answers);
            }
            if ((flags & 4) != 0) {
                solution.serializeToStream(stream);
            }
        }

        public int length() {
            int length = 0;
            if (question != null) {
                length += question.text.length();
            }
            for (int i = 0; i < answers.size(); ++i) {
                length += answers.get(i).text.text.length();
            }
            if (solution != null) {
                length += solution.text.length();
            }
            return length;
        }


        public static PollText fromMessage(MessageObject messageObject) {
            final TLRPC.MessageMedia media = MessageObject.getMedia(messageObject);
            if (media instanceof TLRPC.TL_messageMediaPoll) {
                return PollText.fromPoll((TLRPC.TL_messageMediaPoll) media);
            }
            return null;
        }

        public static PollText fromPoll(TLRPC.TL_messageMediaPoll mediaPoll) {
            final TLRPC.Poll poll = mediaPoll.poll;
            final PollText pollText = new PollText();
            pollText.question = poll.question;
            for (int i = 0; i < poll.answers.size(); ++i) {
                TLRPC.PollAnswer answer = poll.answers.get(i);
                TLRPC.TL_pollAnswer answerText = new TLRPC.TL_pollAnswer();
                answerText.text = answer.text;
                answerText.option = answer.option;
                pollText.answers.add(answerText);
            }
            if (mediaPoll.results != null && !TextUtils.isEmpty(mediaPoll.results.solution)) {
                pollText.solution = new TLRPC.TL_textWithEntities();
                pollText.solution.text = mediaPoll.results.solution;
                pollText.solution.entities = mediaPoll.results.solution_entities;
            }
            return pollText;
        }

        public static boolean isFullyTranslated(MessageObject messageObject, PollText b) {
            final TLRPC.MessageMedia media = MessageObject.getMedia(messageObject);
            final TLRPC.TL_messageMediaPoll poll;
            if (media instanceof TLRPC.TL_messageMediaPoll) {
                poll = (TLRPC.TL_messageMediaPoll) media;
            } else {
                return true;
            }
            if (poll.poll == null) {
                return true;
            }

            if ((poll.poll.question != null) != (b.question != null)) return false;
            if ((poll.results != null && poll.results.solution != null) != (b.solution != null)) return false;

            if (poll.poll.answers.size() != b.answers.size()) return false;

            return true;
        }
    }

//    private static DispatchQueue checkProgressQueue;
//    public static Runnable trackDownloadingProgress(String lng, Utilities.Callback<Float> onProgress) {
//        TranslateRemoteModel model = new TranslateRemoteModel.Builder(lng).build();
//        boolean[] loading = new boolean[1];
//        loading[0] = true;
//        Runnable[] checkProgress = new Runnable[1];
//        tryGetModelDownloadId(model, 0, downloadId -> {
//            if (!loading[0] || downloadId == null) {
//                return;
//            }
//            if (checkProgressQueue == null) {
//                checkProgressQueue = new DispatchQueue("translationModelsProgressCheck");
//            }
//
//            DownloadManager downloadManager = (DownloadManager) ApplicationLoader.applicationContext.getSystemService(Context.DOWNLOAD_SERVICE);
//            DownloadManager.Query query = new DownloadManager.Query();
//            query.setFilterById(downloadId);
//            (checkProgress[0] = () -> {
//                try {
//                    Cursor cursor = downloadManager.query(query);
//                    if (cursor.moveToFirst()) {
//                        int sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
//                        int downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
//                        long size = cursor.getInt(sizeIndex);
//                        long downloaded = cursor.getInt(downloadedIndex);
//                        FileLog.d("[mlkit] downloading " + lng + " model: " + ((float) (downloaded) / size * 100f) + "% (" + downloaded + "/" + size + ")");
//                        if (size != -1) {
//                            AndroidUtilities.runOnUIThread(() -> {
//                                onProgress.run((float) downloaded / size);
//                            });
//                        }
//                    }
//                    cursor.close();
//                    if (checkProgress[0] != null) {
//                        checkProgressQueue.postRunnable(checkProgress[0], 120);
//                    }
//                } catch (Exception e) {
//                    FileLog.e("[mlkit] failed to get percent", e);
//                }
//            }).run();
//        });
//        return () -> {
//            loading[0] = false;
//        };
//    }

//    private static void tryGetModelDownloadId(TranslateRemoteModel model, int tried, Utilities.Callback<Long> onDownloadId) {
//        if (onDownloadId == null || tried >= 5) {
//            return;
//        }
//        try {
//            Long downloadId = SharedPrefManager.getInstance(MlKitContext.getInstance()).getDownloadingModelId(model);
//            if (downloadId != null) {
//                onDownloadId.run(downloadId);
//                return;
//            }
//        } catch (Exception e) {
//            FileLog.e("[mlkit] failed to get model download id", e);
//        }
//        AndroidUtilities.runOnUIThread(
//            () -> tryGetModelDownloadId(model, tried + 1, onDownloadId),
//            25
//        );
//    }
//
//    private final ArrayList<String> downloadingModels = new ArrayList<>();
//    private final ArrayList<Float> downloadingModelsPercent = new ArrayList<>();
//    private final ArrayList<Runnable> cancelTrackingDownloadingModels = new ArrayList<>();
//
//    private Bulletin downloadingModelsBulletin;
//    private BaseFragment bulletinFragment;
//
//    private void checkModelInstalled(String lng, Runnable done) {
//        RemoteModelManager.getInstance()
//            .isModelDownloaded(new TranslateRemoteModel.Builder(lng).build())
//            .addOnSuccessListener(downloaded -> {
//                FileLog.d("[mlkit] isModelDownloaded " + lng + " = " + downloaded);
//                if (!downloaded) {
//                    trackModelDownloading(lng);
//                }
//                AndroidUtilities.runOnUIThread(done);
//            })
//            .addOnFailureListener(e -> {
//                FileLog.e("[mlkit] isModelDownloaded " + lng + " failed", e);
//                AndroidUtilities.runOnUIThread(done);
//            });
//    }
//
//    private void trackModelDownloading(String lng) {
//        final int index = downloadingModels.indexOf(lng);
//        if (index >= 0) return;
//
//        downloadingModels.add(lng);
//        downloadingModelsPercent.add(0.0f);
//        cancelTrackingDownloadingModels.add(trackDownloadingProgress(lng, percent -> {
//            final int index2 = downloadingModels.indexOf(lng);
//            if (index2 >= 0) {
//                downloadingModelsPercent.set(index2, percent);
//                updateDownloadingModelsBulletin();
//            }
//        }));
//    }
//
//    private void forceModelInstalled(String lng) {
//        final int index = downloadingModels.indexOf(lng);
//        if (index < 0) return;
//
//        downloadingModels.remove(index);
//        downloadingModelsPercent.remove(index);
//        cancelTrackingDownloadingModels.remove(index).run();
//
//        updateDownloadingModelsBulletin();
//    }
//
//    public void clearDownloadingModels() {
//        if (downloadingModels.isEmpty()) return;
//
//        for (int i = 0; i < cancelTrackingDownloadingModels.size(); ++i) {
//            cancelTrackingDownloadingModels.get(i).run();
//        }
//        downloadingModels.clear();
//        downloadingModelsPercent.clear();
//
//        updateDownloadingModelsBulletin();
//    }
//
//    private void updateDownloadingModelsBulletin() {
//        final BaseFragment lastFragment = LaunchActivity.getSafeLastFragment();
//        if (!(lastFragment instanceof ChatActivity) || downloadingModels.isEmpty()) {
//            if (downloadingModelsBulletin != null) {
//                downloadingModelsBulletin.hide();
//            }
//            downloadingModelsBulletin = null;
//            bulletinFragment = null;
//            return;
//        }
//
//        float total = 0.0f;
//        for (int i = 0; i < downloadingModelsPercent.size(); ++i) {
//            total += downloadingModelsPercent.get(i);
//        }
//
//        final ChatActivity chatActivity = ((ChatActivity) lastFragment);
//        if (downloadingModelsBulletin != null && chatActivity != bulletinFragment) {
//            downloadingModelsBulletin.hide();
//            downloadingModelsBulletin = null;
//            bulletinFragment = null;
//        }
//
//        if (!isTranslatingDialog(chatActivity.getDialogId())) {
//            if (downloadingModelsBulletin != null) {
//                downloadingModelsBulletin.hide();
//            }
//            downloadingModelsBulletin = null;
//            bulletinFragment = null;
//            return;
//        }
//
//        final ArrayList<String> languages = new ArrayList<>();
//        for (int i = 0; i < downloadingModels.size(); ++i) {
//            languages.add(TranslateAlert2.languageName(downloadingModels.get(i)));
//        }
//
//        final String title = LocaleController.getString(R.string.DownloadingTranslationModelsTitle);
//        final String subtitle = LocaleController.formatPluralString(
//            "DownloadingTranslationModelsText",
//            languages.size(),
//            languages.size() == 2 ?
//                LocaleController.formatString(R.string.DownloadingTranslationModelsTextAnd, languages.get(0), languages.get(1)) :
//                TextUtils.join(", ", languages)
//        );
//        final float percent = !downloadingModelsPercent.isEmpty() ? total / downloadingModelsPercent.size() : 1.0f;
//
//        if (downloadingModelsBulletin == null) {
//            final Bulletin.ProgressTwoLineAnimatedTitleLottieLayout layout = new Bulletin.ProgressTwoLineAnimatedTitleLottieLayout(chatActivity.getContext(), chatActivity.getResourceProvider());
//            layout.setAnimation(R.raw.msg_translate, 36, 36);
//            downloadingModelsBulletin =
//                BulletinFactory.of(bulletinFragment = chatActivity)
//                    .create(layout, Bulletin.DURATION_LONG);
//            downloadingModelsBulletin.setCanHide(false);
//            downloadingModelsBulletin.setCanHideOnShow = false;
//            downloadingModelsBulletin.show(true);
//        }
//
//        final Bulletin.ProgressTwoLineAnimatedTitleLottieLayout layout = (Bulletin.ProgressTwoLineAnimatedTitleLottieLayout) downloadingModelsBulletin.getLayout();
//        layout.titleTextView.setText(title);
//        layout.subtitleTextView.setText(subtitle);
//        layout.setProgress(percent);
//    }
}
