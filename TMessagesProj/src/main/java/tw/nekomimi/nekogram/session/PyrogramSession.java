package tw.nekomimi.nekogram.session;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PyrogramSession {

    public static final int SESSION_BYTE_LENGTH = 271; // 1 + 4 + 1 + 256 + 8 + 1

    public static byte[] pack(
            int dcId,
            long apiId,
            boolean testMode,
            byte[] authKey,
            long userId,
            boolean isBot
    ) {
        var buffer = ByteBuffer.allocate(SESSION_BYTE_LENGTH);
        // Pyrogram format ">" specifies BIG_ENDIAN
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.put((byte) dcId);                  // B: unsigned char (1 byte)
        buffer.putInt((int) (apiId & 0xFFFFFFFFL)); // I: unsigned int 32 (4 bytes)
        buffer.put((byte) (testMode ? 1 : 0));     // ?: boolean (1 byte)
        buffer.put(authKey);                      // 256s: raw 256 bytes
        buffer.putLong(userId);                   // Q: unsigned long long 64 (8 bytes)
        buffer.put((byte) (isBot ? 1 : 0));       // ?: boolean (1 byte)

        return buffer.array();
    }

    public static SessionData unpack(byte[] rawBytes) {
        if (rawBytes.length != SESSION_BYTE_LENGTH) {
            throw new IllegalArgumentException("Invalid session length: expected 271 bytes, got " + rawBytes.length);
        }
        ByteBuffer buffer = ByteBuffer.wrap(rawBytes);
        buffer.order(ByteOrder.BIG_ENDIAN);

        var dcId = buffer.get() & 0xFF;
        var apiId = buffer.getInt() & 0xFFFFFFFFL;
        var testMode = buffer.get() != 0;

        var authKey = new byte[256];
        buffer.get(authKey);

        var userId = buffer.getLong();
        var isBot = buffer.get() != 0;

        return new SessionData(dcId, apiId, testMode, authKey, userId, isBot);
    }

    public record SessionData(int dcId, long apiId, boolean testMode, byte[] authKey, long userId,
                              boolean isBot) {
    }
}