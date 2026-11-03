package tw.nekomimi.nekogram.translator;

import com.google.net.cronet.okhttptransport.CronetInterceptor;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.TranslateAlert2;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import app.nekogram.translator.AndroidTranslatorConfig;
import app.nekogram.translator.BaiduTranslator;
import app.nekogram.translator.BaseTranslator;
import app.nekogram.translator.DeepLTranslator;
import app.nekogram.translator.GoogleAppTranslator;
import app.nekogram.translator.LingoTranslator;
import app.nekogram.translator.MicrosoftTranslator;
import app.nekogram.translator.SogouTranslator;
import app.nekogram.translator.TranSmartTranslator;
import app.nekogram.translator.YandexTranslator;
import app.nekogram.translator.YouDaoTranslator;
import okhttp3.OkHttpClient;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.CronetHelper;
import tw.nekomimi.nekogram.translator.deepl.DeepLOAuth;
import tw.nekomimi.nekogram.translator.html.HTMLKeeper;

public class TextWithEntitiesTranslator implements Translator.ITranslator {

    private static final HashMap<String, TextWithEntitiesTranslator> wrappedTranslators = new HashMap<>();
    private static boolean configuredCallFactory = false;

    public static TextWithEntitiesTranslator of(String type) {
        if (!configuredCallFactory) {
            AndroidTranslatorConfig.setOkhttpClient(buildClient());
            configuredCallFactory = true;
        }
        return wrappedTranslators.computeIfAbsent(type, type1 -> {
            var translator = switch (type1) {
                case Translator.PROVIDER_YANDEX -> YandexTranslator.INSTANCE;
                case Translator.PROVIDER_LINGO -> LingoTranslator.INSTANCE;
                case Translator.PROVIDER_DEEPL -> DeepLTranslator.INSTANCE;
                case Translator.PROVIDER_MICROSOFT -> MicrosoftTranslator.INSTANCE;
                case Translator.PROVIDER_YOUDAO -> YouDaoTranslator.INSTANCE;
                case Translator.PROVIDER_BAIDU -> BaiduTranslator.INSTANCE;
                case Translator.PROVIDER_SOGOU -> SogouTranslator.INSTANCE;
                case Translator.PROVIDER_TENCENT -> TranSmartTranslator.INSTANCE;
                default -> GoogleAppTranslator.INSTANCE;
            };
            return new TextWithEntitiesTranslator(translator);
        });
    }

    private static OkHttpClient buildClient() {
        var builder = new OkHttpClient.Builder();
        builder.connectTimeout(120, TimeUnit.SECONDS);
        builder.readTimeout(120, TimeUnit.SECONDS);
        builder.writeTimeout(120, TimeUnit.SECONDS);
        if (CronetHelper.isAvailable()) {
            builder.addInterceptor(CronetInterceptor.newBuilder(CronetHelper.getEngine()).build());
        }
        return builder.build();
    }

    private final BaseTranslator translator;

    private TextWithEntitiesTranslator(BaseTranslator translator) {
        this.translator = translator;
    }

    @Override
    public Translator.TranslationResult translate(TLRPC.TL_textWithEntities query, String fl, String tl) throws Exception {
        if (translator instanceof DeepLTranslator) {
            DeepLOAuth.configureAccessToken();
        }
        if (NekoConfig.keepFormatting) {
            var html = HTMLKeeper.entitiesToHtml(query.text, query.entities, false);
            var result = translator.translateSync(html, null, tl);
            var textAndEntitiesTranslated = HTMLKeeper.htmlToEntities(result.getTranslation(), query.entities, false);
            return Translator.TranslationResult.of(
                    TranslateAlert2.preprocess(query, textAndEntitiesTranslated),
                    result.getSourceLanguage()
            );
        } else {
            var result = translator.translateSync(query.text, null, tl);
            return Translator.TranslationResult.of(
                    Translator.textWithEntities(result.getTranslation(), null)
                    , result.getSourceLanguage());
        }
    }

    @Override
    public boolean supportLanguage(String language) {
        return translator.supportLanguage(language);
    }

    @Override
    public List<String> getTargetLanguages() {
        return translator.getTargetLanguages();
    }
}
