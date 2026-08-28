package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.internal.bridge.GrapheneBridgeInternals;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

final class GrapheneCefFileDialogRouting implements AutoCloseable {
    private static final String ARM_CHANNEL = "graphene:file-dialog:arm-directory";
    private static final long INTENT_LIFETIME_NANOS = 2_000_000_000L;

    private final AtomicLong directoryIntentDeadlineNanos = new AtomicLong();
    private final LongSupplier nanoTime;
    private GrapheneSubscription requestSubscription;

    GrapheneCefFileDialogRouting() {
        this(System::nanoTime);
    }

    GrapheneCefFileDialogRouting(LongSupplier nanoTime) {
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    @Override
    public void close() {
        if (requestSubscription != null) {
            requestSubscription.close();
            requestSubscription = null;
        }
        directoryIntentDeadlineNanos.set(0L);
    }

    void attach(GrapheneBridge bridge) {
        if (requestSubscription != null) {
            throw new IllegalStateException("File dialog routing is already attached");
        }
        requestSubscription = GrapheneBridgeInternals.onDocumentRequest(
                Objects.requireNonNull(bridge, "bridge"), ARM_CHANNEL, (channel, payloadJson) -> {
                    armDirectoryIntent();
                    return null;
                });
    }

    boolean consumeDirectoryIntent() {
        long deadlineNanos = directoryIntentDeadlineNanos.getAndSet(0L);
        return deadlineNanos != 0L && deadlineNanos - nanoTime.getAsLong() >= 0L;
    }

    void armDirectoryIntent() {
        directoryIntentDeadlineNanos.set(nanoTime.getAsLong() + INTENT_LIFETIME_NANOS);
    }
}
