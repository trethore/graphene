package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserConsoleMessage;
import io.github.trethore.graphene.api.browser.BrowserConsoleMessageListener;
import io.github.trethore.graphene.api.browser.BrowserConsoleSeverity;
import io.github.trethore.graphene.api.browser.BrowserTitleListener;
import io.github.trethore.graphene.api.browser.BrowserUrlListener;
import io.github.trethore.graphene.internal.event.GrapheneListenerList;
import java.util.Objects;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneBrowserDisplayState implements AutoCloseable {
  private static final String BLANK_URL = "about:blank";
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBrowserDisplayState.class);

  private final GrapheneListenerList<BrowserTitleListener> titleListeners =
      new GrapheneListenerList<>();
  private final GrapheneListenerList<BrowserUrlListener> urlListeners =
      new GrapheneListenerList<>();
  private final GrapheneListenerList<BrowserConsoleMessageListener> consoleMessageListeners =
      new GrapheneListenerList<>();
  private volatile String currentTitle = "";
  private volatile String currentUrl;
  private boolean closed;

  public GrapheneBrowserDisplayState(String initialUrl) {
    currentUrl = normalizeUrl(initialUrl);
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    titleListeners.close();
    urlListeners.close();
    consoleMessageListeners.close();
  }

  public GrapheneSubscription onTitleChanged(BrowserTitleListener listener) {
    return titleListeners.subscribe(listener);
  }

  public GrapheneSubscription onUrlChanged(BrowserUrlListener listener) {
    return urlListeners.subscribe(listener);
  }

  public GrapheneSubscription onConsoleMessage(BrowserConsoleMessageListener listener) {
    return consoleMessageListeners.subscribe(listener);
  }

  public Runnable updateTitle(String title) {
    String normalizedTitle = Objects.requireNonNullElse(title, "");
    synchronized (this) {
      if (closed || currentTitle.equals(normalizedTitle)) {
        return null;
      }
      currentTitle = normalizedTitle;
    }
    return notification(
        titleListeners,
        BrowserTitleListener::onTitleChanged,
        normalizedTitle,
        "browser title-change listener");
  }

  public Runnable updateUrl(String url) {
    String normalizedUrl = normalizeUrl(url);
    synchronized (this) {
      if (closed || currentUrl.equals(normalizedUrl)) {
        return null;
      }
      currentUrl = normalizedUrl;
    }
    return notification(
        urlListeners,
        BrowserUrlListener::onUrlChanged,
        normalizedUrl,
        "browser URL-change listener");
  }

  public Runnable consoleMessage(
      BrowserConsoleSeverity severity, String message, String source, int line) {
    synchronized (this) {
      if (closed || consoleMessageListeners.isEmpty()) {
        return null;
      }
    }
    BrowserConsoleMessage consoleMessage =
        new BrowserConsoleMessage(
            Objects.requireNonNull(severity, "severity"),
            Objects.requireNonNullElse(message, ""),
            Objects.requireNonNullElse(source, ""),
            line);
    return notification(
        consoleMessageListeners,
        BrowserConsoleMessageListener::onConsoleMessage,
        consoleMessage,
        "browser console-message listener");
  }

  private static <T, U> Runnable notification(
      GrapheneListenerList<T> listeners,
      BiConsumer<T, U> callback,
      U event,
      String listenerDescription) {
    if (listeners.isEmpty()) {
      return null;
    }
    return () ->
        listeners.dispatch(
            listener -> callback.accept(listener, event), LOGGER, listenerDescription);
  }

  private static String normalizeUrl(String url) {
    return url == null || url.isBlank() ? BLANK_URL : url;
  }

  public String currentTitle() {
    return currentTitle;
  }

  public String currentUrl() {
    return currentUrl;
  }
}
