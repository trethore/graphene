package tytoo.grapheneui.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.Objects;

public final class GrapheneBridgeJson {
    private static final Gson GSON = new Gson();
    private static final String JSON_TYPE = "type";

    private GrapheneBridgeJson() {
    }

    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        Objects.requireNonNull(type, JSON_TYPE);
        return fromJson(json, (Type) type);
    }

    public static <T> T fromJson(String json, Type type) {
        Objects.requireNonNull(type, JSON_TYPE);
        String normalizedJson = json == null ? "null" : json;
        try {
            return GSON.fromJson(normalizedJson, type);
        } catch (JsonParseException exception) {
            throw new IllegalArgumentException("payloadJson must be valid JSON for type " + type.getTypeName(), exception);
        }
    }
}
