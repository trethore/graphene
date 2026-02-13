package tytoo.grapheneui.internal.bridge;

import com.google.gson.JsonElement;
import org.cef.callback.CefQueryCallback;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeEventListener;
import tytoo.grapheneui.api.bridge.GrapheneBridgeRequestHandler;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GrapheneBridgeEndpoint implements GrapheneBridge {
    private static final String CHANNEL_NAME = "channel";
    private static final String TIMEOUT_NAME = "timeout";
    private static final long BOOTSTRAP_FALLBACK_RETRY_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    private final GrapheneBrowser browser;
    private final GrapheneBridgeOptions options;
    private final GrapheneBridgeMessageCodec codec;
    private final GrapheneBridgeHandlerRegistry handlers;
    private final GrapheneBridgeOutboundQueue outboundQueue;
    private final GrapheneBridgeRequestLifecycle requestLifecycle;
    private final GrapheneBridgeInboundRouter inboundRouter;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private long lastBootstrapFallbackAttemptNanos;
    private String lastBootstrapFallbackUrl;
    private String readyUrl;

    public GrapheneBridgeEndpoint(GrapheneBrowser browser) {
        this(browser, GrapheneBridgeOptions.defaults());
    }

    GrapheneBridgeEndpoint(GrapheneBrowser browser, GrapheneBridgeOptions options) {
        this.browser = Objects.requireNonNull(browser, "browser");
        this.options = Objects.requireNonNull(options, "options");
        this.codec = new GrapheneBridgeMessageCodec(this.options.gson());
        this.handlers = new GrapheneBridgeHandlerRegistry(this.options.diagnostics());
        this.outboundQueue = new GrapheneBridgeOutboundQueue(
                this::dispatchToDom,
                this.options.maxQueuedOutboundMessages(),
                this.options.queueOverflowPolicy(),
                this.options.diagnostics()
        );
        this.requestLifecycle = new GrapheneBridgeRequestLifecycle(codec, outboundQueue);
        this.inboundRouter = new GrapheneBridgeInboundRouter(codec, handlers, requestLifecycle, this::onBridgeReady);
    }

    @Override
    public boolean isReady() {
        return outboundQueue.isReady() && !closed.get();
    }

    @Override
    public GrapheneBridgeSubscription onReady(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        ensureOpen();
        return handlers.onReady(listener, isReady());
    }

    @Override
    public GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
        Objects.requireNonNull(listener, "listener");
        String validatedChannel = validateChannel(channel);
        ensureOpen();
        return handlers.onEvent(validatedChannel, listener);
    }

    @Override
    public GrapheneBridgeSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler) {
        Objects.requireNonNull(handler, "handler");
        String validatedChannel = validateChannel(channel);
        ensureOpen();
        return handlers.onRequest(validatedChannel, handler);
    }

    @Override
    public void emit(String channel, String payloadJson) {
        String validatedChannel = validateChannel(channel);
        JsonElement payload = codec.parsePayloadJson(payloadJson);
        ensureOpen();

        String outboundJson = codec.createOutboundPacketJson(GrapheneBridgeProtocol.KIND_EVENT, null, validatedChannel, payload);
        queueOrDispatch(outboundJson);
    }

    @Override
    public CompletableFuture<String> request(String channel, String payloadJson) {
        return request(channel, payloadJson, options.defaultRequestTimeout());
    }

    @Override
    public <T> CompletableFuture<T> requestJson(String channel, Object payload, Class<T> responseType) {
        return requestJson(channel, payload, options.defaultRequestTimeout(), responseType);
    }

    @Override
    public CompletableFuture<String> request(String channel, String payloadJson, Duration timeout) {
        String validatedChannel = validateChannel(channel);
        Duration validatedTimeout = validateTimeout(timeout);
        ensureOpen();
        return requestLifecycle.request(validatedChannel, payloadJson, validatedTimeout);
    }

    public void onPageLoadStart() {
        onNavigationRequested();
    }

    public void onNavigationRequested() {
        if (closed.get()) {
            return;
        }

        outboundQueue.markNotReady();
        requestLifecycle.failAllForPageChange();
        lastBootstrapFallbackAttemptNanos = 0L;
        lastBootstrapFallbackUrl = null;
        readyUrl = null;
    }

    public void onPageLoadEnd() {
        if (closed.get()) {
            return;
        }

        try {
            injectBootstrapScript();
        } catch (RuntimeException _) {
            // Bridge bootstrap will be retried by the fallback path during rendering.
        }
    }

    public boolean handleQuery(String requestJson, CefQueryCallback callback) {
        if (closed.get()) {
            return false;
        }

        return inboundRouter.route(requestJson, callback);
    }

    public void onQueryCanceled() {
        // no-op
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        outboundQueue.markNotReady();
        outboundQueue.clear();
        requestLifecycle.failAllForClose();
        handlers.clear();
    }

    private void onBridgeReady() {
        if (closed.get()) {
            return;
        }

        outboundQueue.markReadyAndFlush();
        readyUrl = currentUrl();
        handlers.notifyReady();
    }

    void tryBootstrapFallback() {
        if (closed.get()) {
            return;
        }

        String currentUrl = currentUrl();
        if (outboundQueue.isReady()) {
            if (Objects.equals(readyUrl, currentUrl)) {
                return;
            }

            outboundQueue.markNotReady();
            requestLifecycle.failAllForPageChange();
            readyUrl = null;
            lastBootstrapFallbackAttemptNanos = 0L;
            lastBootstrapFallbackUrl = null;
        }

        if (!browserHasDocument()) {
            return;
        }

        long nowNanos = System.nanoTime();
        boolean urlChanged = !Objects.equals(lastBootstrapFallbackUrl, currentUrl);
        if (!urlChanged && nowNanos - lastBootstrapFallbackAttemptNanos < BOOTSTRAP_FALLBACK_RETRY_NANOS) {
            return;
        }

        lastBootstrapFallbackAttemptNanos = nowNanos;
        lastBootstrapFallbackUrl = currentUrl;

        try {
            injectBootstrapScript();
        } catch (RuntimeException _) {
            // Bridge bootstrap will be retried by the fallback path during rendering.
        }
    }

    private void injectBootstrapScript() {
        String scriptUrl = currentUrl();
        List<String> bootstrapScripts = GrapheneBridgeScriptLoader.scripts();
        for (String script : bootstrapScripts) {
            browser.executeScript(script, scriptUrl);
        }
    }

    private void queueOrDispatch(String outboundPacketJson) {
        outboundQueue.queueOrDispatch(outboundPacketJson);
    }

    private void dispatchToDom(String outboundPacketJson) {
        String script = "window.__grapheneBridgeReceiveFromJava(" + codec.quoteJsString(outboundPacketJson) + ");";
        browser.executeScript(script, currentUrl());
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("Bridge is closed");
        }
    }

    private String validateChannel(String channel) {
        Objects.requireNonNull(channel, CHANNEL_NAME);
        if (channel.isBlank()) {
            throw new IllegalArgumentException(CHANNEL_NAME + " must not be blank");
        }

        return channel;
    }

    private Duration validateTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, TIMEOUT_NAME);
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(TIMEOUT_NAME + " must be > 0");
        }

        return timeout;
    }

    private String currentUrl() {
        return browser.currentUrl();
    }

    private boolean browserHasDocument() {
        try {
            return browser.hasDocument();
        } catch (RuntimeException _) {
            // Browser state is transient while creating/navigating.
            return false;
        }
    }
}
