package io.github.trethore.graphene.internal.bridge;

import com.google.gson.JsonElement;

final class GrapheneBridgePacket {
  String bridge;
  String kind;
  String id;
  String channel;
  JsonElement payload;
  Boolean ok;
  GrapheneBridgePacketError error;
}
