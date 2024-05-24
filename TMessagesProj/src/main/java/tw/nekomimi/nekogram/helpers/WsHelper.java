package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.text.TextUtils;

import com.google.gson.JsonParser;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;

import java.net.ServerSocket;
import java.util.HashMap;

import app.nekogram.tcp2ws.Tcp2WsServer;
import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.NekoConfig;

public class WsHelper {

    public static final String WS_ADDRESS = "ws.neko";
    private static int socksPort = -1;
    private static boolean tcp2wsStarted = false;

    public static int getSocksPort() {
        return getSocksPort(6356);
    }

    private static int getSocksPort(int port) {
        if (tcp2wsStarted && socksPort != -1) {
            return socksPort;
        }
        try {
            if (port != -1) {
                socksPort = port;
            } else {
                var socket = new ServerSocket(0);
                socksPort = socket.getLocalPort();
                socket.close();
            }
            if (!tcp2wsStarted) {
                Tcp2WsServer.setUserAgent(Extra.WS_USER_AGENT);
                Tcp2WsServer.setConnHash(Extra.WS_CONN_HASH);
                Tcp2WsServer.setCdnDomain(getWsDomain());
                Tcp2WsServer.setTls(NekoConfig.wsEnableTLS);
                var tcp2wsServer = new Tcp2WsServer();
                tcp2wsServer.start(socksPort);
                tcp2wsStarted = true;
                var map = new HashMap<String, String>();
                map.put("buildType", BuildConfig.BUILD_TYPE);
                map.put("isChineseUser", String.valueOf(NekoConfig.isChineseUser));
                AnalyticsHelper.trackEvent("tcp2ws_started", map);
            }
            return socksPort;
        } catch (Exception e) {
            FileLog.e(e);
            if (port != -1) {
                return getSocksPort(-1);
            } else {
                return -1;
            }
        }
    }

    private static String getWsDomain() {
        if (!TextUtils.isEmpty(NekoConfig.wsDomain)) {
            return NekoConfig.wsDomain;
        }
        var preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoremoteconfig", Activity.MODE_PRIVATE);
        var json = preferences.getString("get_config", "");
        if (TextUtils.isEmpty(json)) {
            return Extra.WS_DEFAULT_DOMAIN;
        }
        try {
            return JsonParser.parseString(json).getAsJsonObject().get("wsdomainv2").getAsString();
        } catch (Exception e) {
            FileLog.e(e);
            return Extra.WS_DEFAULT_DOMAIN;
        }
    }
}
