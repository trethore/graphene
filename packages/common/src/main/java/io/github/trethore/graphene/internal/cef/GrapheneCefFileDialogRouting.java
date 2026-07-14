package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeSubscription;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

final class GrapheneCefFileDialogRouting implements AutoCloseable {
  private static final String ARM_CHANNEL = "graphene.internal.file-dialog.arm-directory";
  private static final long INTENT_LIFETIME_NANOS = 2_000_000_000L;

  private final AtomicLong directoryIntentDeadlineNanos = new AtomicLong();
  private GrapheneBridgeSubscription requestSubscription;

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
    requestSubscription =
        Objects.requireNonNull(bridge, "bridge")
            .onRequest(
                ARM_CHANNEL,
                (channel, payloadJson) -> {
                  directoryIntentDeadlineNanos.set(System.nanoTime() + INTENT_LIFETIME_NANOS);
                  return null;
                });
  }

  boolean consumeDirectoryIntent() {
    long deadlineNanos = directoryIntentDeadlineNanos.getAndSet(0L);
    return deadlineNanos != 0L && deadlineNanos - System.nanoTime() >= 0L;
  }
}
