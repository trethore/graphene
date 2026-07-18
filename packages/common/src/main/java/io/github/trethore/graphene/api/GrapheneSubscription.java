package io.github.trethore.graphene.api;

/** A closeable subscription to a Graphene listener or handler. */
@FunctionalInterface
public interface GrapheneSubscription extends AutoCloseable {
  /** Removes the associated listener or handler. Repeated calls have no additional effect. */
  void unsubscribe();

  @Override
  default void close() {
    unsubscribe();
  }
}
