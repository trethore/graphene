package io.github.trethore.graphene.api.bridge;

@FunctionalInterface
public interface GrapheneBridgeSubscription extends AutoCloseable {
  void unsubscribe();

  @Override
  default void close() {
    unsubscribe();
  }
}
