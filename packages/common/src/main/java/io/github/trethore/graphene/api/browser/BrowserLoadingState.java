package io.github.trethore.graphene.api.browser;

/** Current session-wide loading and history state. */
public record BrowserLoadingState(boolean loading, boolean canGoBack, boolean canGoForward) {}
