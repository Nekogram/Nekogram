package tw.nekomimi.nekogram.tlv;

import android.util.Base64;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.telegram.tgnet.InputSerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;

import java.util.function.BiFunction;
import java.util.function.Function;

public class TlBinaryReader {

    private static final JsonPrimitive JSON_TRUE = new JsonPrimitive(true);
    private static final JsonPrimitive JSON_FALSE = new JsonPrimitive(false);

    private final BiFunction<Integer, TlBinaryReader, JsonObject> reader;
    private final InputSerializedData data;

    private TlBinaryReader(BiFunction<Integer, TlBinaryReader, JsonObject> reader, InputSerializedData data) {
        this.reader = reader;
        this.data = data;
    }

    public static JsonElement deserializeObject(BiFunction<Integer, TlBinaryReader, JsonObject> reader, InputSerializedData data) {
        return new TlBinaryReader(reader, data).readObject();
    }

    public int readInt() {
        return data.readInt32(true);
    }

    public long readLong() {
        return data.readInt64(true);
    }

    public float readFloat() {
        return data.readFloat(true);
    }

    public double readDouble() {
        return data.readDouble(true);
    }

    public boolean readBoolean() {
        return data.readBool(true);
    }

    public byte[] readBytes() {
        return data.readByteArray(true);
    }

    public String readString() {
        return data.readString(true);
    }

    public JsonElement readObject() {
        return readObject(readInt());
    }

    public JsonElement readObject(int constructor) {
        if (constructor == Vector.constructor) {
            return readBareVector(TlBinaryReader::readObject);
        }

        if (constructor == TLRPC.TL_boolFalse.constructor) {
            return JSON_FALSE;
        }

        if (constructor == TLRPC.TL_boolTrue.constructor) {
            return JSON_TRUE;
        }

        if (constructor == 0x3fedd339) {
            return JSON_TRUE;
        }

        if (constructor == TLRPC.TL_null.constructor) {
            return JsonNull.INSTANCE;
        }

        return reader.apply(constructor, this);
    }

    public JsonArray readVector(
            Function<TlBinaryReader, ?> reader
    ) {
        var constructor = readInt();

        if (constructor != Vector.constructor) {
            throw new IllegalStateException("Invalid object code, expected 0x1cb5c415 (vector), got 0x" + Integer.toUnsignedString(constructor, 16));
        }

        return readBareVector(reader);
    }

    public JsonArray readBareVector(Function<TlBinaryReader, ?> reader) {
        var count = readInt();

        var result = new JsonArray(count);

        for (var i = 0; i < count; i++) {
            var value = reader.apply(this);
            switch (value) {
                case null -> result.add(JsonNull.INSTANCE);
                case JsonElement element -> result.add(element);
                case Boolean b -> result.add(b);
                case Number n -> result.add(n);
                case String s -> result.add(s);
                case byte[] bytes -> result.add(Base64.encodeToString(bytes, Base64.NO_WRAP));
                default -> throw new IllegalStateException("Unexpected value: " + value);
            }
        }

        return result;
    }
}