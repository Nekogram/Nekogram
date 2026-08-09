package tw.nekomimi.nekogram.helpers;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_bots;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import tw.nekomimi.nekogram.Extra;

public class CloudStorageHelper extends AccountInstance {

    private static final CloudStorageHelper[] Instance = new CloudStorageHelper[UserConfig.MAX_ACCOUNT_COUNT];

    private static final Type STRING_MAP_TYPE = new TypeToken<Map<String, String>>() {
    }.getType();
    private static final Gson GSON = new Gson();

    public CloudStorageHelper(int num) {
        super(num);
    }

    public static CloudStorageHelper getInstance(int num) {
        CloudStorageHelper localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (CloudStorageHelper.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new CloudStorageHelper(num);
                }
            }
        }
        return localInstance;
    }

    private void invokeWebViewCustomMethod(String method, String data, @NonNull BiConsumer<String, String> callback) {
        invokeWebViewCustomMethod(method, data, true, callback);
    }

    private void invokeWebViewCustomMethod(String method, String data, boolean searchUser, @NonNull BiConsumer<String, String> callback) {
        var botInfo = Extra.getHelperBot();
        if (botInfo == null) {
            return;
        }
        var user = getMessagesController().getUser(botInfo.getId());
        if (user == null) {
            if (searchUser) {
                getUserHelper().resolveUser(botInfo.getUsername(), botInfo.getId(), arg -> invokeWebViewCustomMethod(method, data, false, callback));
            } else {
                callback.accept(null, "USER_NOT_FOUND");
            }
            return;
        }
        var req = new TL_bots.invokeWebViewCustomMethod();
        req.bot = getMessagesController().getInputUser(user);
        req.custom_method = method;
        req.params = new TLRPC.TL_dataJSON();
        req.params.data = data;
        getConnectionsManager().sendRequest(req, (res, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                callback.accept(null, error.text);
            } else if (res instanceof TLRPC.TL_dataJSON dataJSON) {
                callback.accept(dataJSON.data, null);
            } else {
                callback.accept(null, null);
            }
        }));
    }

    public void setItem(String key, String value, @NonNull BiConsumer<String, String> callback) {
        Map<String, String> map = new HashMap<>();
        map.put("key", key);
        map.put("value", value);
        invokeWebViewCustomMethod("saveStorageValue", GSON.toJson(map), callback);
    }

    public void getItem(String key, @NonNull BiConsumer<String, String> callback) {
        getItems(new String[]{key}, (res, error) -> {
            if (res != null) {
                callback.accept(res.get(key), null);
            } else {
                callback.accept(null, error);
            }
        });
    }

    public void getItems(String[] keys, @NonNull BiConsumer<Map<String, String>, String> callback) {
        Map<String, String[]> map = new HashMap<>();
        map.put("keys", keys);
        invokeWebViewCustomMethod("getStorageValues", GSON.toJson(map), (res, error) -> {
            if (error == null) {
                callback.accept(GSON.fromJson(res, STRING_MAP_TYPE), null);
            } else {
                callback.accept(null, error);
            }
        });
    }

    public void removeItem(String key, @NonNull BiConsumer<String, String> callback) {
        removeItems(new String[]{key}, callback);
    }

    public void removeItems(String[] keys, @NonNull BiConsumer<String, String> callback) {
        Map<String, String[]> map = new HashMap<>();
        map.put("keys", keys);
        invokeWebViewCustomMethod("deleteStorageValues", GSON.toJson(map), callback);
    }

    public void getKeys(BiConsumer<String[], String> callback) {
        invokeWebViewCustomMethod("getStorageKeys", "{}", (res, error) -> {
            if (error == null) {
                String[] keys = GSON.fromJson(res, String[].class);
                callback.accept(keys, null);
            } else {
                callback.accept(null, error);
            }
        });
    }
}
