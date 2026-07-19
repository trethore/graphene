package io.github.trethore.graphene.internal.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.browser.BrowserLoadCompleted;
import io.github.trethore.graphene.api.browser.BrowserLoadFailed;
import io.github.trethore.graphene.api.browser.BrowserLoadFailureReason;
import io.github.trethore.graphene.api.browser.BrowserLoadListener;
import io.github.trethore.graphene.api.browser.BrowserLoadStarted;
import io.github.trethore.graphene.api.browser.BrowserLoadTransition;
import io.github.trethore.graphene.api.browser.BrowserLoadingState;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class GrapheneLoadEventBusTest {
  @Test
  void dispatchesGrapheneOwnedEventsInOrder() {
    try (GrapheneLoadEventBus eventBus = new GrapheneLoadEventBus()) {
      List<Object> receivedEvents = new ArrayList<>();
      eventBus.subscribe(new RecordingListener(receivedEvents));

      BrowserLoadingState loadingState = new BrowserLoadingState(true, false, false);
      BrowserLoadStarted started =
          new BrowserLoadStarted("app://assets/test", true, BrowserLoadTransition.LINK);
      BrowserLoadCompleted completed =
          new BrowserLoadCompleted("app://assets/test", true, OptionalInt.of(200));
      BrowserLoadFailed failed =
          new BrowserLoadFailed(
              "app://assets/test",
              true,
              BrowserLoadFailureReason.UNKNOWN,
              "failed",
              OptionalInt.of(-2));
      eventBus.publish(loadingState);
      eventBus.publish(started);
      eventBus.publish(completed);
      eventBus.publish(failed);

      assertEquals(List.of(loadingState, started, completed, failed), receivedEvents);
    }
  }

  @Test
  void subscriptionAndCloseStopListenerDelivery() {
    try (GrapheneLoadEventBus eventBus = new GrapheneLoadEventBus()) {
      List<Object> firstEvents = new ArrayList<>();
      List<Object> secondEvents = new ArrayList<>();
      List<Object> afterCloseEvents = new ArrayList<>();
      BrowserLoadListener first = new RecordingListener(firstEvents);
      BrowserLoadListener second = new RecordingListener(secondEvents);
      GrapheneSubscription firstSubscription = eventBus.subscribe(first);
      eventBus.subscribe(second);

      BrowserLoadingState beforeUnregister = new BrowserLoadingState(true, false, false);
      eventBus.publish(beforeUnregister);
      firstSubscription.close();
      BrowserLoadingState afterUnregister = new BrowserLoadingState(false, false, false);
      eventBus.publish(afterUnregister);
      eventBus.close();
      eventBus.subscribe(new RecordingListener(afterCloseEvents));
      eventBus.publish(new BrowserLoadingState(true, false, false));

      assertEquals(List.of(beforeUnregister), firstEvents);
      assertEquals(List.of(beforeUnregister, afterUnregister), secondEvents);
      assertEquals(List.of(), afterCloseEvents);
    }
  }

  @Test
  void listenerFailureDoesNotPreventRemainingListeners() {
    try (GrapheneLoadEventBus eventBus = new GrapheneLoadEventBus()) {
      List<Object> receivedEvents = new ArrayList<>();
      eventBus.subscribe(
          new BrowserLoadListener() {
            @Override
            public void onLoadingStateChanged(BrowserLoadingState state) {
              throw new IllegalStateException("listener failed");
            }
          });
      eventBus.subscribe(new RecordingListener(receivedEvents));
      BrowserLoadingState state = new BrowserLoadingState(true, false, false);

      eventBus.publish(state);

      assertEquals(List.of(state), receivedEvents);
    }
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
