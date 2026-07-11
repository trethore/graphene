package io.github.trethore.graphene.internal.platform;

public interface GrapheneModResolver {
  String resolveModId(Class<?> anchorClass);

  boolean isModLoaded(String modId);
}
