package io.github.trethore.graphene.internal.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.bridge.GrapheneBridgeSubscription;
import io.github.trethore.graphene.api.browser.bridge.BrowserBridgePolicy;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

final class GrapheneBridgeEndpointTest {
  @Test
  void injectsBootstrapAndFlushesQueuedMessagesAfterReadyHandshake() {
    TestBrowser browser = new TestBrowser();
    GrapheneBridgeEndpoint endpoint = endpoint(browser);

    endpoint.emit("test:event", "{\"value\":1}");
    assertFalse(endpoint.isReady());
    assertTrue(browser.executedScripts.isEmpty());

    endpoint.onPageLoadEnd(browser.currentUrl());
    int bootstrapScriptCount = GrapheneBridgeScriptLoader.scripts().size();
    assertEquals(bootstrapScriptCount, browser.executedScripts.size());

    TestQueryCallback callback = new TestQueryCallback();
    boolean handled =
        endpoint.handleQuery(
            mainFrame(browser.currentUrl()),
            "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"ready\"}",
            callback);

    assertTrue(handled);
    assertTrue(endpoint.isReady());
    assertEquals("{}", callback.successResponse);
    assertEquals(bootstrapScriptCount + 1, browser.executedScripts.size());
    assertTrue(
        browser
            .executedScripts
            .get(bootstrapScriptCount)
            .contains("__grapheneBridgeReceiveFromJava"));
  }

  @Test
  void completesJavaRequestFromInboundResponse() {
    TestBrowser browser = new TestBrowser();
    GrapheneBridgeEndpoint endpoint = endpoint(browser);
    endpoint.onPageLoadEnd(browser.currentUrl());
    TestQueryCallback readyCallback = new TestQueryCallback();
    endpoint.handleQuery(
        mainFrame(browser.currentUrl()),
        "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"ready\"}",
        readyCallback);

    CompletableFuture<String> response = endpoint.request("test:request", "{\"value\":2}");
    String outboundScript = browser.executedScripts.getLast();
    String requestId = extractRequestId(outboundScript);
    TestQueryCallback responseCallback = new TestQueryCallback();

    endpoint.handleQuery(
        mainFrame(browser.currentUrl()),
        "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"response\","
            + "\"id\":\""
            + requestId
            + "\",\"channel\":\"test:request\",\"ok\":true,\"payload\":{\"result\":4}}",
        responseCallback);

    assertEquals("{\"result\":4}", response.join());
    assertEquals("{}", responseCallback.successResponse);
  }

  @Test
  void deniesRemoteDocumentsByDefault() {
    TestBrowser browser = new TestBrowser();
    browser.currentUrl = "https://example.com/index.html";
    GrapheneBridgeEndpoint endpoint = endpoint(browser);

    endpoint.onPageLoadEnd(browser.currentUrl());
    RecordingQueryCallback callback = new RecordingQueryCallback();
    boolean handled =
        endpoint.handleQuery(
            mainFrame(browser.currentUrl()),
            "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"ready\"}",
            callback);

    assertTrue(handled);
    assertTrue(browser.executedScripts.isEmpty());
    assertEquals(403, callback.failureCode);
    assertFalse(endpoint.isReady());
  }

  @Test
  void allowsExplicitInitialOrigin() {
    TestBrowser browser = new TestBrowser();
    browser.currentUrl = "https://example.com/index.html";
    GrapheneBridgeEndpoint endpoint =
        new GrapheneBridgeEndpoint(
            browser,
            GrapheneBridgeOptions.defaults(),
            GrapheneTaskExecutor.direct(),
            new GrapheneBridgeExposureConfig(
                BrowserBridgePolicy.initialOrigin(), browser.currentUrl(), ""));

    endpoint.onPageLoadEnd(browser.currentUrl());

    assertEquals(GrapheneBridgeScriptLoader.scripts().size(), browser.executedScripts.size());
  }

  @Test
  void allowsOnlyTheConfiguredGrapheneHttpOriginByDefault() {
    TestBrowser browser = new TestBrowser();
    browser.currentUrl = "http://127.0.0.1:31000/mods/test/index.html";
    GrapheneBridgeEndpoint endpoint =
        new GrapheneBridgeEndpoint(
            browser,
            GrapheneBridgeOptions.defaults(),
            GrapheneTaskExecutor.direct(),
            new GrapheneBridgeExposureConfig(
                BrowserBridgePolicy.defaultPolicy(),
                browser.currentUrl(),
                "http://127.0.0.1:31000"));

    endpoint.onPageLoadEnd(browser.currentUrl());
    assertEquals(GrapheneBridgeScriptLoader.scripts().size(), browser.executedScripts.size());

    browser.executedScripts.clear();
    browser.currentUrl = "http://127.0.0.1:31001/mods/test/index.html";
    endpoint.onNavigationRequested();
    endpoint.onPageLoadEnd(browser.currentUrl());
    assertTrue(browser.executedScripts.isEmpty());
  }

  @Test
  void policyFailuresFailClosed() {
    assertPolicyDenied(request -> null);
    assertPolicyDenied(
        request -> {
          throw new IllegalStateException("Policy failure");
        });
  }

  @Test
  void deniesQueriesFromSubframes() {
    TestBrowser browser = new TestBrowser();
    GrapheneBridgeEndpoint endpoint = endpoint(browser);
    endpoint.onPageLoadEnd(browser.currentUrl());
    RecordingQueryCallback callback = new RecordingQueryCallback();

    boolean handled =
        endpoint.handleQuery(
            new BridgeFrame(browser.currentUrl(), false),
            "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"ready\"}",
            callback);

    assertTrue(handled);
    assertEquals(403, callback.failureCode);
    assertFalse(endpoint.isReady());
  }

  @Test
  void reservesGrapheneChannelPrefixForInternalUse() {
    TestBrowser browser = new TestBrowser();
    GrapheneBridgeEndpoint endpoint = endpoint(browser);

    assertThrows(IllegalArgumentException.class, () -> endpoint.emit("graphene:test", "null"));
    try (GrapheneBridgeSubscription subscription =
        GrapheneBridgeInternals.onEvent(endpoint, "graphene:test", (channel, payload) -> {})) {
      assertNotNull(subscription);
    }
  }

  @Test
  void ignoresAReadyHandshakeAfterTheDocumentChanges() {
    TestBrowser browser = new TestBrowser();
    QueuedTaskExecutor taskExecutor = new QueuedTaskExecutor();
    GrapheneBridgeEndpoint endpoint = endpoint(browser, taskExecutor);
    endpoint.onPageLoadEnd(browser.currentUrl());

    endpoint.handleQuery(
        mainFrame(browser.currentUrl()),
        "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"ready\"}",
        new TestQueryCallback());
    browser.currentUrl = "app://assets/test/next.html";
    endpoint.onNavigationRequested();
    endpoint.onPageLoadEnd(browser.currentUrl());
    taskExecutor.runAll();

    assertFalse(endpoint.isReady());
  }

  private static GrapheneBridgeEndpoint endpoint(TestBrowser browser) {
    return endpoint(browser, GrapheneTaskExecutor.direct());
  }

  private static GrapheneBridgeEndpoint endpoint(
      TestBrowser browser, GrapheneTaskExecutor taskExecutor) {
    return new GrapheneBridgeEndpoint(
        browser,
        GrapheneBridgeOptions.defaults(),
        taskExecutor,
        new GrapheneBridgeExposureConfig(
            BrowserBridgePolicy.defaultPolicy(), browser.currentUrl(), ""));
  }

  private static void assertPolicyDenied(BrowserBridgePolicy policy) {
    TestBrowser browser = new TestBrowser();
    GrapheneBridgeEndpoint endpoint =
        new GrapheneBridgeEndpoint(
            browser,
            GrapheneBridgeOptions.defaults(),
            GrapheneTaskExecutor.direct(),
            new GrapheneBridgeExposureConfig(policy, browser.currentUrl(), ""));

    endpoint.onPageLoadEnd(browser.currentUrl());

    assertTrue(browser.executedScripts.isEmpty());
  }

  private static BridgeFrame mainFrame(String url) {
    return new BridgeFrame(url, true);
  }

  private static String extractRequestId(String script) {
    int idStart = script.indexOf("\\\"id\\\":\\\"") + "\\\"id\\\":\\\"".length();
    int idEnd = script.indexOf("\\\"", idStart);
    return script.substring(idStart, idEnd);
  }

  private static final class TestBrowser implements BridgeBrowser {
    private final List<String> executedScripts = new ArrayList<>();
    private String currentUrl = "app://assets/test/index.html";

    @Override
    public void executeScript(String script, String sourceUrl) {
      executedScripts.add(script);
    }

    @Override
    public String currentUrl() {
      return currentUrl;
    }

    @Override
    public boolean hasDocument() {
      return true;
    }

    @Override
    public int identifier() {
      return 1;
    }
  }

  private static final class TestQueryCallback implements BridgeQueryCallback {
    private String successResponse;

    @Override
    public void success(String response) {
      successResponse = response;
    }

    @Override
    public void failure(int errorCode, String errorMessage) {
      throw new AssertionError(errorCode + ": " + errorMessage);
    }
  }

  private static final class RecordingQueryCallback implements BridgeQueryCallback {
    private int failureCode;

    @Override
    public void success(String response) {
      throw new AssertionError("Unexpected query success: " + response);
    }

    @Override
    public void failure(int errorCode, String errorMessage) {
      failureCode = errorCode;
    }
  }

  private static final class QueuedTaskExecutor implements GrapheneTaskExecutor {
    private final List<Runnable> actions = new ArrayList<>();

    @Override
    public void execute(Runnable action) {
      actions.add(action);
    }

    @Override
    public <T> CompletableFuture<T> supply(Supplier<T> action) {
      try {
        return CompletableFuture.completedFuture(action.get());
      } catch (RuntimeException exception) {
        return CompletableFuture.failedFuture(exception);
      }
    }

    private void runAll() {
      List<Runnable> queuedActions = List.copyOf(actions);
      actions.clear();
      queuedActions.forEach(Runnable::run);
    }
  }
}
