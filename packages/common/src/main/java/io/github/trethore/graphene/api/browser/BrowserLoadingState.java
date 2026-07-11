package io.github.trethore.graphene.api.browser;

public record BrowserLoadingState(
    int browserId, boolean loading, boolean canGoBack, boolean canGoForward) {}
