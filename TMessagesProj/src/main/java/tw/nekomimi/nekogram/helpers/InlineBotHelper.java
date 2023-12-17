package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class InlineBotHelper extends BaseController {

    private static final InlineBotHelper[] Instance = new InlineBotHelper[UserConfig.MAX_ACCOUNT_COUNT];

    public InlineBotHelper(int num) {
        super(num);
    }

    public static InlineBotHelper getInstance(int num) {
        InlineBotHelper localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (InlineBotHelper.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new InlineBotHelper(num);
                }
            }
        }
        return localInstance;
    }

    public void query(UserHelper.BotInfo botInfo, String query, Utilities.Callback<ArrayList<TLRPC.BotInlineResult>> callback) {
        query(botInfo, query, true, true, callback);
    }

    public void query(UserHelper.BotInfo botInfo, String query, boolean searchUser, boolean cache, Utilities.Callback<ArrayList<TLRPC.BotInlineResult>> callback) {
        if (botInfo == null) {
            return;
        }
        var bot = getMessagesController().getUser(botInfo.getId());
        if (bot == null) {
            if (searchUser) {
                getUserHelper().resolveUser(botInfo.getUsername(), botInfo.getId(), user -> query(botInfo, query, false, cache, callback));
            } else {
                callback.run(null);
            }
            return;
        }

        var key = "inline_" + query + "_" + botInfo.getId();
        RequestDelegate requestDelegate = (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (cache && (!(response instanceof TLRPC.messages_BotResults) || ((TLRPC.messages_BotResults) response).results.isEmpty())) {
                query(botInfo, query, searchUser, false, callback);
                return;
            }

            if (response instanceof TLRPC.messages_BotResults res) {
                if (!cache && res.cache_time != 0) {
                    getMessagesStorage().saveBotCache(key, res);
                }
                callback.run(res.results);
            } else {
                callback.run(null);
            }
        });

        if (cache) {
            getMessagesStorage().getBotCache(key, requestDelegate);
        } else {
            TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
            req.query = query;
            req.bot = getMessagesController().getInputUser(bot);
            req.offset = "";
            req.peer = new TLRPC.TL_inputPeerEmpty();
            getConnectionsManager().sendRequest(req, requestDelegate, ConnectionsManager.RequestFlagFailOnServerErrors);
        }
    }
}
