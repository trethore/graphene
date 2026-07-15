package io.github.trethore.graphene.internal.bridge;

import com.google.gson.JsonElement;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeEventListener;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeRequestHandler;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeSubscription;
import io.github.trethore.graphene.api.browser.bridge.BrowserBridgeOrigin;
import io.github.trethore.graphene.api.browser.bridge.BrowserBridgePolicy;
import io.github.trethore.graphene.internal.logging.GrapheneDebugLogger;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneBridgeEndpoint implements GrapheneBridge {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgeEndpoint.class);
  private static final GrapheneDebugLogger DEBUG_LOGGER =
      GrapheneDebugLogger.of(GrapheneBridgeEndpoint.class);

  private static final String CHANNEL_NAME = "channel";
  private static final String LISTENER_NAME = "listener";
  private static final String TIMEOUT_NAME = "timeout";
  private static final long BOOTSTRAP_FALLBACK_RETRY_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

  private final BridgeBrowser browser;
  private final GrapheneBridgeOptions options;
  private final BrowserBridgePolicy policy;
  private final GrapheneBridgeDocumentClassifier documentClassifier;
  private final BrowserBridgeOrigin initialOrigin;
  private final GrapheneBridgeMessageCodec codec;
  private final GrapheneBridgeHandlerRegistry handlers;
  private final GrapheneBridgeOutboundQueue outboundQueue;
  private final GrapheneBridgeRequestLifecycle requestLifecycle;
  private final GrapheneBridgeInboundRouter inboundRouter;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicLong documentGeneration = new AtomicLong();
  private long lastBootstrapFallbackAttemptNanos;
  private String lastBootstrapFallbackUrl;
  private String readyUrl;
  private volatile ExposureState exposureState = ExposureState.PENDING;
  private volatile boolean hasEverBeenReady;

  GrapheneBridgeEndpoint(
      BridgeBrowser browser,
      GrapheneBridgeOptions options,
      GrapheneTaskExecutor taskExecutor,
      GrapheneBridgeExposureConfig exposureConfig) {
    this.browser = Objects.requireNonNull(browser, "browser");
    this.options = Objects.requireNonNull(options, "options");
    GrapheneBridgeExposureConfig validatedExposureConfig =
        Objects.requireNonNull(exposureConfig, "exposureConfig");
    this.policy = validatedExposureConfig.policy();
    this.documentClassifier =
        new GrapheneBridgeDocumentClassifier(validatedExposureConfig.grapheneHttpBaseUrl());
    this.initialOrigin =
        BrowserBridgeOrigin.fromUrl(validatedExposureConfig.initialUrl()).orElse(null);
    this.codec = new GrapheneBridgeMessageCodec(this.options.gson());
    this.handlers = new GrapheneBridgeHandlerRegistry(this.options.diagnostics());
    this.outboundQueue =
        new GrapheneBridgeOutboundQueue(
            this::dispatchToDom,
            this.options.maxQueuedOutboundMessages(),
            this.options.queueOverflowPolicy(),
            this.options.diagnostics());
    this.requestLifecycle = new GrapheneBridgeRequestLifecycle(codec, outboundQueue);
    this.inboundRouter =
        new GrapheneBridgeInboundRouter(codec, handlers, requestLifecycle, taskExecutor);

    DEBUG_LOGGER.debug(
        "Created bridge endpoint browserId={} maxQueuedMessages={} overflowPolicy={} defaultTimeoutMs={}",
        browserIdentifier(),
        this.options.maxQueuedOutboundMessages(),
        this.options.queueOverflowPolicy(),
        this.options.defaultRequestTimeout().toMillis());
  }

  @Override
  public boolean isReady() {
    return exposureState == ExposureState.ALLOWED && outboundQueue.isReady() && !closed.get();
  }

  @Override
  public GrapheneBridgeSubscription onReady(Runnable listener) {
    Objects.requireNonNull(listener, LISTENER_NAME);
    ensureOpen();
    return handlers.onReady(listener, isReady());
  }

  @Override
  public GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
    return onEventValidated(validateConsumerChannel(channel), listener);
  }

  @Override
  public GrapheneBridgeSubscription onRequest(
      String channel, GrapheneBridgeRequestHandler handler) {
    return onRequestValidated(validateConsumerChannel(channel), handler);
  }

  @Override
  public void emit(String channel, String payloadJson) {
    emitValidated(validateConsumerChannel(channel), payloadJson);
  }

  @Override
  public CompletableFuture<String> request(String channel, String payloadJson) {
    return request(channel, payloadJson, options.defaultRequestTimeout());
  }

  @Override
  public <T> CompletableFuture<T> requestJson(
      String channel, Object payload, Class<T> responseType) {
    return requestJson(channel, payload, options.defaultRequestTimeout(), responseType);
  }

  @Override
  public CompletableFuture<String> request(String channel, String payloadJson, Duration timeout) {
    String validatedChannel = validateConsumerChannel(channel);
    Duration validatedTimeout = validateTimeout(timeout);
    ensureOpen();
    ensureOutboundAvailable();
    DEBUG_LOGGER.debugIfEnabled(
        logger -> {
          int payloadSize = payloadJson == null ? 0 : payloadJson.length();
          logger.debug(
              "Queued bridge request channel={} timeoutMs={} payloadSize={}",
              validatedChannel,
              validatedTimeout.toMillis(),
              payloadSize);
        });
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
    if (hasEverBeenReady) {
      outboundQueue.clear();
    }
    requestLifecycle.failAllForPageChange();
    documentGeneration.incrementAndGet();
    exposureState = ExposureState.PENDING;
    lastBootstrapFallbackAttemptNanos = 0L;
    lastBootstrapFallbackUrl = null;
    readyUrl = null;
    DEBUG_LOGGER.debug(
        "Bridge endpoint marked not ready for navigation browserId={}", browserIdentifier());
  }

  public void onPageLoadEnd(String documentUrl) {
    if (closed.get()) {
      return;
    }

    String validatedDocumentUrl = Objects.requireNonNull(documentUrl, "documentUrl");
    if (!allows(validatedDocumentUrl)) {
      denyDocument(validatedDocumentUrl);
      return;
    }

    try {
      injectBootstrapScript(validatedDocumentUrl);
      exposureState = ExposureState.ALLOWED;
      DEBUG_LOGGER.debug(
          "Injected bridge bootstrap on load end browserId={} url={}",
          browserIdentifier(),
          validatedDocumentUrl);
    } catch (RuntimeException exception) {
      exposureState = ExposureState.PENDING;
      DEBUG_LOGGER.debug(
          "Bridge bootstrap injection failed on load end browserId={} url={} reason={}",
          browserIdentifier(),
          validatedDocumentUrl,
          exception.getMessage());
      DEBUG_LOGGER.debug("Bridge bootstrap load-end failure stack trace", exception);
      // Bridge bootstrap will be retried by the fallback path during rendering.
    }
  }

  public boolean handleQuery(BridgeFrame frame, String requestJson, BridgeQueryCallback callback) {
    if (closed.get()) {
      return false;
    }

    BridgeFrame validatedFrame = Objects.requireNonNull(frame, "frame");
    BridgeQueryCallback validatedCallback = Objects.requireNonNull(callback, "callback");
    if (exposureState != ExposureState.ALLOWED
        || !validatedFrame.mainFrame()
        || !allows(validatedFrame.url())) {
      validatedCallback.failure(403, "Graphene bridge access is denied for this document");
      return true;
    }

    long queryDocumentGeneration = documentGeneration.get();
    boolean handled =
        inboundRouter.route(
            requestJson,
            validatedCallback,
            () -> onBridgeReady(queryDocumentGeneration, validatedFrame.url()));
    DEBUG_LOGGER.debugIfEnabled(
        logger -> {
          int requestSize = requestJson == null ? 0 : requestJson.length();
          logger.debug(
              "Bridge endpoint handled query browserId={} requestSize={} handled={}",
              browserIdentifier(),
              requestSize,
              handled);
        });

    return handled;
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
    DEBUG_LOGGER.debug("Closed bridge endpoint browserId={}", browserIdentifier());
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

      String previousReadyUrl = readyUrl;
      outboundQueue.markNotReady();
      requestLifecycle.failAllForPageChange();
      readyUrl = null;
      lastBootstrapFallbackAttemptNanos = 0L;
      lastBootstrapFallbackUrl = null;
      DEBUG_LOGGER.debug(
          "Bridge ready URL changed browserId={} previousUrl={} currentUrl={}",
          browserIdentifier(),
          previousReadyUrl,
          currentUrl);
    }

    if (!browserHasDocument()) {
      return;
    }

    long nowNanos = System.nanoTime();
    boolean urlChanged = !Objects.equals(lastBootstrapFallbackUrl, currentUrl);
    if (!urlChanged
        && nowNanos - lastBootstrapFallbackAttemptNanos < BOOTSTRAP_FALLBACK_RETRY_NANOS) {
      return;
    }

    lastBootstrapFallbackAttemptNanos = nowNanos;
    lastBootstrapFallbackUrl = currentUrl;

    if (!allows(currentUrl)) {
      denyDocument(currentUrl);
      return;
    }

    try {
      injectBootstrapScript(currentUrl);
      exposureState = ExposureState.ALLOWED;
      DEBUG_LOGGER.debug(
          "Injected bridge bootstrap fallback browserId={} url={}",
          browserIdentifier(),
          currentUrl);
    } catch (RuntimeException exception) {
      DEBUG_LOGGER.debug(
          "Bridge bootstrap fallback injection failed browserId={} url={} reason={}",
          browserIdentifier(),
          currentUrl,
          exception.getMessage());
      DEBUG_LOGGER.debug("Bridge bootstrap fallback failure stack trace", exception);
      // Bridge bootstrap will be retried by the fallback path during rendering.
    }
  }

  GrapheneBridgeSubscription onInternalEvent(String channel, GrapheneBridgeEventListener listener) {
    return onEventValidated(validateInternalChannel(channel), listener);
  }

  GrapheneBridgeSubscription onInternalRequest(
      String channel, GrapheneBridgeRequestHandler handler) {
    return onRequestValidated(validateInternalChannel(channel), handler);
  }

  void emitInternal(String channel, String payloadJson) {
    emitValidated(validateInternalChannel(channel), payloadJson);
  }

  private void onBridgeReady(long readyDocumentGeneration, String documentUrl) {
    if (!canActivateDocument(readyDocumentGeneration)) {
      return;
    }

    outboundQueue.markReadyAndFlush();
    if (!canActivateDocument(readyDocumentGeneration)) {
      outboundQueue.markNotReady();
      return;
    }
    readyUrl = documentUrl;
    hasEverBeenReady = true;
    handlers.notifyReady();
    DEBUG_LOGGER.debug(
        "Bridge endpoint ready browserId={} readyUrl={}", browserIdentifier(), readyUrl);
  }

  private boolean canActivateDocument(long readyDocumentGeneration) {
    return !closed.get()
        && exposureState == ExposureState.ALLOWED
        && documentGeneration.get() == readyDocumentGeneration;
  }

  private GrapheneBridgeSubscription onEventValidated(
      String validatedChannel, GrapheneBridgeEventListener listener) {
    Objects.requireNonNull(listener, LISTENER_NAME);
    ensureOpen();
    return handlers.onEvent(validatedChannel, listener);
  }

  private GrapheneBridgeSubscription onRequestValidated(
      String validatedChannel, GrapheneBridgeRequestHandler handler) {
    Objects.requireNonNull(handler, "handler");
    ensureOpen();
    return handlers.onRequest(validatedChannel, handler);
  }

  private void emitValidated(String validatedChannel, String payloadJson) {
    JsonElement payload = codec.parsePayloadJson(payloadJson);
    ensureOpen();
    ensureOutboundAvailable();

    String outboundJson =
        codec.createOutboundPacketJson(
            GrapheneBridgeProtocol.KIND_EVENT, null, validatedChannel, payload);
    queueOrDispatch(outboundJson);
    DEBUG_LOGGER.debugIfEnabled(
        logger -> {
          int payloadSize = payloadJson == null ? 0 : payloadJson.length();
          logger.debug(
              "Queued bridge event channel={} payloadSize={}", validatedChannel, payloadSize);
        });
  }

  private void injectBootstrapScript(String scriptUrl) {
    List<String> bootstrapScripts = GrapheneBridgeScriptLoader.scripts();
    for (String script : bootstrapScripts) {
      browser.executeScript(script, scriptUrl);
    }
  }

  private void queueOrDispatch(String outboundPacketJson) {
    outboundQueue.queueOrDispatch(outboundPacketJson);
  }

  private void dispatchToDom(String outboundPacketJson) {
    String documentUrl = currentUrl();
    if (exposureState != ExposureState.ALLOWED) {
      denyDocument(documentUrl);
      throw new IllegalStateException("Bridge is unavailable for the current document");
    }
    String script =
        "window.__grapheneBridgeReceiveFromJava(" + codec.quoteJsString(outboundPacketJson) + ");";
    browser.executeScript(script, documentUrl);
  }

  private void ensureOpen() {
    if (closed.get()) {
      throw new IllegalStateException("Bridge is closed");
    }
  }

  private void ensureOutboundAvailable() {
    if (exposureState == ExposureState.DENIED) {
      throw new IllegalStateException("Bridge is unavailable for the current document");
    }
  }

  private boolean allows(String documentUrl) {
    Optional<BrowserBridgeOrigin> documentOrigin = BrowserBridgeOrigin.fromUrl(documentUrl);
    BrowserBridgePolicy.Request request =
        new BrowserBridgePolicy.Request(
            documentUrl,
            documentClassifier.classify(documentUrl, documentOrigin.orElse(null)),
            documentOrigin,
            Optional.ofNullable(initialOrigin));
    try {
      BrowserBridgePolicy.Decision decision = policy.decide(request);
      if (decision != null) {
        return decision == BrowserBridgePolicy.Decision.ALLOW;
      }
      LOGGER.warn("Bridge policy returned null for {}", documentUrl);
    } catch (RuntimeException exception) {
      LOGGER.warn("Bridge policy failed for {}", documentUrl, exception);
    }
    return false;
  }

  private void denyDocument(String documentUrl) {
    exposureState = ExposureState.DENIED;
    outboundQueue.markNotReady();
    outboundQueue.clear();
    requestLifecycle.failAllForPageChange();
    readyUrl = null;
    DEBUG_LOGGER.debug(
        "Bridge exposure denied browserId={} url={}", browserIdentifier(), documentUrl);
  }

  private String validateChannel(String channel) {
    Objects.requireNonNull(channel, CHANNEL_NAME);
    if (channel.isBlank()) {
      throw new IllegalArgumentException(CHANNEL_NAME + " must not be blank");
    }

    return channel;
  }

  private String validateConsumerChannel(String channel) {
    String validatedChannel = validateChannel(channel);
    if (validatedChannel.startsWith(GrapheneBridge.RESERVED_CHANNEL_PREFIX)) {
      throw new IllegalArgumentException(
          CHANNEL_NAME
              + " must not use the reserved prefix "
              + GrapheneBridge.RESERVED_CHANNEL_PREFIX);
    }
    return validatedChannel;
  }

  private String validateInternalChannel(String channel) {
    String validatedChannel = validateChannel(channel);
    if (!validatedChannel.startsWith(GrapheneBridge.RESERVED_CHANNEL_PREFIX)) {
      throw new IllegalArgumentException(
          CHANNEL_NAME + " must use the reserved prefix " + GrapheneBridge.RESERVED_CHANNEL_PREFIX);
    }
    return validatedChannel;
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
    } catch (RuntimeException ignored) {
      // Browser state is transient while creating/navigating.
      return false;
    }
  }

  private int browserIdentifier() {
    try {
      return browser.identifier();
    } catch (RuntimeException ignored) {
      return -1;
    }
  }

  private enum ExposureState {
    PENDING,
    ALLOWED,
    DENIED
  }
}
