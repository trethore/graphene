package io.github.trethore.graphene.internal.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.browser.BrowserLoadCompleted;
import io.github.trethore.graphene.api.browser.BrowserLoadFailed;
import io.github.trethore.graphene.api.browser.BrowserLoadListener;
import io.github.trethore.graphene.api.browser.BrowserLoadStarted;
import io.github.trethore.graphene.api.browser.BrowserLoadingState;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrapheneLoadEventBusTest {
  @Test
  void dispatchesGrapheneOwnedEventsInOrder() {
    GrapheneLoadEventBus eventBus = new GrapheneLoadEventBus();
    List<Object> receivedEvents = new ArrayList<>();
    eventBus.register(new RecordingListener(receivedEvents));

    BrowserLoadingState loadingState = new BrowserLoadingState(4, true, false, false);
    BrowserLoadStarted started = new BrowserLoadStarted(4, "app://assets/test", true, "TT_LINK");
    BrowserLoadCompleted completed = new BrowserLoadCompleted(4, "app://assets/test", true, 200);
    BrowserLoadFailed failed =
        new BrowserLoadFailed(4, "app://assets/test", true, -2, "ERR_FAILED", "failed");
    eventBus.publish(loadingState);
    eventBus.publish(started);
    eventBus.publish(completed);
    eventBus.publish(failed);

    assertEquals(List.of(loadingState, started, completed, failed), receivedEvents);
  }

  @Test
  void unregisterAndClearStopListenerDelivery() {
    GrapheneLoadEventBus eventBus = new GrapheneLoadEventBus();
    List<Object> firstEvents = new ArrayList<>();
    List<Object> secondEvents = new ArrayList<>();
    BrowserLoadListener first = new RecordingListener(firstEvents);
    BrowserLoadListener second = new RecordingListener(secondEvents);
    eventBus.register(first);
    eventBus.register(second);

    BrowserLoadingState beforeUnregister = new BrowserLoadingState(1, true, false, false);
    eventBus.publish(beforeUnregister);
    eventBus.unregister(first);
    BrowserLoadingState afterUnregister = new BrowserLoadingState(1, false, false, false);
    eventBus.publish(afterUnregister);
    eventBus.clear();
    eventBus.publish(new BrowserLoadingState(1, true, false, false));

    assertEquals(List.of(beforeUnregister), firstEvents);
    assertEquals(List.of(beforeUnregister, afterUnregister), secondEvents);
  }

  @Test
  void listenerFailureDoesNotPreventRemainingListeners() {
    GrapheneLoadEventBus eventBus = new GrapheneLoadEventBus();
    List<Object> receivedEvents = new ArrayList<>();
    eventBus.register(
        new BrowserLoadListener() {
          @Override
          public void onLoadingStateChanged(BrowserLoadingState state) {
            throw new IllegalStateException("listener failed");
          }
        });
    eventBus.register(new RecordingListener(receivedEvents));
    BrowserLoadingState state = new BrowserLoadingState(2, true, false, false);

    eventBus.publish(state);

    assertEquals(List.of(state), receivedEvents);
  }

  private record RecordingListener(List<Object> events) implements BrowserLoadListener {
    @Override
    public void onLoadingStateChanged(BrowserLoadingState state) {
      events.add(state);
    }

    @Override
    public void onLoadStarted(BrowserLoadStarted event) {
      events.add(event);
    }

    @Override
    public void onLoadCompleted(BrowserLoadCompleted event) {
      events.add(event);
    }

    @Override
    public void onLoadFailed(BrowserLoadFailed event) {
      events.add(event);
    }
  }
}
