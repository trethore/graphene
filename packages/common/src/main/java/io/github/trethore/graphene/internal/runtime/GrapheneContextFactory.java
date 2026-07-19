package io.github.trethore.graphene.internal.runtime;

import io.github.trethore.graphene.api.GrapheneContext;
import io.github.trethore.graphene.api.browser.BrowserSessions;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.url.GrapheneAssetUrls;
import java.util.function.UnaryOperator;

@FunctionalInterface
public interface GrapheneContextFactory {
  GrapheneContext create(Parameters parameters);

  record Parameters(
      String id,
      GrapheneConfig config,
      GrapheneAssetUrls appAssets,
      GrapheneAssetUrls classpathAssets,
      GrapheneAssetUrls httpAssets,
      UnaryOperator<String> httpUrlFactory,
      BrowserSessions browsers) {}
}
