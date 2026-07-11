package io.github.trethore.graphene.internal.platform;

public interface GrapheneStartupPresenter {
  void update(String stage, double progress);

  void close();
}
