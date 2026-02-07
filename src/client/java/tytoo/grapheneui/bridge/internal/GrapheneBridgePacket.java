package tytoo.grapheneui.bridge.internal;

import com.google.gson.JsonElement;

final class GrapheneBridgePacket {
    String bridge;
    int version = GrapheneBridgeProtocol.VERSION;
    String kind;
    String id;
    String channel;
    JsonElement payload;
    Boolean ok;
    GrapheneBridgePacketError error;
}
