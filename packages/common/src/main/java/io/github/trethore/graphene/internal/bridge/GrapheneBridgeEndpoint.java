package io.github.trethore.graphene.internal.bridge;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeEventListener;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeRequestHandler;
import io.github.trethore.graphene.api.browser.bridge.BrowserBridgeOrigin;
import io.github.trethore.graphene.api.browser.bridge.BrowserBridgePolicy;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneBridgeEndpoint implements GrapheneBridge {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgeEndpoint.class);

  private static final String CHANNEL_NAME = "channel";
  private static final String CLIPBOARD_PASTE_FUNCTION = "__grapheneClipboardPasteFromHost";
  private static final int MAX_QUEUED_OUTBOUND_MESSAGES = 1024;
  private static final String LISTENER_NAME = "listener";
  private static final String TIMEOUT_NAME = "timeout";
  private static final long BOOTSTRAP_FALLBACK_RETRY_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

  private final BridgeBrowser browser;
  private final BrowserBridgePolicy policy;
  private final GrapheneBridgeDocumentClassifier documentClassifier;
  private final BrowserBridgeOrigin initialOrigin;
  private final GrapheneBridgeMessageCodec codec;
  private final GrapheneBridgeHandlerRegistry handlers;
  private final GrapheneBridgeOutboundQueue outboundQueue;
  private final GrapheneBridgeRequestLifecycle requestLifecycle;
  private final GrapheneBridgeInboundRouter inboundRouter;
  private final GrapheneBridgeClipboardAccess clipboardAccess = new GrapheneBridgeClipboardAccess();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicLong documentGeneration = new AtomicLong();
  private final AtomicReference<DocumentIdentity> documentScriptsDocument = new AtomicReference<>();
  private long lastBootstrapFallbackAttemptNanos;
  private String lastBootstrapFallbackUrl;
  private String readyUrl;
  private volatile ExposureState exposureState = ExposureState.PENDING;
  private volatile boolean hasEverBeenReady;

  GrapheneBridgeEndpoint(
      BridgeBrowser browser,
      GrapheneTaskExecutor taskExecutor,
      GrapheneBridgeExposureConfig exposureConfig) {
    this.browser = Objects.requireNonNull(browser, "browser");
    GrapheneBridgeExposureConfig validatedExposureConfig =
        Objects.requireNonNull(exposureConfig, "exposureConfig");
    this.policy = validatedExposureConfig.policy();
    this.documentClassifier =
        new GrapheneBridgeDocumentClassifier(validatedExposureConfig.grapheneHttpBaseUrl());
    this.initialOrigin =
        BrowserBridgeOrigin.fromUrl(validatedExposureConfig.initialUrl()).orElse(null);
    this.codec = new GrapheneBridgeMessageCodec(new Gson());
    this.handlers = new GrapheneBridgeHandlerRegistry();
    this.outboundQueue =
        new GrapheneBridgeOutboundQueue(this::dispatchToDom, MAX_QUEUED_OUTBOUND_MESSAGES);
    this.requestLifecycle = new GrapheneBridgeRequestLifecycle(codec, outboundQueue);
    this.inboundRouter =
        new GrapheneBridgeInboundRouter(codec, handlers, requestLifecycle, taskExecutor);

    LOGGER.debug(
        "Created bridge endpoint browserId={} maxQueuedMessages={} defaultTimeoutMs={}",
        browserIdentifier(),
        MAX_QUEUED_OUTBOUND_MESSAGES,
        DEFAULT_REQUEST_TIMEOUT.toMillis());
  }

  @Override
  public boolean isReady() {
    return exposureState == ExposureState.ALLOWED && outboundQueue.isReady() && !closed.get();
  }

  @Override
  public GrapheneSubscription onReady(Runnable listener) {
    Objects.requireNonNull(listener, LISTENER_NAME);
    ensureOpen();
    return handlers.onReady(listener, isReady());
  }

  @Override
  public GrapheneSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
    return onEventValidated(validateConsumerChannel(channel), listener);
  }

  @Override
  public GrapheneSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler) {
    return onRequestValidated(validateConsumerChannel(channel), handler);
  }

  @Override
  public void emit(String channel, String payloadJson) {
    emitValidated(validateConsumerChannel(channel), payloadJson);
  }

  @Override
  public CompletableFuture<String> request(String channel, String payloadJson, Duration timeout) {
    String validatedChannel = validateConsumerChannel(channel);
    Duration validatedTimeout = validateTimeout(timeout);
    ensureOpen();
    ensureOutboundAvailable();
    if (LOGGER.isDebugEnabled()) {
      int payloadSize = payloadJson == null ? 0 : payloadJson.length();
      LOGGER.debug(
          "Queued bridge request channel={} timeoutMs={} payloadSize={}",
          validatedChannel,
          validatedTimeout.toMillis(),
          payloadSize);
    }
    return requestLifecycle.request(validatedChannel, payloadJson, validatedTimeout);
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
    LOGGER.debug(
        "Bridge endpoint marked not ready for navigation browserId={}", browserIdentifier());
  }

  public void onPageLoadEnd(String documentUrl) {
    if (closed.get()) {
      return;
    }

    String validatedDocumentUrl = Objects.requireNonNull(documentUrl, "documentUrl");
    if (!allows(validatedDocumentUrl)) {
      tryInjectDocumentScripts(validatedDocumentUrl, "load end");
      denyDocument(validatedDocumentUrl);
      return;
    }

    try {
      injectBootstrapScript(validatedDocumentUrl);
      documentScriptsDocument.set(documentIdentity(validatedDocumentUrl));
      exposureState = ExposureState.ALLOWED;
      LOGGER.debug(
          "Injected bridge bootstrap on load end browserId={} url={}",
          browserIdentifier(),
          validatedDocumentUrl);
    } catch (RuntimeException exception) {
      exposureState = ExposureState.PENDING;
      LOGGER.debug(
          "Bridge bootstrap injection failed on load end browserId={} url={} reason={}",
          browserIdentifier(),
          validatedDocumentUrl,
          exception.getMessage());
      LOGGER.debug("Bridge bootstrap load-end failure stack trace", exception);
      // Bridge bootstrap will be retried by the fallback path during rendering.
    }
  }

  public boolean handleQuery(BridgeFrame frame, String requestJson, BridgeQueryCallback callback) {
    if (closed.get()) {
      return false;
    }

    BridgeFrame validatedFrame = Objects.requireNonNull(frame, "frame");
    BridgeQueryCallback validatedCallback = Objects.requireNonNull(callback, "callback");
    boolean bridgeAllowed =
        exposureState == ExposureState.ALLOWED
            && validatedFrame.mainFrame()
            && allows(validatedFrame.url());
    if (!bridgeAllowed && !allowsAuthorizedClipboardWrite(validatedFrame, requestJson)) {
      validatedCallback.failure(403, "Graphene bridge access is denied for this document");
      return true;
    }

    long queryDocumentGeneration = documentGeneration.get();
    boolean handled =
        inboundRouter.route(
            requestJson,
            validatedCallback,
            () -> onBridgeReady(queryDocumentGeneration, validatedFrame.url()));
    if (LOGGER.isDebugEnabled()) {
      int requestSize = requestJson == null ? 0 : requestJson.length();
      LOGGER.debug(
          "Bridge endpoint handled query browserId={} requestSize={} handled={}",
          browserIdentifier(),
          requestSize,
          handled);
    }

    return handled;
  }

  public void close() {
    if (!closed.compareAndSet(false, true)) {
      return;
    }

    outboundQueue.markNotReady();
    outboundQueue.clear();
    requestLifecycle.failAllForClose();
    handlers.clear();
    LOGGER.debug("Closed bridge endpoint browserId={}", browserIdentifier());
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
      LOGGER.debug(
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
      tryInjectDocumentScripts(currentUrl, "fallback");
      denyDocument(currentUrl);
      return;
    }

    try {
      injectBootstrapScript(currentUrl);
      documentScriptsDocument.set(documentIdentity(currentUrl));
      exposureState = ExposureState.ALLOWED;
      LOGGER.debug(
          "Injected bridge bootstrap fallback browserId={} url={}",
          browserIdentifier(),
          currentUrl);
    } catch (RuntimeException exception) {
      LOGGER.debug(
          "Bridge bootstrap fallback injection failed browserId={} url={} reason={}",
          browserIdentifier(),
          currentUrl,
          exception.getMessage());
      LOGGER.debug("Bridge bootstrap fallback failure stack trace", exception);
      // Bridge bootstrap will be retried by the fallback path during rendering.
    }
  }

  GrapheneSubscription onInternalEvent(String channel, GrapheneBridgeEventListener listener) {
    return onEventValidated(validateInternalChannel(channel), listener);
  }

  GrapheneSubscription onInternalRequest(String channel, GrapheneBridgeRequestHandler handler) {
    return onRequestValidated(validateInternalChannel(channel), handler);
  }

  void emitInternal(String channel, String payloadJson) {
    emitValidated(validateInternalChannel(channel), payloadJson);
  }

  void authorizeClipboardWrite() {
    ensureOpen();
    clipboardAccess.authorize(documentGeneration.get());
  }

  void pasteClipboard(String payloadJson) {
    JsonElement payload = codec.parsePayloadJson(payloadJson);
    ensureOpen();
    browser.executeScript(
        "globalThis." + CLIPBOARD_PASTE_FUNCTION + "?.(" + codec.payloadToJson(payload) + ");",
        currentUrl());
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
    LOGGER.debug("Bridge endpoint ready browserId={} readyUrl={}", browserIdentifier(), readyUrl);
  }

  private boolean canActivateDocument(long readyDocumentGeneration) {
    return !closed.get()
        && exposureState == ExposureState.ALLOWED
        && documentGeneration.get() == readyDocumentGeneration;
  }

  private GrapheneSubscription onEventValidated(
      String validatedChannel, GrapheneBridgeEventListener listener) {
    Objects.requireNonNull(listener, LISTENER_NAME);
    ensureOpen();
    return handlers.onEvent(validatedChannel, listener);
  }

  private GrapheneSubscription onRequestValidated(
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
    outboundQueue.queueOrDispatch(outboundJson);
    if (LOGGER.isDebugEnabled()) {
      int payloadSize = payloadJson == null ? 0 : payloadJson.length();
      LOGGER.debug("Queued bridge event channel={} payloadSize={}", validatedChannel, payloadSize);
    }
  }

  private void injectBootstrapScript(String scriptUrl) {
    List<String> bootstrapScripts = GrapheneBridgeScriptLoader.scripts();
    for (String script : bootstrapScripts) {
      browser.executeScript(script, scriptUrl);
    }
  }

  private boolean allowsAuthorizedClipboardWrite(BridgeFrame frame, String requestJson) {
    GrapheneBridgePacket packet = codec.parsePacket(requestJson);
    return clipboardAccess.allows(frame, packet, documentGeneration.get());
  }

  private void tryInjectDocumentScripts(String documentUrl, String injectionPath) {
    DocumentIdentity currentDocument = documentIdentity(documentUrl);
    if (currentDocument.equals(documentScriptsDocument.get())) {
      return;
    }

    try {
      for (String script : GrapheneBridgeScriptLoader.documentScripts()) {
        browser.executeScript(script, documentUrl);
      }
      documentScriptsDocument.set(currentDocument);
      LOGGER.debug(
          "Injected document support scripts on {} browserId={} url={}",
          injectionPath,
          browserIdentifier(),
          documentUrl);
    } catch (RuntimeException exception) {
      LOGGER.debug(
          "Document support script injection failed on {} browserId={} url={} reason={}",
          injectionPath,
          browserIdentifier(),
          documentUrl,
          exception.getMessage());
      LOGGER.debug("Document support script injection failure stack trace", exception);
    }
  }

  private DocumentIdentity documentIdentity(String documentUrl) {
    return new DocumentIdentity(documentGeneration.get(), documentUrl);
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
    LOGGER.debug("Bridge exposure denied browserId={} url={}", browserIdentifier(), documentUrl);
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

  private record DocumentIdentity(long generation, String url) {}
}
