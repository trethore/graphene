package tytoo.grapheneui.internal.browser.input.devtools;

import com.google.gson.JsonObject;
import org.cef.browser.CefDevToolsClient;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.Objects;

public final class GrapheneDevToolsMethodExecutor {
    private final GrapheneDevToolsTarget browser;

    public GrapheneDevToolsMethodExecutor(GrapheneDevToolsTarget browser) {
        this.browser = Objects.requireNonNull(browser, "browser");
    }

    public void executeMethod(String method, JsonObject payload, GrapheneDebugLogger debugLogger) {
        CefDevToolsClient devToolsClient = browser.getDevToolsClient();
        if (devToolsClient == null) {
            debugLogger.debug("Skipping DevTools input dispatch because the client is not available: {}", method);
            return;
        }

        debugLogger.debug("Dispatching DevTools input method {} with payload {}", method, payload);
        devToolsClient.executeDevToolsMethod(method, payload.toString()).exceptionally(throwable -> {
            debugLogger.debug("DevTools input dispatch failed for " + method, throwable);
            return null;
        });
    }
}
