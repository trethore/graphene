package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class GrapheneFrameEventBusTest {
  @Test
  void coalescesPendingFramesToTheLatestSnapshot() {
    TestTaskExecutor executor = new TestTaskExecutor();
    try (GrapheneFrameEventBus eventBus = new GrapheneFrameEventBus(executor)) {
      List<BrowserFrame> receivedFrames = new ArrayList<>();
      eventBus.subscribe(receivedFrames::add);

      eventBus.publish(frame(1));
      eventBus.publish(frame(2));

      assertEquals(1, executor.pendingTasks());
      executor.runNext();
      assertEquals(List.of(2L), receivedFrames.stream().map(BrowserFrame::sequence).toList());
    }
  }

  @Test
  void drainsAFramePublishedDuringDispatchWithoutAnotherTask() {
    TestTaskExecutor executor = new TestTaskExecutor();
    try (GrapheneFrameEventBus eventBus = new GrapheneFrameEventBus(executor)) {
      List<Long> receivedSequences = new ArrayList<>();
      eventBus.subscribe(
          frame -> {
            receivedSequences.add(frame.sequence());
            if (frame.sequence() == 1) {
              eventBus.publish(frame(2));
            }
          });

      eventBus.publish(frame(1));
      executor.runNext();

      assertEquals(0, executor.pendingTasks());
      assertEquals(List.of(1L, 2L), receivedSequences);
    }
  }

  @Test
  void discardsQueuedNotificationsAfterClosure() {
    TestTaskExecutor executor = new TestTaskExecutor();
    GrapheneFrameEventBus eventBus = new GrapheneFrameEventBus(executor);
    List<BrowserFrame> receivedFrames = new ArrayList<>();
    eventBus.subscribe(receivedFrames::add);
    eventBus.publish(frame(1));

    eventBus.close();
    executor.runNext();

    assertTrue(receivedFrames.isEmpty());
  }

  @Test
  void isolatesListenerFailures() {
    TestTaskExecutor executor = new TestTaskExecutor();
    try (GrapheneFrameEventBus eventBus = new GrapheneFrameEventBus(executor)) {
      List<BrowserFrame> receivedFrames = new ArrayList<>();
      eventBus.subscribe(
          frame -> {
            throw new IllegalStateException("listener failure");
          });
      eventBus.subscribe(receivedFrames::add);

      eventBus.publish(frame(1));
      executor.runNext();

      assertEquals(1, receivedFrames.size());
    }
  }

  private static BrowserFrame frame(long sequence) {
    return new BrowserFrame(
        1, 1, sequence, List.of(new BrowserDirtyRegion(0, 0, 1, 1)), ByteBuffer.allocateDirect(4));
  }

  private static final class TestTaskExecutor implements GrapheneTaskExecutor {
    private final Queue<Runnable> tasks = new ArrayDeque<>();

    @Override
    public void execute(Runnable action) {
      tasks.add(action);
    }

    @Override
    public <T> CompletableFuture<T> supply(Supplier<T> action) {
      return CompletableFuture.completedFuture(action.get());
    }

    void runNext() {
      tasks.remove().run();
    }

    int pendingTasks() {
      return tasks.size();
    }
  }
}
