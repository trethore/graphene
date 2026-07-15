package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.navigation.BrowserNavigationPolicy;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefWindowOpenDisposition;
import org.junit.jupiter.api.Test;

class GrapheneCefNavigationRouterTest {
  @Test
  void mapsWindowOpenDispositions() {
    for (CefWindowOpenDisposition disposition : CefWindowOpenDisposition.values()) {
      assertEquals(
          BrowserNavigationPolicy.Disposition.valueOf(disposition.name()),
          GrapheneCefNavigationRouter.disposition(disposition));
    }
    assertEquals(
        BrowserNavigationPolicy.Disposition.UNKNOWN, GrapheneCefNavigationRouter.disposition(null));
  }

  @Test
  void cancelsPopupsByDefault() {
    AtomicReference<String> navigatedUrl = new AtomicReference<>();
    CefBrowser browser = browser(BrowserOptions.defaults(), navigatedUrl);
    GrapheneCefNavigationRouter router =
        new GrapheneCefNavigationRouter(GrapheneTaskExecutor.direct(), url -> {});

    router.onPopup(
        browser,
        frame(),
        "https://example.invalid/popup",
        "popup",
        CefWindowOpenDisposition.NEW_POPUP,
        true);
    assertNull(navigatedUrl.get());
  }

  @Test
  void routesSameSessionAndExternalDecisions() {
    AtomicReference<String> navigatedUrl = new AtomicReference<>();
    AtomicReference<String> externalUrl = new AtomicReference<>();
    BrowserNavigationPolicy policy =
        request ->
            request.type() == BrowserNavigationPolicy.Type.POPUP
                ? BrowserNavigationPolicy.Decision.SAME_SESSION
                : BrowserNavigationPolicy.Decision.EXTERNAL_BROWSER;
    CefBrowser browser =
        browser(BrowserOptions.builder().navigationPolicy(policy).build(), navigatedUrl);
    GrapheneCefNavigationRouter router =
        new GrapheneCefNavigationRouter(GrapheneTaskExecutor.direct(), externalUrl::set);

    router.onPopup(
        browser,
        frame(),
        "https://example.invalid/popup",
        "popup",
        CefWindowOpenDisposition.NEW_POPUP,
        true);
    assertEquals("https://example.invalid/popup", navigatedUrl.get());

    assertTrue(
        router.onMainFrameNavigation(
            browser, frame(), "https://example.invalid/external", true, false));
    assertEquals("https://example.invalid/external", externalUrl.get());
  }

  @Test
  void preservesCurrentTabNavigationAndCancelsConsumerManagedRequests() {
    AtomicReference<String> navigatedUrl = new AtomicReference<>();
    CefBrowser defaultBrowser = browser(BrowserOptions.defaults(), navigatedUrl);
    GrapheneCefNavigationRouter router =
        new GrapheneCefNavigationRouter(GrapheneTaskExecutor.direct(), url -> {});

    assertFalse(
        router.onMainFrameNavigation(
            defaultBrowser, frame(), "https://example.invalid/main", true, false));
    assertFalse(
        router.onOpenFromTab(
            defaultBrowser,
            frame(),
            "https://example.invalid/current",
            CefWindowOpenDisposition.CURRENT_TAB,
            true));

    BrowserOptions consumerManagedOptions =
        BrowserOptions.builder()
            .navigationPolicy(request -> BrowserNavigationPolicy.Decision.CONSUMER_MANAGED)
            .build();
    assertTrue(
        router.onOpenFromTab(
            browser(consumerManagedOptions, navigatedUrl),
            frame(),
            "https://example.invalid/new",
            CefWindowOpenDisposition.NEW_FOREGROUND_TAB,
            true));
    assertNull(navigatedUrl.get());
  }

  @Test
  void snapshotsPopupRequestForConsumerManagedHandling() {
    AtomicReference<BrowserNavigationPolicy.Request> capturedRequest = new AtomicReference<>();
    BrowserOptions options =
        BrowserOptions.builder()
            .navigationPolicy(
                request -> {
                  capturedRequest.set(request);
                  return BrowserNavigationPolicy.Decision.CONSUMER_MANAGED;
                })
            .build();
    CefBrowser browser = browser(options, new AtomicReference<>());
    GrapheneCefNavigationRouter router =
        new GrapheneCefNavigationRouter(GrapheneTaskExecutor.direct(), url -> {});

    router.onPopup(
        browser,
        frame(),
        "https://example.invalid/popup",
        "target-frame",
        CefWindowOpenDisposition.NEW_BACKGROUND_TAB,
        true);

    BrowserNavigationPolicy.Request request = capturedRequest.get();
    assertSame(browser, request.session());
    assertEquals(BrowserNavigationPolicy.Type.POPUP, request.type());
    assertEquals("https://example.invalid/popup", request.targetUrl());
    assertEquals("target-frame", request.targetFrameName());
    assertEquals(BrowserNavigationPolicy.Disposition.NEW_BACKGROUND_TAB, request.disposition());
    assertTrue(request.userGesture());
    assertFalse(request.redirect());
    BrowserNavigationPolicy.Frame sourceFrame = request.sourceFrame().orElseThrow();
    assertEquals("frame-id", sourceFrame.identifier());
    assertEquals("https://source.invalid", sourceFrame.url());
    assertTrue(sourceFrame.mainFrame());
  }

  private static CefBrowser browser(BrowserOptions options, AtomicReference<String> navigatedUrl) {
    return (CefBrowser)
        Proxy.newProxyInstance(
            CefBrowser.class.getClassLoader(),
            new Class<?>[] {CefBrowser.class, BrowserSession.class},
            (proxy, method, arguments) ->
                switch (method.getName()) {
                  case "options" -> options;
                  case "navigate" -> {
                    navigatedUrl.set((String) arguments[0]);
                    yield null;
                  }
                  case "isClosed" -> false;
                  case "equals", "hashCode", "toString" ->
                      objectMethod(proxy, method.getName(), arguments);
                  default -> defaultValue(method.getReturnType());
                });
  }

  private static CefFrame frame() {
    return (CefFrame)
        Proxy.newProxyInstance(
            CefFrame.class.getClassLoader(),
            new Class<?>[] {CefFrame.class},
            (proxy, method, arguments) ->
                switch (method.getName()) {
                  case "getIdentifier" -> "frame-id";
                  case "getName" -> "";
                  case "getURL" -> "https://source.invalid";
                  case "isMain", "isValid" -> true;
                  case "equals", "hashCode", "toString" ->
                      objectMethod(proxy, method.getName(), arguments);
                  default -> defaultValue(method.getReturnType());
                });
  }

  private static Object objectMethod(Object proxy, String methodName, Object[] arguments) {
    return switch (methodName) {
      case "equals" -> proxy == arguments[0];
      case "hashCode" -> System.identityHashCode(proxy);
      case "toString" -> proxy.getClass().getSimpleName();
      default -> throw new IllegalArgumentException(methodName);
    };
  }

  private static Object defaultValue(Class<?> type) {
    if (type == boolean.class) {
      return false;
    }
    if (type == int.class) {
      return 0;
    }
    if (type == long.class) {
      return 0L;
    }
    if (type == double.class) {
      return 0.0;
    }
    return null;
  }
}
