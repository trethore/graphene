package io.github.trethore.graphene.internal.platform;

@FunctionalInterface
public interface GrapheneExternalBrowser {
  void open(String url);
}
