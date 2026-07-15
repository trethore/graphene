package io.github.trethore.graphene.internal.event;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserLoadCompleted;
import io.github.trethore.graphene.api.browser.BrowserLoadFailed;
import io.github.trethore.graphene.api.browser.BrowserLoadListener;
import io.github.trethore.graphene.api.browser.BrowserLoadStarted;
import io.github.trethore.graphene.api.browser.BrowserLoadingState;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneLoadEventBus implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneLoadEventBus.class);

  private final List<BrowserLoadListener> listeners = new CopyOnWriteArrayList<>();
  private boolean closed;

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    listeners.clear();
  }

  public synchronized GrapheneSubscription subscribe(BrowserLoadListener listener) {
    BrowserLoadListener validatedListener = Objects.requireNonNull(listener, "listener");
    if (closed) {
      return GrapheneSubscriptions.empty();
    }
    listeners.add(validatedListener);
    return GrapheneSubscriptions.create(() -> listeners.remove(validatedListener));
  }

  public void publish(BrowserLoadingState state) {
    dispatch(listener -> listener.onLoadingStateChanged(state));
  }

  public void publish(BrowserLoadStarted event) {
    dispatch(listener -> listener.onLoadStarted(event));
  }

  public void publish(BrowserLoadCompleted event) {
    dispatch(listener -> listener.onLoadCompleted(event));
  }

  public void publish(BrowserLoadFailed event) {
    dispatch(listener -> listener.onLoadFailed(event));
  }

  private void dispatch(Consumer<BrowserLoadListener> callback) {
    for (BrowserLoadListener listener : listeners) {
      try {
        callback.accept(listener);
      } catch (RuntimeException exception) {
        LOGGER.error("Unhandled Graphene browser load listener exception", exception);
      }
    }
  }
}
