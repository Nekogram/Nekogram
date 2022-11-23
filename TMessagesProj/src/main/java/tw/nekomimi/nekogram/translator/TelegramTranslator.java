package tw.nekomimi.nekogram.translator;

import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class TelegramTranslator extends BaseTranslator {

    private static final class InstanceHolder {
        private static final TelegramTranslator instance = new TelegramTranslator();
    }

    static TelegramTranslator getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public List<String> getTargetLanguages() {
        return GoogleAppTranslator.getInstance().getTargetLanguages();
    }

    @Override
    public String convertLanguageCode(String language, String country) {
        return GoogleAppTranslator.getInstance().convertLanguageCode(language, country);
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws Exception {
        AtomicReference<Object> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        TLRPC.TL_messages_translateText req = new TLRPC.TL_messages_translateText();
        req.flags |= 2;
        req.to_lang = tl;
        req.text = query;
        ConnectionsManager.getInstance(UserConfig.selectedAccount).sendRequest(req, (res, error) -> {
            if (error == null) {
                if (res instanceof TLRPC.TL_messages_translateResultText) {
                    result.set(((TLRPC.TL_messages_translateResultText) res).text);
                } else {
                    result.set(new IOException("messages.translateNoResult"));
                }
            } else {
                result.set(new IOException(error.text));
            }
            latch.countDown();
        });

        latch.await();
        Object object = result.get();
        if (object instanceof String) {
            return new Result(object, null);
        } else {
            throw (IOException) object;
        }
    }
}
