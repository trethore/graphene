package tytoo.grapheneui.internal.bridge;

import com.google.gson.*;

import java.util.Objects;

final class GrapheneBridgeMessageCodec {
    private static final String FIELD_BRIDGE = "bridge";
    private static final String FIELD_PAYLOAD = "payload";
    private final Gson gson;

    GrapheneBridgeMessageCodec(Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    GrapheneBridgePacket parsePacket(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            return null;
        }

        try {
            JsonObject jsonObject = JsonParser.parseString(requestJson).getAsJsonObject();
            JsonElement bridgeElement = jsonObject.get(FIELD_BRIDGE);
            if (bridgeElement == null || !bridgeElement.isJsonPrimitive()) {
                return null;
            }

            GrapheneBridgePacket packet = gson.fromJson(jsonObject, GrapheneBridgePacket.class);
            if (!GrapheneBridgeProtocol.NAME.equals(packet.bridge)) {
                return null;
            }

            return packet;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    JsonElement parsePayloadJson(String payloadJson) {
        String value = payloadJson == null ? "null" : payloadJson;
        try {
            return JsonParser.parseString(value);
        } catch (JsonSyntaxException exception) {
            throw new IllegalArgumentException("payloadJson must be a valid JSON value", exception);
        }
    }

    String payloadToJson(JsonElement payload) {
        if (payload == null || payload.isJsonNull()) {
            return "null";
        }

        return gson.toJson(payload);
    }

    String createOutboundPacketJson(String kind, String id, String channel, JsonElement payload) {
        JsonObject packet = new JsonObject();
        packet.addProperty(FIELD_BRIDGE, GrapheneBridgeProtocol.NAME);
        packet.addProperty("version", GrapheneBridgeProtocol.VERSION);
        packet.addProperty("kind", kind);
        if (id != null) {
            packet.addProperty("id", id);
        }
        packet.addProperty("channel", channel);
        packet.add(FIELD_PAYLOAD, payload == null ? JsonNull.INSTANCE : payload);
        return gson.toJson(packet);
    }

    String createSuccessResponseJson(String requestId, String channel, JsonElement payload) {
        JsonObject response = createResponseBase(requestId, channel);
        response.addProperty("ok", true);
        response.add(FIELD_PAYLOAD, payload == null ? JsonNull.INSTANCE : payload);
        return gson.toJson(response);
    }

    String createErrorResponseJson(String requestId, String channel, String errorCode, String errorMessage) {
        JsonObject response = createResponseBase(requestId, channel);
        response.addProperty("ok", false);
        response.add(FIELD_PAYLOAD, JsonNull.INSTANCE);

        JsonObject error = new JsonObject();
        error.addProperty("code", errorCode == null ? "bridge_error" : errorCode);
        error.addProperty("message", errorMessage == null ? "Bridge request failed" : errorMessage);
        response.add("error", error);
        return gson.toJson(response);
    }

    String quoteJsString(String value) {
        return gson.toJson(value);
    }

    private JsonObject createResponseBase(String requestId, String channel) {
        JsonObject response = new JsonObject();
        response.addProperty(FIELD_BRIDGE, GrapheneBridgeProtocol.NAME);
        response.addProperty("version", GrapheneBridgeProtocol.VERSION);
        response.addProperty("kind", GrapheneBridgeProtocol.KIND_RESPONSE);
        if (requestId != null) {
            response.addProperty("id", requestId);
        }
        if (channel != null) {
            response.addProperty("channel", channel);
        }
        return response;
    }
}
