package io.github.trethore.graphene.api.bridge;

/** Receives deserialized event payloads emitted through a bridge channel. */
@FunctionalInterface
public interface GrapheneBridgeJsonEventListener<T> {
  void onEvent(String channel, T payload);
}
