package io.github.trethore.graphene.api.bridge;

@FunctionalInterface
public interface GrapheneBridgeEventListener {
  void onEvent(String channel, String payloadJson);
}
