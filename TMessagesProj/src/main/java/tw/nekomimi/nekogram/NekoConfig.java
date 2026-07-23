package tw.nekomimi.nekogram;

import android.app.Activity;
import android.content.SharedPreferences;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.ui.ActionBar.Theme;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import tw.nekomimi.nekogram.helpers.AnalyticsHelper;
import tw.nekomimi.nekogram.helpers.CloudSettingsHelper;
import tw.nekomimi.nekogram.helpers.LensHelper;
import tw.nekomimi.nekogram.translator.Translator;
import tw.nekomimi.nekogram.translator.TranslatorApps;

public class NekoConfig {
    //TODO: refactor

    public static final int TITLE_TYPE_TEXT = 0;
    public static final int TITLE_TYPE_ICON = 1;
    public static final int TITLE_TYPE_MIX = 2;

    public static final int ID_TYPE_HIDDEN = 0;
    public static final int ID_TYPE_API = 1;
    public static final int ID_TYPE_BOTAPI = 2;

    public static final int TRANS_TYPE_NEKO = 0;
    public static final int TRANS_TYPE_TG = 1;
    public static final int TRANS_TYPE_EXTERNAL = 2;

    public static final int DOUBLE_TAP_ACTION_NONE = 0;
    public static final int DOUBLE_TAP_ACTION_REACTION = 1;
    public static final int DOUBLE_TAP_ACTION_TRANSLATE = 2;
    public static final int DOUBLE_TAP_ACTION_REPLY = 3;
    public static final int DOUBLE_TAP_ACTION_SAVE = 4;
    public static final int DOUBLE_TAP_ACTION_REPEAT = 5;
    public static final int DOUBLE_TAP_ACTION_EDIT = 6;

    public static final int TABLET_AUTO = 0;
    public static final int TABLET_ENABLE = 1;
    public static final int TABLET_DISABLE = 2;

    public static final int BOOST_NONE = 0;
    public static final int BOOST_AVERAGE = 1;
    public static final int BOOST_EXTREME = 2;

    public static final int TRANSCRIBE_AUTO = 0;
    public static final int TRANSCRIBE_PREMIUM = 1;
    public static final int TRANSCRIBE_WORKERSAI = 2;

    public static final int CAMERA_FRONT = 0;
    public static final int CAMERA_REAR = 1;
    public static final int CAMERA_ASK = 2;

    private static final Object sync = new Object();

    public static int cameraInVideoMessages = CAMERA_FRONT;
    public static int doubleTapInAction = DOUBLE_TAP_ACTION_REACTION;
    public static int doubleTapOutAction = DOUBLE_TAP_ACTION_REACTION;
    public static int downloadSpeedBoost = BOOST_NONE;
    public static int idType = ID_TYPE_API;
    public static int maxRecentStickers = 20;
    public static int nameOrder = 1;
    public static int tabletMode = TABLET_AUTO;
    public static int tabsTitleType = TITLE_TYPE_MIX;
    public static int transcribeProvider = TRANSCRIBE_PREMIUM;
    public static int transType = TRANS_TYPE_NEKO;

    public static float stickerSize = 14.0f;

    public static boolean accentAsNotificationColor = false;
    public static boolean askBeforeCall = true;
    public static boolean autoInlineBot = false;
    public static boolean autoPauseVideo = true;
    public static boolean autoTranslate = true;
    public static boolean bottomFilterTabs = false;
    public static boolean confirmAVMessage = false;
    public static boolean disableAppBarShadow = false;
    public static boolean disableGreetingSticker = false;
    public static boolean disableInstantCamera = false;
    public static boolean disableJumpToNextChannel = false;
    public static boolean disableMarkdownByDefault = false;
    public static boolean disableNumberRounding = false;
    public static boolean disableProximityEvents = false;
    public static boolean disableVoiceMessageAutoPlay = false;
    public static boolean forceFontWeightFallback = false;
    public static boolean formatTimeWithSeconds = false;
    public static boolean gooeyAvatarAnimation = true;
    public static boolean hideAllTab = false;
    public static boolean hideBottomNavigationBar = false;
    public static boolean hideChannelBottomButtons = false;
    public static boolean hideKeyboardOnChatScroll = false;
    public static boolean hideStories = false;
    public static boolean hideTimeOnSticker = false;
    public static boolean ignoreBlocked = false;
    public static boolean ignoreContentRestriction = false;
    public static boolean keepFormatting = true;
    public static boolean mapDriftingFix = false;
    public static boolean markdownParseLinks = true;
    public static boolean mediaPreview = true;
    public static boolean minimizedStickerCreator = false;
    public static boolean newMarkdownParser = true;
    public static boolean openArchiveOnPull = false;
    public static boolean predictiveBackAnimation = true;
    public static boolean preferIPv6 = false;
    public static boolean preferOriginalQuality = false;
    public static boolean quickForward = false;
    public static boolean reducedColors = false;
    public static boolean shouldNOTTrustMe = false;
    public static boolean showAddToSavedMessages = true;
    public static boolean showCopyPhoto = false;
    public static boolean showDeleteDownloadedFile = false;
    public static boolean showMessageDetails = false;
    public static boolean showNoQuoteForward = false;
    public static boolean showOpenIn = false;
    public static boolean showOriginal = true;
    public static boolean showPrPr = false;
    public static boolean showQrCode = false;
    public static boolean showRepeat = true;
    public static boolean showReport = false;
    public static boolean showRPCError = false;
    public static boolean showSetReminder = false;
    public static boolean showTimeHint = false;
    public static boolean showTranslate = true;
    public static boolean silenceNonContacts = false;
    public static boolean strokeOnViews = true;
    public static boolean tryToOpenAllLinksInIV = false;
    public static boolean unmuteVideosWithVolumeButtons = true;
    public static boolean useSystemEmoji = false;
    public static boolean voiceEnhancements = false;

    public static String cfAccountID = "";
    public static String cfApiToken = "";
    public static String externalTranslationProvider;
    public static String translationProvider = Translator.PROVIDER_GOOGLE;
    public static String translationTarget = "app";

    public static Set<String> restrictedLanguages;

    public static int userMcc = 0;

    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoconfig", Activity.MODE_PRIVATE);
    private static final SharedPreferences.OnSharedPreferenceChangeListener listener = (preferences, key) -> {
        var map = new HashMap<String, String>(1);
        map.put("key", key);
        AnalyticsHelper.trackEvent("neko_config_changed", map);

        CloudSettingsHelper.getInstance().doAutoSync();
    };
    private static boolean configLoaded;

    static {
        loadConfig(false);
    }

    public static void loadConfig(boolean force) {
        synchronized (sync) {
            if (configLoaded && !force) {
                return;
            }
            userMcc = ApplicationLoader.applicationContext.getResources().getConfiguration().mcc;

            cameraInVideoMessages = preferences.getInt("cameraInVideoMessages", CAMERA_FRONT);
            doubleTapInAction = preferences.getInt("doubleTapAction", DOUBLE_TAP_ACTION_REACTION);
            doubleTapOutAction = preferences.getInt("doubleTapOutAction", DOUBLE_TAP_ACTION_REACTION);
            downloadSpeedBoost = preferences.getInt("downloadSpeedBoost2", BOOST_NONE);
            idType = preferences.getInt("idType", ID_TYPE_API);
            maxRecentStickers = preferences.getInt("maxRecentStickers", 20);
            nameOrder = preferences.getInt("nameOrder", 1);
            tabletMode = preferences.getInt("tabletMode", TABLET_AUTO);
            tabsTitleType = preferences.getInt("tabsTitleType2", TITLE_TYPE_MIX);
            transcribeProvider = preferences.getInt("transcribeProvider", TRANSCRIBE_PREMIUM);
            transType = preferences.getInt("transType", TRANS_TYPE_NEKO);
            stickerSize = preferences.getFloat("stickerSize", 14.0f);
            accentAsNotificationColor = preferences.getBoolean("accentAsNotificationColor", false);
            askBeforeCall = preferences.getBoolean("askBeforeCall", true);
            autoInlineBot = preferences.getBoolean("autoInlineBot", false);
            autoPauseVideo = preferences.getBoolean("autoPauseVideo", true);
            autoTranslate = preferences.getBoolean("autoTranslate", true);
            bottomFilterTabs = preferences.getBoolean("bottomFilterTabs", false);
            confirmAVMessage = preferences.getBoolean("confirmAVMessage", false);
            disableAppBarShadow = preferences.getBoolean("disableAppBarShadow", false);
            disableGreetingSticker = preferences.getBoolean("disableGreetingSticker", false);
            disableInstantCamera = preferences.getBoolean("disableInstantCamera", false);
            disableJumpToNextChannel = preferences.getBoolean("disableJumpToNextChannel", false);
            disableMarkdownByDefault = preferences.getBoolean("disableMarkdownByDefault", false);
            disableNumberRounding = preferences.getBoolean("disableNumberRounding", false);
            disableProximityEvents = preferences.getBoolean("disableProximityEvents", false);
            disableVoiceMessageAutoPlay = preferences.getBoolean("disableVoiceMessageAutoPlay", false);
            forceFontWeightFallback = preferences.getBoolean("forceFontWeightFallback", false);
            formatTimeWithSeconds = preferences.getBoolean("formatTimeWithSeconds", false);
            gooeyAvatarAnimation = preferences.getBoolean("gooeyAvatarAnimation", true);
            hideAllTab = preferences.getBoolean("hideAllTab", false);
            hideBottomNavigationBar = preferences.getBoolean("hideBottomNavigationBar", false);
            hideChannelBottomButtons = preferences.getBoolean("hideChannelBottomButtons", false);
            hideKeyboardOnChatScroll = preferences.getBoolean("hideKeyboardOnChatScroll", false);
            hideStories = preferences.getBoolean("hideStories", false);
            hideTimeOnSticker = preferences.getBoolean("hideTimeOnSticker", false);
            ignoreBlocked = preferences.getBoolean("ignoreBlocked2", false);
            ignoreContentRestriction = preferences.getBoolean("ignoreContentRestriction", false);
            keepFormatting = preferences.getBoolean("keepFormatting", true);
            mapDriftingFix = preferences.getBoolean("mapDriftingFix", userMcc == 460);
            markdownParseLinks = preferences.getBoolean("markdownParseLinks", true);
            mediaPreview = preferences.getBoolean("mediaPreview", true);
            minimizedStickerCreator = preferences.getBoolean("minimizedStickerCreator", false);
            newMarkdownParser = preferences.getBoolean("newMarkdownParser", true);
            openArchiveOnPull = preferences.getBoolean("openArchiveOnPull", false);
            predictiveBackAnimation = preferences.getBoolean("predictiveBackAnimation", true);
            preferIPv6 = preferences.getBoolean("preferIPv6", false);
            preferOriginalQuality = preferences.getBoolean("preferOriginalQuality", false);
            quickForward = preferences.getBoolean("quickForward", false);
            reducedColors = preferences.getBoolean("reducedColors", false);
            shouldNOTTrustMe = preferences.getBoolean("shouldNOTTrustMe", false);
            showAddToSavedMessages = preferences.getBoolean("showAddToSavedMessages", true);
            showCopyPhoto = preferences.getBoolean("showCopyPhoto", false);
            showDeleteDownloadedFile = preferences.getBoolean("showDeleteDownloadedFile", false);
            showMessageDetails = preferences.getBoolean("showMessageDetails", false);
            showNoQuoteForward = preferences.getBoolean("showNoQuoteForward", false);
            showOpenIn = preferences.getBoolean("showOpenIn", false);
            showOriginal = preferences.getBoolean("showOriginal", true);
            showPrPr = preferences.getBoolean("showPrPr", false);
            showQrCode = preferences.getBoolean("showQrCode", false);
            showRepeat = preferences.getBoolean("showRepeat", true);
            showReport = preferences.getBoolean("showReport", false);
            showRPCError = preferences.getBoolean("showRPCError", false);
            showSetReminder = preferences.getBoolean("showSetReminder", false);
            showTimeHint = preferences.getBoolean("showTimeHint", false);
            showTranslate = preferences.getBoolean("showTranslate", true);
            silenceNonContacts = preferences.getBoolean("silenceNonContacts", false);
            strokeOnViews = preferences.getBoolean("strokeOnViews", true);
            tryToOpenAllLinksInIV = preferences.getBoolean("tryToOpenAllLinksInIV", false);
            unmuteVideosWithVolumeButtons = preferences.getBoolean("unmuteVideosWithVolumeButtons", true);
            useSystemEmoji = preferences.getBoolean("useSystemEmoji", false);
            voiceEnhancements = preferences.getBoolean("voiceEnhancements", false);
            cfAccountID = preferences.getString("cfAccountID", "");
            cfApiToken = preferences.getString("cfApiToken", "");
            externalTranslationProvider = preferences.getString("externalTranslationProvider", null);
            translationProvider = preferences.getString("translationProvider2", Translator.PROVIDER_GOOGLE);
            translationTarget = preferences.getString("translationTarget", "app");
            restrictedLanguages = preferences.getStringSet("restrictedLanguages", null);

            TranslatorApps.loadTranslatorAppsAsync();
            LensHelper.checkLensSupportAsync();
            preferences.registerOnSharedPreferenceChangeListener(listener);

            if (!configLoaded) {
                var map = new HashMap<String, String>();
                map.put("buildType", BuildConfig.BUILD_TYPE);
                map.put("mcc", String.valueOf(userMcc));
                AnalyticsHelper.trackEvent("load_config", map);
            }
            configLoaded = true;
        }
    }

    public static String exportConfigs() {
        var object = new JsonObject();
        if (preferences.contains("cameraInVideoMessages")) {
            object.addProperty("cameraInVideoMessages", preferences.getInt("cameraInVideoMessages", CAMERA_FRONT));
        }
        if (preferences.contains("doubleTapAction")) {
            object.addProperty("doubleTapAction", preferences.getInt("doubleTapAction", DOUBLE_TAP_ACTION_REACTION));
        }
        if (preferences.contains("doubleTapOutAction")) {
            object.addProperty("doubleTapOutAction", preferences.getInt("doubleTapOutAction", DOUBLE_TAP_ACTION_REACTION));
        }
        if (preferences.contains("downloadSpeedBoost2")) {
            object.addProperty("downloadSpeedBoost2", preferences.getInt("downloadSpeedBoost2", BOOST_NONE));
        }
        if (preferences.contains("idType")) {
            object.addProperty("idType", preferences.getInt("idType", ID_TYPE_API));
        }
        if (preferences.contains("maxRecentStickers")) {
            object.addProperty("maxRecentStickers", preferences.getInt("maxRecentStickers", 20));
        }
        if (preferences.contains("nameOrder")) {
            object.addProperty("nameOrder", preferences.getInt("nameOrder", 1));
        }
        if (preferences.contains("tabletMode")) {
            object.addProperty("tabletMode", preferences.getInt("tabletMode", TABLET_AUTO));
        }
        if (preferences.contains("tabsTitleType2")) {
            object.addProperty("tabsTitleType2", preferences.getInt("tabsTitleType2", TITLE_TYPE_MIX));
        }
        if (preferences.contains("transcribeProvider")) {
            object.addProperty("transcribeProvider", preferences.getInt("transcribeProvider", TRANSCRIBE_PREMIUM));
        }
        if (preferences.contains("transType")) {
            object.addProperty("transType", preferences.getInt("transType", TRANS_TYPE_NEKO));
        }
        if (preferences.contains("stickerSize")) {
            object.addProperty("stickerSize", preferences.getFloat("stickerSize", 14.0f));
        }
        if (preferences.contains("accentAsNotificationColor")) {
            object.addProperty("accentAsNotificationColor", preferences.getBoolean("accentAsNotificationColor", false));
        }
        if (preferences.contains("askBeforeCall")) {
            object.addProperty("askBeforeCall", preferences.getBoolean("askBeforeCall", true));
        }
        if (preferences.contains("autoInlineBot")) {
            object.addProperty("autoInlineBot", preferences.getBoolean("autoInlineBot", false));
        }
        if (preferences.contains("autoPauseVideo")) {
            object.addProperty("autoPauseVideo", preferences.getBoolean("autoPauseVideo", true));
        }
        if (preferences.contains("autoTranslate")) {
            object.addProperty("autoTranslate", preferences.getBoolean("autoTranslate", true));
        }
        if (preferences.contains("bottomFilterTabs")) {
            object.addProperty("bottomFilterTabs", preferences.getBoolean("bottomFilterTabs", false));
        }
        if (preferences.contains("confirmAVMessage")) {
            object.addProperty("confirmAVMessage", preferences.getBoolean("confirmAVMessage", false));
        }
        if (preferences.contains("disableAppBarShadow")) {
            object.addProperty("disableAppBarShadow", preferences.getBoolean("disableAppBarShadow", false));
        }
        if (preferences.contains("disableGreetingSticker")) {
            object.addProperty("disableGreetingSticker", preferences.getBoolean("disableGreetingSticker", false));
        }
        if (preferences.contains("disableInstantCamera")) {
            object.addProperty("disableInstantCamera", preferences.getBoolean("disableInstantCamera", false));
        }
        if (preferences.contains("disableJumpToNextChannel")) {
            object.addProperty("disableJumpToNextChannel", preferences.getBoolean("disableJumpToNextChannel", false));
        }
        if (preferences.contains("disableMarkdownByDefault")) {
            object.addProperty("disableMarkdownByDefault", preferences.getBoolean("disableMarkdownByDefault", false));
        }
        if (preferences.contains("disableNumberRounding")) {
            object.addProperty("disableNumberRounding", preferences.getBoolean("disableNumberRounding", false));
        }
        if (preferences.contains("disableProximityEvents")) {
            object.addProperty("disableProximityEvents", preferences.getBoolean("disableProximityEvents", false));
        }
        if (preferences.contains("disableVoiceMessageAutoPlay")) {
            object.addProperty("disableVoiceMessageAutoPlay", preferences.getBoolean("disableVoiceMessageAutoPlay", false));
        }
        if (preferences.contains("forceFontWeightFallback")) {
            object.addProperty("forceFontWeightFallback", preferences.getBoolean("forceFontWeightFallback", false));
        }
        if (preferences.contains("formatTimeWithSeconds")) {
            object.addProperty("formatTimeWithSeconds", preferences.getBoolean("formatTimeWithSeconds", false));
        }
        if (preferences.contains("gooeyAvatarAnimation")) {
            object.addProperty("gooeyAvatarAnimation", preferences.getBoolean("gooeyAvatarAnimation", true));
        }
        if (preferences.contains("hideAllTab")) {
            object.addProperty("hideAllTab", preferences.getBoolean("hideAllTab", false));
        }
        if (preferences.contains("hideBottomNavigationBar")) {
            object.addProperty("hideBottomNavigationBar", preferences.getBoolean("hideBottomNavigationBar", false));
        }
        if (preferences.contains("hideChannelBottomButtons")) {
            object.addProperty("hideChannelBottomButtons", preferences.getBoolean("hideChannelBottomButtons", false));
        }
        if (preferences.contains("hideKeyboardOnChatScroll")) {
            object.addProperty("hideKeyboardOnChatScroll", preferences.getBoolean("hideKeyboardOnChatScroll", false));
        }
        if (preferences.contains("hideStories")) {
            object.addProperty("hideStories", preferences.getBoolean("hideStories", false));
        }
        if (preferences.contains("hideTimeOnSticker")) {
            object.addProperty("hideTimeOnSticker", preferences.getBoolean("hideTimeOnSticker", false));
        }
        if (preferences.contains("ignoreBlocked2")) {
            object.addProperty("ignoreBlocked2", preferences.getBoolean("ignoreBlocked2", false));
        }
        if (preferences.contains("ignoreContentRestriction")) {
            object.addProperty("ignoreContentRestriction", preferences.getBoolean("ignoreContentRestriction", false));
        }
        if (preferences.contains("keepFormatting")) {
            object.addProperty("keepFormatting", preferences.getBoolean("keepFormatting", true));
        }
        if (preferences.contains("mapDriftingFix")) {
            object.addProperty("mapDriftingFix", preferences.getBoolean("mapDriftingFix", false));
        }
        if (preferences.contains("markdownParseLinks")) {
            object.addProperty("markdownParseLinks", preferences.getBoolean("markdownParseLinks", true));
        }
        if (preferences.contains("mediaPreview")) {
            object.addProperty("mediaPreview", preferences.getBoolean("mediaPreview", true));
        }
        if (preferences.contains("minimizedStickerCreator")) {
            object.addProperty("minimizedStickerCreator", preferences.getBoolean("minimizedStickerCreator", false));
        }
        if (preferences.contains("newMarkdownParser")) {
            object.addProperty("newMarkdownParser", preferences.getBoolean("newMarkdownParser", true));
        }
        if (preferences.contains("openArchiveOnPull")) {
            object.addProperty("openArchiveOnPull", preferences.getBoolean("openArchiveOnPull", false));
        }
        if (preferences.contains("predictiveBackAnimation")) {
            object.addProperty("predictiveBackAnimation", preferences.getBoolean("predictiveBackAnimation", true));
        }
        if (preferences.contains("preferIPv6")) {
            object.addProperty("preferIPv6", preferences.getBoolean("preferIPv6", false));
        }
        if (preferences.contains("preferOriginalQuality")) {
            object.addProperty("preferOriginalQuality", preferences.getBoolean("preferOriginalQuality", false));
        }
        if (preferences.contains("quickForward")) {
            object.addProperty("quickForward", preferences.getBoolean("quickForward", false));
        }
        if (preferences.contains("reducedColors")) {
            object.addProperty("reducedColors", preferences.getBoolean("reducedColors", false));
        }
        if (preferences.contains("shouldNOTTrustMe")) {
            object.addProperty("shouldNOTTrustMe", preferences.getBoolean("shouldNOTTrustMe", false));
        }
        if (preferences.contains("showAddToSavedMessages")) {
            object.addProperty("showAddToSavedMessages", preferences.getBoolean("showAddToSavedMessages", true));
        }
        if (preferences.contains("showCopyPhoto")) {
            object.addProperty("showCopyPhoto", preferences.getBoolean("showCopyPhoto", false));
        }
        if (preferences.contains("showDeleteDownloadedFile")) {
            object.addProperty("showDeleteDownloadedFile", preferences.getBoolean("showDeleteDownloadedFile", false));
        }
        if (preferences.contains("showMessageDetails")) {
            object.addProperty("showMessageDetails", preferences.getBoolean("showMessageDetails", false));
        }
        if (preferences.contains("showNoQuoteForward")) {
            object.addProperty("showNoQuoteForward", preferences.getBoolean("showNoQuoteForward", false));
        }
        if (preferences.contains("showOpenIn")) {
            object.addProperty("showOpenIn", preferences.getBoolean("showOpenIn", false));
        }
        if (preferences.contains("showOriginal")) {
            object.addProperty("showOriginal", preferences.getBoolean("showOriginal", true));
        }
        if (preferences.contains("showPrPr")) {
            object.addProperty("showPrPr", preferences.getBoolean("showPrPr", false));
        }
        if (preferences.contains("showQrCode")) {
            object.addProperty("showQrCode", preferences.getBoolean("showQrCode", false));
        }
        if (preferences.contains("showRepeat")) {
            object.addProperty("showRepeat", preferences.getBoolean("showRepeat", true));
        }
        if (preferences.contains("showReport")) {
            object.addProperty("showReport", preferences.getBoolean("showReport", false));
        }
        if (preferences.contains("showRPCError")) {
            object.addProperty("showRPCError", preferences.getBoolean("showRPCError", false));
        }
        if (preferences.contains("showSetReminder")) {
            object.addProperty("showSetReminder", preferences.getBoolean("showSetReminder", false));
        }
        if (preferences.contains("showTimeHint")) {
            object.addProperty("showTimeHint", preferences.getBoolean("showTimeHint", false));
        }
        if (preferences.contains("showTranslate")) {
            object.addProperty("showTranslate", preferences.getBoolean("showTranslate", true));
        }
        if (preferences.contains("silenceNonContacts")) {
            object.addProperty("silenceNonContacts", preferences.getBoolean("silenceNonContacts", false));
        }
        if (preferences.contains("strokeOnViews")) {
            object.addProperty("strokeOnViews", preferences.getBoolean("strokeOnViews", true));
        }
        if (preferences.contains("tryToOpenAllLinksInIV")) {
            object.addProperty("tryToOpenAllLinksInIV", preferences.getBoolean("tryToOpenAllLinksInIV", false));
        }
        if (preferences.contains("unmuteVideosWithVolumeButtons")) {
            object.addProperty("unmuteVideosWithVolumeButtons", preferences.getBoolean("unmuteVideosWithVolumeButtons", true));
        }
        if (preferences.contains("useSystemEmoji")) {
            object.addProperty("useSystemEmoji", preferences.getBoolean("useSystemEmoji", false));
        }
        if (preferences.contains("voiceEnhancements")) {
            object.addProperty("voiceEnhancements", preferences.getBoolean("voiceEnhancements", false));
        }
        if (preferences.contains("cfAccountID")) {
            object.addProperty("cfAccountID", preferences.getString("cfAccountID", ""));
        }
        if (preferences.contains("cfApiToken")) {
            object.addProperty("cfApiToken", preferences.getString("cfApiToken", ""));
        }
        if (preferences.contains("externalTranslationProvider")) {
            object.addProperty("externalTranslationProvider", preferences.getString("externalTranslationProvider", null));
        }
        if (preferences.contains("translationProvider2")) {
            object.addProperty("translationProvider2", preferences.getString("translationProvider2", Translator.PROVIDER_GOOGLE));
        }
        if (preferences.contains("translationTarget")) {
            object.addProperty("translationTarget", preferences.getString("translationTarget", "app"));
        }
        if (preferences.contains("restrictedLanguages")) {
            var array = new JsonArray();
            for (var language : preferences.getStringSet("restrictedLanguages", Collections.emptySet())) {
                array.add(language);
            }
            object.add("restrictedLanguages", array);
        }

        return object.toString();
    }

    public static void importConfigs(String config) {
        var map = JsonParser.parseString(config);
        if (!map.isJsonObject()) {
            throw new IllegalStateException("INVALID_BACKUP");
        }
        var object = map.getAsJsonObject();
        preferences.unregisterOnSharedPreferenceChangeListener(listener);
        var editor = preferences.edit();
        editor.clear();
        if (object.has("cameraInVideoMessages")) {
            editor.putInt("cameraInVideoMessages", object.get("cameraInVideoMessages").getAsInt());
        }
        if (object.has("doubleTapAction")) {
            editor.putInt("doubleTapAction", object.get("doubleTapAction").getAsInt());
        }
        if (object.has("doubleTapOutAction")) {
            editor.putInt("doubleTapOutAction", object.get("doubleTapOutAction").getAsInt());
        }
        if (object.has("downloadSpeedBoost2")) {
            editor.putInt("downloadSpeedBoost2", object.get("downloadSpeedBoost2").getAsInt());
        }
        if (object.has("idType")) {
            editor.putInt("idType", object.get("idType").getAsInt());
        }
        if (object.has("maxRecentStickers")) {
            editor.putInt("maxRecentStickers", object.get("maxRecentStickers").getAsInt());
        }
        if (object.has("nameOrder")) {
            editor.putInt("nameOrder", object.get("nameOrder").getAsInt());
        }
        if (object.has("tabletMode")) {
            editor.putInt("tabletMode", object.get("tabletMode").getAsInt());
        }
        if (object.has("tabsTitleType2")) {
            editor.putInt("tabsTitleType2", object.get("tabsTitleType2").getAsInt());
        }
        if (object.has("transcribeProvider")) {
            editor.putInt("transcribeProvider", object.get("transcribeProvider").getAsInt());
        }
        if (object.has("transType")) {
            editor.putInt("transType", object.get("transType").getAsInt());
        }
        if (object.has("stickerSize")) {
            editor.putFloat("stickerSize", object.get("stickerSize").getAsFloat());
        }
        if (object.has("accentAsNotificationColor")) {
            editor.putBoolean("accentAsNotificationColor", object.get("accentAsNotificationColor").getAsBoolean());
        }
        if (object.has("askBeforeCall")) {
            editor.putBoolean("askBeforeCall", object.get("askBeforeCall").getAsBoolean());
        }
        if (object.has("autoInlineBot")) {
            editor.putBoolean("autoInlineBot", object.get("autoInlineBot").getAsBoolean());
        }
        if (object.has("autoPauseVideo")) {
            editor.putBoolean("autoPauseVideo", object.get("autoPauseVideo").getAsBoolean());
        }
        if (object.has("autoTranslate")) {
            editor.putBoolean("autoTranslate", object.get("autoTranslate").getAsBoolean());
        }
        if (object.has("bottomFilterTabs")) {
            editor.putBoolean("bottomFilterTabs", object.get("bottomFilterTabs").getAsBoolean());
        }
        if (object.has("confirmAVMessage")) {
            editor.putBoolean("confirmAVMessage", object.get("confirmAVMessage").getAsBoolean());
        }
        if (object.has("disableAppBarShadow")) {
            editor.putBoolean("disableAppBarShadow", object.get("disableAppBarShadow").getAsBoolean());
        }
        if (object.has("disableGreetingSticker")) {
            editor.putBoolean("disableGreetingSticker", object.get("disableGreetingSticker").getAsBoolean());
        }
        if (object.has("disableInstantCamera")) {
            editor.putBoolean("disableInstantCamera", object.get("disableInstantCamera").getAsBoolean());
        }
        if (object.has("disableJumpToNextChannel")) {
            editor.putBoolean("disableJumpToNextChannel", object.get("disableJumpToNextChannel").getAsBoolean());
        }
        if (object.has("disableMarkdownByDefault")) {
            editor.putBoolean("disableMarkdownByDefault", object.get("disableMarkdownByDefault").getAsBoolean());
        }
        if (object.has("disableNumberRounding")) {
            editor.putBoolean("disableNumberRounding", object.get("disableNumberRounding").getAsBoolean());
        }
        if (object.has("disableProximityEvents")) {
            editor.putBoolean("disableProximityEvents", object.get("disableProximityEvents").getAsBoolean());
        }
        if (object.has("disableVoiceMessageAutoPlay")) {
            editor.putBoolean("disableVoiceMessageAutoPlay", object.get("disableVoiceMessageAutoPlay").getAsBoolean());
        }
        if (object.has("forceFontWeightFallback")) {
            editor.putBoolean("forceFontWeightFallback", object.get("forceFontWeightFallback").getAsBoolean());
        }
        if (object.has("formatTimeWithSeconds")) {
            editor.putBoolean("formatTimeWithSeconds", object.get("formatTimeWithSeconds").getAsBoolean());
        }
        if (object.has("gooeyAvatarAnimation")) {
            editor.putBoolean("gooeyAvatarAnimation", object.get("gooeyAvatarAnimation").getAsBoolean());
        }
        if (object.has("hideAllTab")) {
            editor.putBoolean("hideAllTab", object.get("hideAllTab").getAsBoolean());
        }
        if (object.has("hideBottomNavigationBar")) {
            editor.putBoolean("hideBottomNavigationBar", object.get("hideBottomNavigationBar").getAsBoolean());
        }
        if (object.has("hideChannelBottomButtons")) {
            editor.putBoolean("hideChannelBottomButtons", object.get("hideChannelBottomButtons").getAsBoolean());
        }
        if (object.has("hideKeyboardOnChatScroll")) {
            editor.putBoolean("hideKeyboardOnChatScroll", object.get("hideKeyboardOnChatScroll").getAsBoolean());
        }
        if (object.has("hideStories")) {
            editor.putBoolean("hideStories", object.get("hideStories").getAsBoolean());
        }
        if (object.has("hideTimeOnSticker")) {
            editor.putBoolean("hideTimeOnSticker", object.get("hideTimeOnSticker").getAsBoolean());
        }
        if (object.has("ignoreBlocked2")) {
            editor.putBoolean("ignoreBlocked2", object.get("ignoreBlocked2").getAsBoolean());
        }
        if (object.has("ignoreContentRestriction")) {
            editor.putBoolean("ignoreContentRestriction", object.get("ignoreContentRestriction").getAsBoolean());
        }
        if (object.has("keepFormatting")) {
            editor.putBoolean("keepFormatting", object.get("keepFormatting").getAsBoolean());
        }
        if (object.has("mapDriftingFix")) {
            editor.putBoolean("mapDriftingFix", object.get("mapDriftingFix").getAsBoolean());
        }
        if (object.has("markdownParseLinks")) {
            editor.putBoolean("markdownParseLinks", object.get("markdownParseLinks").getAsBoolean());
        }
        if (object.has("mediaPreview")) {
            editor.putBoolean("mediaPreview", object.get("mediaPreview").getAsBoolean());
        }
        if (object.has("minimizedStickerCreator")) {
            editor.putBoolean("minimizedStickerCreator", object.get("minimizedStickerCreator").getAsBoolean());
        }
        if (object.has("newMarkdownParser")) {
            editor.putBoolean("newMarkdownParser", object.get("newMarkdownParser").getAsBoolean());
        }
        if (object.has("openArchiveOnPull")) {
            editor.putBoolean("openArchiveOnPull", object.get("openArchiveOnPull").getAsBoolean());
        }
        if (object.has("predictiveBackAnimation")) {
            editor.putBoolean("predictiveBackAnimation", object.get("predictiveBackAnimation").getAsBoolean());
        }
        if (object.has("preferIPv6")) {
            editor.putBoolean("preferIPv6", object.get("preferIPv6").getAsBoolean());
        }
        if (object.has("preferOriginalQuality")) {
            editor.putBoolean("preferOriginalQuality", object.get("preferOriginalQuality").getAsBoolean());
        }
        if (object.has("quickForward")) {
            editor.putBoolean("quickForward", object.get("quickForward").getAsBoolean());
        }
        if (object.has("reducedColors")) {
            editor.putBoolean("reducedColors", object.get("reducedColors").getAsBoolean());
        }
        if (object.has("shouldNOTTrustMe")) {
            editor.putBoolean("shouldNOTTrustMe", object.get("shouldNOTTrustMe").getAsBoolean());
        }
        if (object.has("showAddToSavedMessages")) {
            editor.putBoolean("showAddToSavedMessages", object.get("showAddToSavedMessages").getAsBoolean());
        }
        if (object.has("showCopyPhoto")) {
            editor.putBoolean("showCopyPhoto", object.get("showCopyPhoto").getAsBoolean());
        }
        if (object.has("showDeleteDownloadedFile")) {
            editor.putBoolean("showDeleteDownloadedFile", object.get("showDeleteDownloadedFile").getAsBoolean());
        }
        if (object.has("showMessageDetails")) {
            editor.putBoolean("showMessageDetails", object.get("showMessageDetails").getAsBoolean());
        }
        if (object.has("showNoQuoteForward")) {
            editor.putBoolean("showNoQuoteForward", object.get("showNoQuoteForward").getAsBoolean());
        }
        if (object.has("showOpenIn")) {
            editor.putBoolean("showOpenIn", object.get("showOpenIn").getAsBoolean());
        }
        if (object.has("showOriginal")) {
            editor.putBoolean("showOriginal", object.get("showOriginal").getAsBoolean());
        }
        if (object.has("showPrPr")) {
            editor.putBoolean("showPrPr", object.get("showPrPr").getAsBoolean());
        }
        if (object.has("showQrCode")) {
            editor.putBoolean("showQrCode", object.get("showQrCode").getAsBoolean());
        }
        if (object.has("showRepeat")) {
            editor.putBoolean("showRepeat", object.get("showRepeat").getAsBoolean());
        }
        if (object.has("showReport")) {
            editor.putBoolean("showReport", object.get("showReport").getAsBoolean());
        }
        if (object.has("showRPCError")) {
            editor.putBoolean("showRPCError", object.get("showRPCError").getAsBoolean());
        }
        if (object.has("showSetReminder")) {
            editor.putBoolean("showSetReminder", object.get("showSetReminder").getAsBoolean());
        }
        if (object.has("showTimeHint")) {
            editor.putBoolean("showTimeHint", object.get("showTimeHint").getAsBoolean());
        }
        if (object.has("showTranslate")) {
            editor.putBoolean("showTranslate", object.get("showTranslate").getAsBoolean());
        }
        if (object.has("silenceNonContacts")) {
            editor.putBoolean("silenceNonContacts", object.get("silenceNonContacts").getAsBoolean());
        }
        if (object.has("strokeOnViews")) {
            editor.putBoolean("strokeOnViews", object.get("strokeOnViews").getAsBoolean());
        }
        if (object.has("tryToOpenAllLinksInIV")) {
            editor.putBoolean("tryToOpenAllLinksInIV", object.get("tryToOpenAllLinksInIV").getAsBoolean());
        }
        if (object.has("unmuteVideosWithVolumeButtons")) {
            editor.putBoolean("unmuteVideosWithVolumeButtons", object.get("unmuteVideosWithVolumeButtons").getAsBoolean());
        }
        if (object.has("useSystemEmoji")) {
            editor.putBoolean("useSystemEmoji", object.get("useSystemEmoji").getAsBoolean());
        }
        if (object.has("voiceEnhancements")) {
            editor.putBoolean("voiceEnhancements", object.get("voiceEnhancements").getAsBoolean());
        }
        if (object.has("cfAccountID")) {
            editor.putString("cfAccountID", object.get("cfAccountID").getAsString());
        }
        if (object.has("cfApiToken")) {
            editor.putString("cfApiToken", object.get("cfApiToken").getAsString());
        }
        if (object.has("externalTranslationProvider")) {
            editor.putString("externalTranslationProvider", object.get("externalTranslationProvider").getAsString());
        }
        if (object.has("translationProvider2")) {
            editor.putString("translationProvider2", object.get("translationProvider2").getAsString());
        }
        if (object.has("translationTarget")) {
            editor.putString("translationTarget", object.get("translationTarget").getAsString());
        }
        if (object.has("restrictedLanguages")) {
            var set = new HashSet<String>();
            var array = object.get("restrictedLanguages").getAsJsonArray();
            for (var element : array) {
                set.add(element.getAsString());
            }
            editor.putStringSet("restrictedLanguages", set);
        }

        editor.apply();
        loadConfig(true);
    }

    public static void setCameraInVideoMessages(int value) {
        cameraInVideoMessages = value;
        preferences.edit().putInt("cameraInVideoMessages", value).apply();
    }

    public static void setDoubleTapInAction(int value) {
        doubleTapInAction = value;
        preferences.edit().putInt("doubleTapAction", value).apply();
    }

    public static void setDoubleTapOutAction(int value) {
        doubleTapOutAction = value;
        preferences.edit().putInt("doubleTapOutAction", value).apply();
    }

    public static void setDownloadSpeedBoost(int value) {
        downloadSpeedBoost = value;
        preferences.edit().putInt("downloadSpeedBoost2", value).apply();
    }

    public static void setIdType(int value) {
        idType = value;
        preferences.edit().putInt("idType", value).apply();
    }

    public static void setMaxRecentStickers(int value) {
        maxRecentStickers = value;
        preferences.edit().putInt("maxRecentStickers", value).apply();
    }

    public static void setNameOrder(int value) {
        nameOrder = value;
        preferences.edit().putInt("nameOrder", value).apply();
    }

    public static void setTabletMode(int value) {
        tabletMode = value;
        preferences.edit().putInt("tabletMode", value).apply();
    }

    public static void setTabsTitleType(int value) {
        tabsTitleType = value;
        preferences.edit().putInt("tabsTitleType2", value).apply();
    }

    public static void setTranscribeProvider(int value) {
        transcribeProvider = value;
        preferences.edit().putInt("transcribeProvider", value).apply();
    }

    public static void setTransType(int value) {
        transType = value;
        preferences.edit().putInt("transType", value).apply();
    }

    public static void setStickerSize(float value) {
        stickerSize = value;
        preferences.edit().putFloat("stickerSize", value).apply();
    }

    public static void toggleAccentAsNotificationColor() {
        accentAsNotificationColor = !accentAsNotificationColor;
        preferences.edit().putBoolean("accentAsNotificationColor", accentAsNotificationColor).apply();
    }

    public static void toggleAskBeforeCall() {
        askBeforeCall = !askBeforeCall;
        preferences.edit().putBoolean("askBeforeCall", askBeforeCall).apply();
    }

    public static void toggleAutoInlineBot() {
        autoInlineBot = !autoInlineBot;
        preferences.edit().putBoolean("autoInlineBot", autoInlineBot).apply();
    }

    public static void toggleAutoPauseVideo() {
        autoPauseVideo = !autoPauseVideo;
        preferences.edit().putBoolean("autoPauseVideo", autoPauseVideo).apply();
    }

    public static void toggleAutoTranslate() {
        autoTranslate = !autoTranslate;
        preferences.edit().putBoolean("autoTranslate", autoTranslate).apply();
    }

    public static void setBottomFilterTabs(boolean value) {
        bottomFilterTabs = value;
        preferences.edit().putBoolean("bottomFilterTabs", value).apply();
    }

    public static void toggleConfirmAVMessage() {
        confirmAVMessage = !confirmAVMessage;
        preferences.edit().putBoolean("confirmAVMessage", confirmAVMessage).apply();
    }

    public static void toggleDisableAppBarShadow() {
        disableAppBarShadow = !disableAppBarShadow;
        preferences.edit().putBoolean("disableAppBarShadow", disableAppBarShadow).apply();
    }

    public static void toggleDisableGreetingSticker() {
        disableGreetingSticker = !disableGreetingSticker;
        preferences.edit().putBoolean("disableGreetingSticker", disableGreetingSticker).apply();
    }

    public static void toggleDisableInstantCamera() {
        disableInstantCamera = !disableInstantCamera;
        preferences.edit().putBoolean("disableInstantCamera", disableInstantCamera).apply();
    }

    public static void toggleDisableJumpToNextChannel() {
        disableJumpToNextChannel = !disableJumpToNextChannel;
        preferences.edit().putBoolean("disableJumpToNextChannel", disableJumpToNextChannel).apply();
    }

    public static void toggleDisableMarkdownByDefault() {
        disableMarkdownByDefault = !disableMarkdownByDefault;
        preferences.edit().putBoolean("disableMarkdownByDefault", disableMarkdownByDefault).apply();
    }

    public static void toggleDisableNumberRounding() {
        disableNumberRounding = !disableNumberRounding;
        preferences.edit().putBoolean("disableNumberRounding", disableNumberRounding).apply();
    }

    public static void toggleDisableProximityEvents() {
        disableProximityEvents = !disableProximityEvents;
        preferences.edit().putBoolean("disableProximityEvents", disableProximityEvents).apply();
    }

    public static void toggleDisableVoiceMessageAutoPlay() {
        disableVoiceMessageAutoPlay = !disableVoiceMessageAutoPlay;
        preferences.edit().putBoolean("disableVoiceMessageAutoPlay", disableVoiceMessageAutoPlay).apply();
    }

    public static void toggleForceFontWeightFallback() {
        forceFontWeightFallback = !forceFontWeightFallback;
        preferences.edit().putBoolean("forceFontWeightFallback", forceFontWeightFallback).apply();
    }

    public static void toggleFormatTimeWithSeconds() {
        formatTimeWithSeconds = !formatTimeWithSeconds;
        preferences.edit().putBoolean("formatTimeWithSeconds", formatTimeWithSeconds).apply();
    }

    public static void toggleGooeyAvatarAnimation() {
        gooeyAvatarAnimation = !gooeyAvatarAnimation;
        preferences.edit().putBoolean("gooeyAvatarAnimation", gooeyAvatarAnimation).apply();
    }

    public static void toggleHideAllTab() {
        hideAllTab = !hideAllTab;
        preferences.edit().putBoolean("hideAllTab", hideAllTab).apply();
    }

    public static void toggleHideBottomNavigationBar() {
        hideBottomNavigationBar = !hideBottomNavigationBar;
        preferences.edit().putBoolean("hideBottomNavigationBar", hideBottomNavigationBar).apply();
    }

    public static void toggleHideChannelBottomButtons() {
        hideChannelBottomButtons = !hideChannelBottomButtons;
        preferences.edit().putBoolean("hideChannelBottomButtons", hideChannelBottomButtons).apply();
    }

    public static void toggleHideKeyboardOnChatScroll() {
        hideKeyboardOnChatScroll = !hideKeyboardOnChatScroll;
        preferences.edit().putBoolean("hideKeyboardOnChatScroll", hideKeyboardOnChatScroll).apply();
    }

    public static void toggleHideStories() {
        hideStories = !hideStories;
        preferences.edit().putBoolean("hideStories", hideStories).apply();
    }

    public static void toggleHideTimeOnSticker() {
        hideTimeOnSticker = !hideTimeOnSticker;
        preferences.edit().putBoolean("hideTimeOnSticker", hideTimeOnSticker).apply();
    }

    public static void toggleIgnoreBlocked() {
        ignoreBlocked = !ignoreBlocked;
        preferences.edit().putBoolean("ignoreBlocked2", ignoreBlocked).apply();
    }

    public static void toggleIgnoreContentRestriction() {
        ignoreContentRestriction = !ignoreContentRestriction;
        preferences.edit().putBoolean("ignoreContentRestriction", ignoreContentRestriction).apply();
    }

    public static void toggleKeepFormatting() {
        keepFormatting = !keepFormatting;
        preferences.edit().putBoolean("keepFormatting", keepFormatting).apply();
    }

    public static void toggleMapDriftingFix() {
        mapDriftingFix = !mapDriftingFix;
        preferences.edit().putBoolean("mapDriftingFix", mapDriftingFix).apply();
    }

    public static void toggleMarkdownParseLinks() {
        markdownParseLinks = !markdownParseLinks;
        preferences.edit().putBoolean("markdownParseLinks", markdownParseLinks).apply();
    }

    public static void toggleMediaPreview() {
        mediaPreview = !mediaPreview;
        preferences.edit().putBoolean("mediaPreview", mediaPreview).apply();
    }

    public static void toggleMinimizedStickerCreator() {
        minimizedStickerCreator = !minimizedStickerCreator;
        preferences.edit().putBoolean("minimizedStickerCreator", minimizedStickerCreator).apply();
    }

    public static void setNewMarkdownParser(boolean value) {
        newMarkdownParser = value;
        preferences.edit().putBoolean("newMarkdownParser", value).apply();
    }

    public static void toggleOpenArchiveOnPull() {
        openArchiveOnPull = !openArchiveOnPull;
        preferences.edit().putBoolean("openArchiveOnPull", openArchiveOnPull).apply();
    }

    public static void togglePredictiveBackAnimation() {
        predictiveBackAnimation = !predictiveBackAnimation;
        preferences.edit().putBoolean("predictiveBackAnimation", predictiveBackAnimation).apply();
    }

    public static void togglePreferIPv6() {
        preferIPv6 = !preferIPv6;
        preferences.edit().putBoolean("preferIPv6", preferIPv6).apply();
    }

    public static void togglePreferOriginalQuality() {
        preferOriginalQuality = !preferOriginalQuality;
        preferences.edit().putBoolean("preferOriginalQuality", preferOriginalQuality).apply();
    }

    public static void toggleQuickForward() {
        quickForward = !quickForward;
        preferences.edit().putBoolean("quickForward", quickForward).apply();
    }

    public static void toggleReducedColors() {
        reducedColors = !reducedColors;
        preferences.edit().putBoolean("reducedColors", reducedColors).apply();
    }

    public static void toggleShouldNOTTrustMe() {
        shouldNOTTrustMe = !shouldNOTTrustMe;
        preferences.edit().putBoolean("shouldNOTTrustMe", shouldNOTTrustMe).apply();
    }

    public static void toggleShowAddToSavedMessages() {
        showAddToSavedMessages = !showAddToSavedMessages;
        preferences.edit().putBoolean("showAddToSavedMessages", showAddToSavedMessages).apply();
    }

    public static void toggleShowCopyPhoto() {
        showCopyPhoto = !showCopyPhoto;
        preferences.edit().putBoolean("showCopyPhoto", showCopyPhoto).apply();
    }

    public static void toggleShowDeleteDownloadedFile() {
        showDeleteDownloadedFile = !showDeleteDownloadedFile;
        preferences.edit().putBoolean("showDeleteDownloadedFile", showDeleteDownloadedFile).apply();
    }

    public static void toggleShowMessageDetails() {
        showMessageDetails = !showMessageDetails;
        preferences.edit().putBoolean("showMessageDetails", showMessageDetails).apply();
    }

    public static void toggleShowNoQuoteForward() {
        showNoQuoteForward = !showNoQuoteForward;
        preferences.edit().putBoolean("showNoQuoteForward", showNoQuoteForward).apply();
    }

    public static void toggleShowOpenIn() {
        showOpenIn = !showOpenIn;
        preferences.edit().putBoolean("showOpenIn", showOpenIn).apply();
    }

    public static void toggleShowOriginal() {
        showOriginal = !showOriginal;
        preferences.edit().putBoolean("showOriginal", showOriginal).apply();
    }

    public static void toggleShowPrPr() {
        showPrPr = !showPrPr;
        preferences.edit().putBoolean("showPrPr", showPrPr).apply();
    }

    public static void toggleShowQrCode() {
        showQrCode = !showQrCode;
        preferences.edit().putBoolean("showQrCode", showQrCode).apply();
    }

    public static void toggleShowRepeat() {
        showRepeat = !showRepeat;
        preferences.edit().putBoolean("showRepeat", showRepeat).apply();
    }

    public static void toggleShowReport() {
        showReport = !showReport;
        preferences.edit().putBoolean("showReport", showReport).apply();
    }

    public static void toggleShowRPCError() {
        showRPCError = !showRPCError;
        preferences.edit().putBoolean("showRPCError", showRPCError).apply();
    }

    public static void toggleShowSetReminder() {
        showSetReminder = !showSetReminder;
        preferences.edit().putBoolean("showSetReminder", showSetReminder).apply();
    }

    public static void toggleShowTimeHint() {
        showTimeHint = !showTimeHint;
        preferences.edit().putBoolean("showTimeHint", showTimeHint).apply();
    }

    public static void toggleShowTranslate() {
        showTranslate = !showTranslate;
        preferences.edit().putBoolean("showTranslate", showTranslate).apply();
    }

    public static void toggleSilenceNonContacts() {
        silenceNonContacts = !silenceNonContacts;
        preferences.edit().putBoolean("silenceNonContacts", silenceNonContacts).apply();
    }

    public static void toggleStrokeOnViews() {
        strokeOnViews = !strokeOnViews;
        preferences.edit().putBoolean("strokeOnViews", strokeOnViews).apply();
    }

    public static void toggleTryToOpenAllLinksInIV() {
        tryToOpenAllLinksInIV = !tryToOpenAllLinksInIV;
        preferences.edit().putBoolean("tryToOpenAllLinksInIV", tryToOpenAllLinksInIV).apply();
    }

    public static void toggleUnmuteVideosWithVolumeButtons() {
        unmuteVideosWithVolumeButtons = !unmuteVideosWithVolumeButtons;
        preferences.edit().putBoolean("unmuteVideosWithVolumeButtons", unmuteVideosWithVolumeButtons).apply();
    }

    public static void toggleUseSystemEmoji() {
        useSystemEmoji = !useSystemEmoji;
        preferences.edit().putBoolean("useSystemEmoji", useSystemEmoji).apply();
    }

    public static void toggleVoiceEnhancements() {
        voiceEnhancements = !voiceEnhancements;
        preferences.edit().putBoolean("voiceEnhancements", voiceEnhancements).apply();
    }

    public static void setCfAccountID(String value) {
        cfAccountID = value;
        preferences.edit().putString("cfAccountID", value).apply();
    }

    public static void setCfApiToken(String value) {
        cfApiToken = value;
        preferences.edit().putString("cfApiToken", value).apply();
    }

    public static void setExternalTranslationProvider(String value) {
        externalTranslationProvider = value;
        preferences.edit().putString("externalTranslationProvider", value).apply();
    }

    public static void setTranslationProvider(String value) {
        translationProvider = value;
        preferences.edit().putString("translationProvider2", value).apply();
    }

    public static void setTranslationTarget(String value) {
        translationTarget = value;
        preferences.edit().putString("translationTarget", value).apply();
    }

    public static void setRestrictedLanguages(Set<String> value) {
        restrictedLanguages = value;
        preferences.edit().putStringSet("restrictedLanguages", value).apply();
    }

    public static int getNotificationColor() {
        if (accentAsNotificationColor) {
            int color = 0;
            if (Theme.getActiveTheme().hasAccentColors()) {
                color = Theme.getActiveTheme().getAccentColor(Theme.getActiveTheme().currentAccentId);
            }
            if (color == 0) {
                color = Theme.getColor(Theme.key_actionBarDefault) | 0xff000000;
            }
            float brightness = AndroidUtilities.computePerceivedBrightness(color);
            if (brightness >= 0.721f || brightness <= 0.279f) {
                color = Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader) | 0xff000000;
            }
            return color;
        } else {
            return 0xff11acfa;
        }
    }
}
