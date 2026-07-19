package io.github.trethore.graphene.api.bridge;

/** Receives JSON event payloads emitted through a bridge channel. */
@FunctionalInterface
public interface GrapheneBridgeEventListener {
  void onEvent(String channel, String payloadJson);
}
