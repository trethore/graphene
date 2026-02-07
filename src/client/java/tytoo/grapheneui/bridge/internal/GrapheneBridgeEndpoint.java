package tytoo.grapheneui.bridge.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import org.cef.callback.CefQueryCallback;
import tytoo.grapheneui.GrapheneCore;
import tytoo.grapheneui.bridge.GrapheneBridge;
import tytoo.grapheneui.bridge.GrapheneBridgeEventListener;
import tytoo.grapheneui.bridge.GrapheneBridgeRequestHandler;
import tytoo.grapheneui.bridge.GrapheneBridgeSubscription;
import tytoo.grapheneui.browser.GrapheneBrowser;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class GrapheneBridgeEndpoint implements GrapheneBridge {
    private static final String CHANNEL_NAME = "channel";
    private static final String TIMEOUT_NAME = "timeout";
    private static final GrapheneBridgeMessageCodec CODEC = new GrapheneBridgeMessageCodec();

    private final GrapheneBrowser browser;
    private final GrapheneBridgeHandlerRegistry handlers = new GrapheneBridgeHandlerRegistry();
    private final GrapheneBridgePendingRequests pendingRequests = new GrapheneBridgePendingRequests();
    private final Object outboundQueueLock = new Object();
    private final ArrayDeque<String> outboundMessageQueue = new ArrayDeque<>();
    private final AtomicLong requestSequence = new AtomicLong();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public GrapheneBridgeEndpoint(GrapheneBrowser browser) {
        this.browser = Objects.requireNonNull(browser, "browser");
    }

    @Override
    public boolean isReady() {
        return ready.get() && !closed.get();
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
        JsonElement payload = CODEC.parsePayloadJson(payloadJson);
        ensureOpen();

        String outboundJson = CODEC.createOutboundPacketJson(GrapheneBridgeProtocol.KIND_EVENT, null, validatedChannel, payload);
        queueOrDispatch(outboundJson);
    }

    @Override
    public CompletableFuture<String> request(String channel, String payloadJson, Duration timeout) {
        String validatedChannel = validateChannel(channel);
        Duration validatedTimeout = validateTimeout(timeout);
        JsonElement payload = CODEC.parsePayloadJson(payloadJson);
        ensureOpen();

        String requestId = "java-" + requestSequence.incrementAndGet();
        CompletableFuture<String> responseFuture = pendingRequests.register(requestId, validatedTimeout);

        try {
            String outboundJson = CODEC.createOutboundPacketJson(GrapheneBridgeProtocol.KIND_REQUEST, requestId, validatedChannel, payload);
            queueOrDispatch(outboundJson);
        } catch (RuntimeException exception) {
            pendingRequests.completeFailure(requestId, exception);
        }

        return responseFuture;
    }

    public void onPageLoadStart() {
        if (closed.get()) {
            return;
        }

        ready.set(false);
        pendingRequests.failAll(new IllegalStateException("Bridge page changed before a response was received"));
    }

    public void onPageLoadEnd() {
        if (closed.get()) {
            return;
        }

        injectBootstrapScript();
    }

    public boolean handleQuery(String requestJson, CefQueryCallback callback) {
        if (closed.get()) {
            return false;
        }

        GrapheneBridgePacket packet = CODEC.parsePacket(requestJson);
        if (packet == null) {
            return false;
        }

        if (packet.version != GrapheneBridgeProtocol.VERSION) {
            callback.failure(422, "Unsupported bridge protocol version: " + packet.version);
            return true;
        }

        if (packet.kind == null || packet.kind.isBlank()) {
            callback.failure(400, "Bridge message is missing kind");
            return true;
        }

        switch (packet.kind) {
            case GrapheneBridgeProtocol.KIND_READY -> {
                callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
                onBridgeReady();
            }
            case GrapheneBridgeProtocol.KIND_EVENT -> handleDomEvent(packet, callback);
            case GrapheneBridgeProtocol.KIND_REQUEST -> handleDomRequest(packet, callback);
            case GrapheneBridgeProtocol.KIND_RESPONSE -> {
                callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
                handleDomResponse(packet);
            }
            default -> callback.failure(400, "Unknown bridge message kind: " + packet.kind);
        }

        return true;
    }

    public void onQueryCanceled(long queryId) {
        // CEF query cancellation callbacks are not correlated with the bridge request protocol.
        GrapheneCore.LOGGER.trace("Bridge query {} canceled by CEF", queryId);
    }

    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }

        ready.set(false);
        clearQueuedOutboundMessages();
        pendingRequests.failAll(new IllegalStateException("Bridge closed"));
        handlers.clear();
    }

    private void onBridgeReady() {
        if (closed.get()) {
            return;
        }

        ready.set(true);
        flushQueuedOutboundMessages();
        handlers.notifyReady();
    }

    private void handleDomEvent(GrapheneBridgePacket packet, CefQueryCallback callback) {
        if (packet.channel == null || packet.channel.isBlank()) {
            callback.failure(400, "Bridge event is missing channel");
            return;
        }

        callback.success(GrapheneBridgeProtocol.EMPTY_RESPONSE_JSON);
        handlers.dispatchEvent(packet.channel, CODEC.payloadToJson(packet.payload));
    }

    private void handleDomRequest(GrapheneBridgePacket packet, CefQueryCallback callback) {
        if (packet.id == null || packet.id.isBlank()) {
            callback.success(CODEC.createErrorResponseJson(null, packet.channel, "invalid_request", "Bridge request is missing id"));
            return;
        }

        if (packet.channel == null || packet.channel.isBlank()) {
            callback.success(CODEC.createErrorResponseJson(packet.id, null, "invalid_request", "Bridge request is missing channel"));
            return;
        }

        GrapheneBridgeRequestHandler requestHandler = handlers.requestHandler(packet.channel);
        if (requestHandler == null) {
            callback.success(
                    CODEC.createErrorResponseJson(
                            packet.id,
                            packet.channel,
                            "handler_not_found",
                            "No Java bridge handler for channel '" + packet.channel + "'"
                    )
            );
            return;
        }

        CompletableFuture<String> responseFuture;
        try {
            responseFuture = requestHandler.handle(packet.channel, CODEC.payloadToJson(packet.payload));
        } catch (RuntimeException exception) {
            callback.success(CODEC.createErrorResponseJson(packet.id, packet.channel, "java_handler_error", exception.getMessage()));
            return;
        }

        if (responseFuture == null) {
            callback.success(CODEC.createSuccessResponseJson(packet.id, packet.channel, JsonNull.INSTANCE));
            return;
        }

        responseFuture.whenComplete((responsePayloadJson, throwable) -> {
            if (throwable != null) {
                Throwable rootCause = unwrap(throwable);
                callback.success(CODEC.createErrorResponseJson(packet.id, packet.channel, "java_handler_error", rootCause.getMessage()));
                return;
            }

            JsonElement responsePayload;
            try {
                responsePayload = CODEC.parsePayloadJson(responsePayloadJson);
            } catch (IllegalArgumentException exception) {
                callback.success(CODEC.createErrorResponseJson(packet.id, packet.channel, "invalid_response", exception.getMessage()));
                return;
            }

            callback.success(CODEC.createSuccessResponseJson(packet.id, packet.channel, responsePayload));
        });
    }

    private void handleDomResponse(GrapheneBridgePacket packet) {
        if (packet.id == null || packet.id.isBlank()) {
            return;
        }

        if (Boolean.FALSE.equals(packet.ok)) {
            String errorCode = packet.error == null || packet.error.code == null ? "bridge_error" : packet.error.code;
            String errorMessage = packet.error == null || packet.error.message == null ? "Bridge request failed" : packet.error.message;
            pendingRequests.completeFailure(packet.id, new IllegalStateException(errorCode + ": " + errorMessage));
            return;
        }

        pendingRequests.completeSuccess(packet.id, CODEC.payloadToJson(packet.payload));
    }

    private void injectBootstrapScript() {
        browser.executeScript(GrapheneBridgeScriptLoader.script(), currentUrl());
    }

    private void queueOrDispatch(String outboundPacketJson) {
        if (ready.get()) {
            dispatchToDom(outboundPacketJson);
            return;
        }

        synchronized (outboundQueueLock) {
            if (ready.get()) {
                dispatchToDom(outboundPacketJson);
                return;
            }

            outboundMessageQueue.addLast(outboundPacketJson);
        }
    }

    private void flushQueuedOutboundMessages() {
        List<String> queuedMessages = new ArrayList<>();
        synchronized (outboundQueueLock) {
            while (!outboundMessageQueue.isEmpty()) {
                queuedMessages.add(outboundMessageQueue.removeFirst());
            }
        }

        for (String message : queuedMessages) {
            dispatchToDom(message);
        }
    }

    private void clearQueuedOutboundMessages() {
        synchronized (outboundQueueLock) {
            outboundMessageQueue.clear();
        }
    }

    private void dispatchToDom(String outboundPacketJson) {
        String script = "window.__grapheneBridgeReceiveFromJava(" + CODEC.quoteJsString(outboundPacketJson) + ");";
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

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return completionException.getCause();
        }

        return throwable;
    }

    private String currentUrl() {
        return browser.currentUrl();
    }
}
