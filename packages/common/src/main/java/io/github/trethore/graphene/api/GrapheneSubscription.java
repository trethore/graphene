package io.github.trethore.graphene.api;

@FunctionalInterface
public interface GrapheneSubscription extends AutoCloseable {
  /** Removes the associated listener or handler. Repeated calls have no additional effect. */
  void unsubscribe();

  @Override
  default void close() {
    unsubscribe();
  }
}
