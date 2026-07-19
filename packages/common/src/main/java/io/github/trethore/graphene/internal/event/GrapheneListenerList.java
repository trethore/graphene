package io.github.trethore.graphene.internal.event;

import io.github.trethore.graphene.api.GrapheneSubscription;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;

public final class GrapheneListenerList<T> implements AutoCloseable {
  private final List<T> listeners = new CopyOnWriteArrayList<>();
  private boolean closed;

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    listeners.clear();
  }

  public synchronized GrapheneSubscription subscribe(T listener) {
    T validatedListener = Objects.requireNonNull(listener, "listener");
    if (closed) {
      return GrapheneSubscriptions.empty();
    }
    listeners.add(validatedListener);
    return GrapheneSubscriptions.create(() -> listeners.remove(validatedListener));
  }

  public void dispatch(Consumer<T> callback, Logger logger, String listenerDescription) {
    for (T listener : listeners) {
      try {
        callback.accept(listener);
      } catch (RuntimeException exception) {
        logger.error("Unhandled Graphene {} exception", listenerDescription, exception);
      }
    }
  }

  public boolean isEmpty() {
    return listeners.isEmpty();
  }
}
