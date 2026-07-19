package io.github.trethore.graphene.internal.platform;

import io.github.trethore.graphene.api.browser.dialog.BrowserFileDialogPresenter;
import io.github.trethore.graphene.api.browser.dialog.BrowserJsDialogPresenter;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import java.util.Objects;

public record GraphenePlatformServices(
    GrapheneLifecycle lifecycle,
    GrapheneTaskExecutor mainThreadExecutor,
    GrapheneModResolver modResolver,
    GrapheneNativeWindow nativeWindow,
    GrapheneWindowMetrics windowMetrics,
    GrapheneExternalBrowser externalBrowser,
    GrapheneStartupPresenter startupPresenter,
    BrowserContextMenuPresenter contextMenuPresenter,
    BrowserFileDialogPresenter fileDialogPresenter,
    BrowserJsDialogPresenter jsDialogPresenter) {
  public GraphenePlatformServices {
    Objects.requireNonNull(lifecycle, "lifecycle");
    Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
    Objects.requireNonNull(modResolver, "modResolver");
    Objects.requireNonNull(nativeWindow, "nativeWindow");
    Objects.requireNonNull(windowMetrics, "windowMetrics");
    Objects.requireNonNull(externalBrowser, "externalBrowser");
    Objects.requireNonNull(startupPresenter, "startupPresenter");
    Objects.requireNonNull(contextMenuPresenter, "contextMenuPresenter");
    Objects.requireNonNull(fileDialogPresenter, "fileDialogPresenter");
    Objects.requireNonNull(jsDialogPresenter, "jsDialogPresenter");
  }
}
