package io.github.trethore.graphene.internal.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

final class GrapheneBridgeEndpointTest {
  @Test
  void injectsBootstrapAndFlushesQueuedMessagesAfterReadyHandshake() {
    TestBrowser browser = new TestBrowser();
    GrapheneBridgeEndpoint endpoint =
        new GrapheneBridgeEndpoint(
            browser, GrapheneBridgeOptions.defaults(), GrapheneTaskExecutor.direct());

    endpoint.emit("test:event", "{\"value\":1}");
    assertFalse(endpoint.isReady());
    assertTrue(browser.executedScripts.isEmpty());

    endpoint.onPageLoadEnd();
    assertEquals(3, browser.executedScripts.size());

    TestQueryCallback callback = new TestQueryCallback();
    boolean handled =
        endpoint.handleQuery(
            "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"ready\"}", callback);

    assertTrue(handled);
    assertTrue(endpoint.isReady());
    assertEquals("{}", callback.successResponse);
    assertEquals(4, browser.executedScripts.size());
    assertTrue(browser.executedScripts.get(3).contains("__grapheneBridgeReceiveFromJava"));
  }

  @Test
  void completesJavaRequestFromInboundResponse() {
    TestBrowser browser = new TestBrowser();
    GrapheneBridgeEndpoint endpoint =
        new GrapheneBridgeEndpoint(
            browser, GrapheneBridgeOptions.defaults(), GrapheneTaskExecutor.direct());
    TestQueryCallback readyCallback = new TestQueryCallback();
    endpoint.handleQuery(
        "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"ready\"}", readyCallback);

    CompletableFuture<String> response = endpoint.request("test:request", "{\"value\":2}");
    String outboundScript = browser.executedScripts.getLast();
    String requestId = extractRequestId(outboundScript);
    TestQueryCallback responseCallback = new TestQueryCallback();

    endpoint.handleQuery(
        "{\"bridge\":\"grapheneui\",\"version\":1,\"kind\":\"response\","
            + "\"id\":\""
            + requestId
            + "\",\"channel\":\"test:request\",\"ok\":true,\"payload\":{\"result\":4}}",
        responseCallback);

    assertEquals("{\"result\":4}", response.join());
    assertEquals("{}", responseCallback.successResponse);
  }

  private static String extractRequestId(String script) {
    int idStart = script.indexOf("\\\"id\\\":\\\"") + "\\\"id\\\":\\\"".length();
    int idEnd = script.indexOf("\\\"", idStart);
    return script.substring(idStart, idEnd);
  }

  private static final class TestBrowser implements BridgeBrowser {
    private final List<String> executedScripts = new ArrayList<>();

    @Override
    public void executeScript(String script, String sourceUrl) {
      executedScripts.add(script);
    }

    @Override
    public String currentUrl() {
      return "app://assets/test/index.html";
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
}
