package io.github.trethore.graphene.internal.platform;

import java.util.Objects;

public record GraphenePlatformServices(
    GrapheneLifecycle lifecycle,
    GrapheneTaskExecutor mainThreadExecutor,
    GrapheneModResolver modResolver,
    GrapheneNativeWindow nativeWindow,
    GrapheneWindowMetrics windowMetrics,
    GrapheneStartupPresenter startupPresenter,
    GrapheneFileDialogPresenter fileDialogPresenter,
    GrapheneJsDialogPresenter jsDialogPresenter) {
  public GraphenePlatformServices {
    Objects.requireNonNull(lifecycle, "lifecycle");
    Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    Objects.requireNonNull(modResolver, "modResolver");
    Objects.requireNonNull(nativeWindow, "nativeWindow");
    Objects.requireNonNull(windowMetrics, "windowMetrics");
    Objects.requireNonNull(startupPresenter, "startupPresenter");
    Objects.requireNonNull(fileDialogPresenter, "fileDialogPresenter");
    Objects.requireNonNull(jsDialogPresenter, "jsDialogPresenter");
  }
}
