package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserConsoleMessage;
import io.github.trethore.graphene.api.browser.BrowserConsoleSeverity;
import io.github.trethore.graphene.api.browser.BrowserTitleListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class GrapheneBrowserDisplayStateTest {
  @Test
  void exposesNormalizedInitialStateAndChangedValues() {
    try (GrapheneBrowserDisplayState state = new GrapheneBrowserDisplayState("")) {
      List<String> titles = new ArrayList<>();
      List<String> urls = new ArrayList<>();
      try (GrapheneSubscription ignoredTitleSubscription =
              state.onTitleChanged(ignored -> titles.add(state.currentTitle()));
          GrapheneSubscription ignoredUrlSubscription =
              state.onUrlChanged(ignored -> urls.add(state.currentUrl()))) {
        updateTitle(state, null);
        updateTitle(state, "Graphene");
        updateTitle(state, "Graphene");
        updateUrl(state, null);
        updateUrl(state, "https://example.test/app");
        updateUrl(state, "https://example.test/app");

        assertEquals("Graphene", state.currentTitle());
        assertEquals("https://example.test/app", state.currentUrl());
        assertEquals(List.of("Graphene"), titles);
        assertEquals(List.of("https://example.test/app"), urls);
      }
    }
  }

  @Test
  void publishesConsoleMessagesWithoutDeduplication() {
    try (GrapheneBrowserDisplayState state = new GrapheneBrowserDisplayState("about:blank")) {
      List<BrowserConsoleMessage> messages = new ArrayList<>();
      try (GrapheneSubscription ignoredSubscription = state.onConsoleMessage(messages::add)) {
        run(state.consoleMessage(BrowserConsoleSeverity.ERROR, "failure", "app.js", 42));
        run(state.consoleMessage(BrowserConsoleSeverity.ERROR, "failure", "app.js", 42));

        BrowserConsoleMessage expected =
            new BrowserConsoleMessage(BrowserConsoleSeverity.ERROR, "failure", "app.js", 42);
        assertEquals(List.of(expected, expected), messages);
      }
    }
  }

  @Test
  void avoidsNotificationsWhenThereAreNoListeners() {
    try (GrapheneBrowserDisplayState state = new GrapheneBrowserDisplayState("about:blank")) {
      assertNull(state.updateTitle("Graphene"));
      assertNull(state.updateUrl("https://example.test/app"));
      assertNull(state.consoleMessage(BrowserConsoleSeverity.INFO, "ready", "app.js", 1));
      assertEquals("Graphene", state.currentTitle());
      assertEquals("https://example.test/app", state.currentUrl());
    }
  }

  @Test
  void subscriptionsAreIndependentAndIdempotent() {
    try (GrapheneBrowserDisplayState state = new GrapheneBrowserDisplayState("about:blank")) {
      AtomicInteger notifications = new AtomicInteger();
      BrowserTitleListener listener = ignored -> notifications.incrementAndGet();
      try (GrapheneSubscription first = state.onTitleChanged(listener);
          GrapheneSubscription ignoredSecond = state.onTitleChanged(listener)) {
        first.unsubscribe();
        first.unsubscribe();
        updateTitle(state, "Graphene");

        assertEquals(1, notifications.get());
      }
    }
  }

  @Test
  void listenerFailuresDoNotPreventLaterListeners() {
    try (GrapheneBrowserDisplayState state = new GrapheneBrowserDisplayState("about:blank")) {
      AtomicInteger notifications = new AtomicInteger();
      try (GrapheneSubscription ignoredFailingSubscription =
              state.onTitleChanged(
                  ignored -> {
                    throw new IllegalStateException("listener failure");
                  });
          GrapheneSubscription ignoredSuccessfulSubscription =
              state.onTitleChanged(ignored -> notifications.incrementAndGet())) {
        updateTitle(state, "Graphene");

        assertEquals(1, notifications.get());
      }
    }
  }

  @Test
  void closeClearsListenersAndRejectsLaterSubscriptions() {
    try (GrapheneBrowserDisplayState state = new GrapheneBrowserDisplayState("about:blank")) {
      AtomicInteger notifications = new AtomicInteger();
      try (GrapheneSubscription ignoredInitialSubscription =
          state.onTitleChanged(ignored -> notifications.incrementAndGet())) {
        state.close();
        updateTitle(state, "ignored");
        try (GrapheneSubscription ignoredRejectedSubscription =
            state.onTitleChanged(ignored -> notifications.incrementAndGet())) {
          updateTitle(state, "also ignored");
        }

        assertEquals(0, notifications.get());
        assertEquals("", state.currentTitle());
      }
    }
  }

  private static void updateTitle(GrapheneBrowserDisplayState state, String title) {
    run(state.updateTitle(title));
  }

  private static void updateUrl(GrapheneBrowserDisplayState state, String url) {
    run(state.updateUrl(url));
  }

  private static void run(Runnable notification) {
    if (notification != null) {
      notification.run();
    }
  }
}
