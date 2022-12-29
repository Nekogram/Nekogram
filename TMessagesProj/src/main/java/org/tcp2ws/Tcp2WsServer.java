package org.tcp2ws;

import java.net.InetAddress;
import java.net.UnknownHostException;

import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

public class Tcp2WsServer extends tcp2wsServer {

    static {
        var wsDomain = ConfigHelper.getWsDomain();

        mtpcdn.clear();
        mtpcdn.put(1, "pluto." + wsDomain);
        mtpcdn.put(2, "venus." + wsDomain);
        mtpcdn.put(3, "aurora." + wsDomain);
        mtpcdn.put(4, "vesta." + wsDomain);
        mtpcdn.put(5, "flora." + wsDomain);
        mtpcdn.put(17, "test_pluto." + wsDomain);
        mtpcdn.put(18, "test_venus." + wsDomain);
        mtpcdn.put(19, "test_aurora." + wsDomain);

        cdn.clear();
        cdn.put("149.154.175.50", "pluto." + wsDomain);
        cdn.put("149.154.167.51", "venus." + wsDomain);
        cdn.put("95.161.76.100", "venus." + wsDomain);
        cdn.put("149.154.175.100", "aurora." + wsDomain);
        cdn.put("149.154.167.91", "vesta." + wsDomain);
        cdn.put("149.154.171.5", "flora." + wsDomain);
        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000a").getHostAddress(), "pluto." + wsDomain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000a").getHostAddress(), "venus." + wsDomain);
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000a").getHostAddress(), "aurora." + wsDomain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000a").getHostAddress(), "vesta." + wsDomain);
            cdn.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000a").getHostAddress(), "flora." + wsDomain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        cdn.put("149.154.175.5", "pluto." + wsDomain);
        cdn.put("149.154.161.144", "venus." + wsDomain);
        cdn.put("149.154.167.15", "venus." + wsDomain);
        cdn.put("149.154.167.5", "venus." + wsDomain);
        cdn.put("149.154.167.6", "venus." + wsDomain);
        cdn.put("149.154.167.7", "venus." + wsDomain);
        cdn.put("91.108.4.", "vesta." + wsDomain);
        cdn.put("149.154.164.", "vesta." + wsDomain);
        cdn.put("149.154.165.", "vesta." + wsDomain);
        cdn.put("149.154.166.", "vesta." + wsDomain);
        cdn.put("149.154.167.8", "vesta." + wsDomain);
        cdn.put("149.154.167.9", "vesta." + wsDomain);
        cdn.put("91.108.56.", "flora." + wsDomain);
        cdn.put("111.62.91.", "venus." + wsDomain);
        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000d").getHostAddress(), "pluto." + wsDomain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000d").getHostAddress(), "venus." + wsDomain);
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000d").getHostAddress(), "aurora." + wsDomain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000d").getHostAddress(), "vesta." + wsDomain);
            cdn.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000d").getHostAddress(), "flora." + wsDomain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        cdn.put("149.154.175.10", "test_pluto." + wsDomain);
        cdn.put("149.154.175.40", "test_pluto." + wsDomain);
        cdn.put("149.154.167.40", "test_venus." + wsDomain);
        cdn.put("149.154.175.117", "test_aurora." + wsDomain);
        try {
            cdn.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000e").getHostAddress(), "test_pluto." + wsDomain);
            cdn.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000e").getHostAddress(), "test_venus." + wsDomain);
            cdn.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000e").getHostAddress(), "test_aurora." + wsDomain);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
}
