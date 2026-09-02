package tw.nekomimi.nekogram.translator;

import com.google.common.util.concurrent.SettableFuture;

import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.TranslateAlert2;

import java.io.IOException;
import java.util.List;

import app.nekogram.translator.GoogleAppTranslator;

public class TelegramTranslator implements Translator.ITranslator {


    private static final class InstanceHolder {
        private static final TelegramTranslator instance = new TelegramTranslator();
    }

    public static TelegramTranslator getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public Translator.TranslationResult translate(TLRPC.TL_textWithEntities query, String fl, String tl) throws Exception {
        SettableFuture<TLRPC.TL_textWithEntities> future = SettableFuture.create();
        var req = new TLRPC.TL_messages_translateText();
        req.flags |= 2;
        req.to_lang = tl;
        req.text.add(query);
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (res, error) -> {
            if (error != null) {
                future.setException(new IOException(error.text));
                return;
            }
            if (res instanceof TLRPC.TL_messages_translateResult tr && !tr.result.isEmpty()) {
                var text = TranslateAlert2.preprocess(query, tr.result.get(0));
                future.set(text);
            } else {
                future.setException(new IOException("not translated"));
            }
        });
        return Translator.TranslationResult.of(future.get(), null);
    }

    @Override
    public boolean supportLanguage(String language) {
        return GoogleAppTranslator.getInstance().supportLanguage(language);
    }

    @Override
    public List<String> getTargetLanguages() {
        return GoogleAppTranslator.getInstance().getTargetLanguages();
    }
}
