package io.github.trethore.graphene.internal.platform;

public interface GrapheneLifecycle {
  void onStarted(Runnable action);

  void onStopping(Runnable action);
}
