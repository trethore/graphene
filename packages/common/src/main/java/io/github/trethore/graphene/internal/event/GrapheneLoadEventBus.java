package io.github.trethore.graphene.internal.event;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserLoadCompleted;
import io.github.trethore.graphene.api.browser.BrowserLoadFailed;
import io.github.trethore.graphene.api.browser.BrowserLoadListener;
import io.github.trethore.graphene.api.browser.BrowserLoadStarted;
import io.github.trethore.graphene.api.browser.BrowserLoadingState;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneLoadEventBus implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneLoadEventBus.class);

  private final GrapheneListenerList<BrowserLoadListener> listeners = new GrapheneListenerList<>();

  @Override
  public void close() {
    listeners.close();
  }

  public GrapheneSubscription subscribe(BrowserLoadListener listener) {
    return listeners.subscribe(listener);
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
    listeners.dispatch(callback, LOGGER, "browser load listener");
  }
}
