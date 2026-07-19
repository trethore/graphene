package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.api.browser.BrowserFrameListener;
import io.github.trethore.graphene.internal.event.GrapheneListenerList;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GrapheneFrameEventBus implements AutoCloseable {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneFrameEventBus.class);

  private final GrapheneTaskExecutor taskExecutor;
  private final GrapheneListenerList<BrowserFrameListener> listeners = new GrapheneListenerList<>();
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
    listeners.close();
  }

  public GrapheneSubscription subscribe(BrowserFrameListener listener) {
    return listeners.subscribe(listener);
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
        listeners.dispatch(listener -> listener.onFrame(frame), LOGGER, "browser frame listener");
      }
      notifications = pendingNotifications.addAndGet(-notifications);
    } while (notifications != 0);
  }
}
