package io.github.trethore.graphene.api.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.util.Objects;

/** Converts bridge payloads to and from JSON using Graphene's JSON representation. */
public final class GrapheneBridgeJson {
  private static final Gson GSON = new Gson();
  private static final String JSON_TYPE = "type";

  private GrapheneBridgeJson() {}

  /** Serializes a value as JSON, including {@code null} as the JSON {@code null} value. */
  public static String toJson(Object value) {
    return GSON.toJson(value);
  }

  /** Deserializes JSON into the requested class, treating a {@code null} payload as JSON null. */
  public static <T> T fromJson(String json, Class<T> type) {
    Objects.requireNonNull(type, JSON_TYPE);
    return fromJson(json, (Type) type);
  }

  /**
   * Deserializes JSON into the requested generic type, treating a {@code null} payload as JSON
   * null.
   */
  public static <T> T fromJson(String json, Type type) {
    Objects.requireNonNull(type, JSON_TYPE);
    String normalizedJson = json == null ? "null" : json;
    try {
      return GSON.fromJson(normalizedJson, type);
    } catch (JsonParseException exception) {
      throw new IllegalArgumentException(
          "payloadJson must be valid JSON for type " + type.getTypeName(), exception);
    }
  }
}
