package io.github.trethore.graphene.api;

import io.github.trethore.graphene.api.browser.BrowserSessions;
import io.github.trethore.graphene.api.config.GrapheneConfig;
import io.github.trethore.graphene.api.config.GrapheneContainerConfig;
import io.github.trethore.graphene.api.config.GrapheneGlobalConfig;
import io.github.trethore.graphene.api.runtime.GrapheneRuntime;
import io.github.trethore.graphene.api.url.GrapheneAssetUrls;
import java.util.Objects;
import java.util.function.UnaryOperator;

@SuppressWarnings("unused")
public final class GrapheneContext {
  private final String id;
  private final GrapheneConfig config;
  private final GrapheneAssetUrls appAssets;
  private final GrapheneAssetUrls classpathAssets;
  private final GrapheneAssetUrls httpAssets;
  private final UnaryOperator<String> httpUrlFactory;
  private final BrowserSessions browsers;

  public GrapheneContext(
      String id,
      GrapheneConfig config,
      GrapheneAssetUrls appAssets,
      GrapheneAssetUrls classpathAssets,
      GrapheneAssetUrls httpAssets,
      UnaryOperator<String> httpUrlFactory,
      BrowserSessions browsers) {
    this.id = Objects.requireNonNull(id, "id");
    this.config = Objects.requireNonNull(config, "config");
    this.appAssets = Objects.requireNonNull(appAssets, "appAssets");
    this.classpathAssets = Objects.requireNonNull(classpathAssets, "classpathAssets");
    this.httpAssets = Objects.requireNonNull(httpAssets, "httpAssets");
    this.httpUrlFactory = Objects.requireNonNull(httpUrlFactory, "httpUrlFactory");
    this.browsers = Objects.requireNonNull(browsers, "browsers");
  }

  public String id() {
    return id;
  }

  public GrapheneConfig config() {
    return config;
  }

  public GrapheneContainerConfig containerConfig() {
    return config.container();
  }

  public GrapheneGlobalConfig globalConfig() {
    return config.global();
  }

  public GrapheneGlobalConfig effectiveGlobalConfig() {
    return Graphene.globalConfig();
  }

  public GrapheneRuntime runtime() {
    return Graphene.runtime();
  }

  public GrapheneAssetUrls appAssets() {
    return appAssets;
  }

  public GrapheneAssetUrls classpathAssets() {
    return classpathAssets;
  }

  public GrapheneAssetUrls httpAssets() {
    return httpAssets;
  }

  public String httpUrl(String path) {
    return httpUrlFactory.apply(path);
  }

  public BrowserSessions browsers() {
    return browsers;
  }
}
