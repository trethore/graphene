package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.api.browser.BrowserFrameListener;
import io.github.trethore.graphene.internal.event.GrapheneSubscriptions;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneFrameEventBus implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneFrameEventBus.class);

  private final GrapheneTaskExecutor taskExecutor;
  private final List<BrowserFrameListener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicReference<BrowserFrame> pendingFrame = new AtomicReference<>();
  private final AtomicInteger pendingNotifications = new AtomicInteger();
  private volatile boolean closed;

  public GrapheneFrameEventBus(GrapheneTaskExecutor taskExecutor) {
    this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
  }

  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
    closed = true;
    pendingFrame.set(null);
    listeners.clear();
  }

  public synchronized GrapheneSubscription subscribe(BrowserFrameListener listener) {
    BrowserFrameListener validatedListener = Objects.requireNonNull(listener, "listener");
    if (closed) {
      return GrapheneSubscriptions.empty();
    }
    listeners.add(validatedListener);
    return GrapheneSubscriptions.create(() -> listeners.remove(validatedListener));
  }

  public void publish(BrowserFrame frame) {
    BrowserFrame validatedFrame = Objects.requireNonNull(frame, "frame");
    if (closed || listeners.isEmpty()) {
      return;
    }
    pendingFrame.set(validatedFrame);
    if (closed) {
      pendingFrame.compareAndSet(validatedFrame, null);
      return;
    }
    scheduleNotificationDispatch();
  }

  private void scheduleNotificationDispatch() {
    if (closed || pendingNotifications.getAndIncrement() != 0) {
      return;
    }
    try {
      taskExecutor.execute(this::dispatchPendingFrames);
    } catch (RuntimeException exception) {
      pendingFrame.set(null);
      pendingNotifications.set(0);
      throw exception;
    }
  }

  private void dispatchPendingFrames() {
    int notifications = 1;
    do {
      BrowserFrame frame = pendingFrame.getAndSet(null);
      if (!closed && frame != null) {
        for (BrowserFrameListener listener : listeners) {
          try {
            listener.onFrame(frame);
          } catch (RuntimeException exception) {
            LOGGER.error("Unhandled Graphene browser frame listener exception", exception);
          }
        }
      }
      notifications = pendingNotifications.addAndGet(-notifications);
    } while (notifications != 0);
  }
}
