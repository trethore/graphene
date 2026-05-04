package tytoo.grapheneui.internal.bridge;

import com.google.gson.Gson;
import tytoo.grapheneui.api.bridge.GrapheneBridge;

import java.time.Duration;
import java.util.Objects;

public final class GrapheneBridgeOptions {
    private static final int DEFAULT_MAX_QUEUED_OUTBOUND_MESSAGES = 1024;
    private static final GrapheneBridgeQueueOverflowPolicy DEFAULT_QUEUE_OVERFLOW_POLICY = GrapheneBridgeQueueOverflowPolicy.DROP_OLDEST;

    private final Gson gson;
    private final Duration defaultRequestTimeout;
    private final int maxQueuedOutboundMessages;
    private final GrapheneBridgeQueueOverflowPolicy queueOverflowPolicy;
    private final GrapheneBridgeDiagnostics diagnostics;

    private GrapheneBridgeOptions(Builder builder) {
        this.gson = Objects.requireNonNull(builder.gson, "gson");

        Duration validatedTimeout = Objects.requireNonNull(builder.defaultRequestTimeout, "defaultRequestTimeout");
        if (validatedTimeout.isZero() || validatedTimeout.isNegative()) {
            throw new IllegalArgumentException("defaultRequestTimeout must be > 0");
        }
        this.defaultRequestTimeout = validatedTimeout;

        if (builder.maxQueuedOutboundMessages < 1) {
            throw new IllegalArgumentException("maxQueuedOutboundMessages must be >= 1");
        }
        this.maxQueuedOutboundMessages = builder.maxQueuedOutboundMessages;

        this.queueOverflowPolicy = Objects.requireNonNull(builder.queueOverflowPolicy, "queueOverflowPolicy");
        this.diagnostics = Objects.requireNonNull(builder.diagnostics, "diagnostics");
    }

    public static GrapheneBridgeOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Gson gson() {
        return gson;
    }

    public Duration defaultRequestTimeout() {
        return defaultRequestTimeout;
    }

    public int maxQueuedOutboundMessages() {
        return maxQueuedOutboundMessages;
    }

    public GrapheneBridgeQueueOverflowPolicy queueOverflowPolicy() {
        return queueOverflowPolicy;
    }

    public GrapheneBridgeDiagnostics diagnostics() {
        return diagnostics;
    }

    public static final class Builder {
        private Gson gson = new Gson();
        private Duration defaultRequestTimeout = GrapheneBridge.DEFAULT_REQUEST_TIMEOUT;
        private int maxQueuedOutboundMessages = DEFAULT_MAX_QUEUED_OUTBOUND_MESSAGES;
        private GrapheneBridgeQueueOverflowPolicy queueOverflowPolicy = DEFAULT_QUEUE_OVERFLOW_POLICY;
        private GrapheneBridgeDiagnostics diagnostics = GrapheneBridgeDiagnostics.noOp();

        private Builder() {
        }

        public Builder gson(Gson gson) {
            this.gson = Objects.requireNonNull(gson, "gson");
            return this;
        }

        public Builder defaultRequestTimeout(Duration defaultRequestTimeout) {
            this.defaultRequestTimeout = Objects.requireNonNull(defaultRequestTimeout, "defaultRequestTimeout");
            return this;
        }

        public Builder maxQueuedOutboundMessages(int maxQueuedOutboundMessages) {
            this.maxQueuedOutboundMessages = maxQueuedOutboundMessages;
            return this;
        }

        public Builder queueOverflowPolicy(GrapheneBridgeQueueOverflowPolicy queueOverflowPolicy) {
            this.queueOverflowPolicy = Objects.requireNonNull(queueOverflowPolicy, "queueOverflowPolicy");
            return this;
        }

        public Builder diagnostics(GrapheneBridgeDiagnostics diagnostics) {
            this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
            return this;
        }

        public GrapheneBridgeOptions build() {
            return new GrapheneBridgeOptions(this);
        }
    }
}
