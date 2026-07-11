package io.github.trethore.graphene.api.bridge;

@FunctionalInterface
public interface GrapheneBridgeJsonEventListener<T> {
  void onEvent(String channel, T payload);
}
