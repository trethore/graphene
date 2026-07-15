package io.github.trethore.graphene.internal.event;

import io.github.trethore.graphene.api.GrapheneSubscription;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GrapheneSubscriptions {
  private static final GrapheneSubscription EMPTY = () -> {};

  private GrapheneSubscriptions() {}

  public static GrapheneSubscription create(Runnable unsubscribeAction) {
    Runnable validatedAction = Objects.requireNonNull(unsubscribeAction, "unsubscribeAction");
    AtomicBoolean subscribed = new AtomicBoolean(true);
    return () -> {
      if (subscribed.compareAndSet(true, false)) {
        validatedAction.run();
      }
    };
  }

  public static GrapheneSubscription empty() {
    return EMPTY;
  }
}
