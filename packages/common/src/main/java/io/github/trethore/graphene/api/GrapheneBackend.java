package io.github.trethore.graphene.api;

import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.runtime.GrapheneRuntime;

public interface GrapheneBackend {
  GrapheneContext register(Class<?> anchorClass, GrapheneConfig config);

  GrapheneContext register(String modId, GrapheneConfig config);

  GrapheneContext context(Class<?> anchorClass);

  GrapheneContext context(String modId);

  GrapheneGlobalConfig globalConfig();

  GrapheneRuntime runtime();
}
