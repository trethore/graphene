package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.dialog.BrowserJsDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.misc.BoolRef;

final class GrapheneCefJsDialogHandler extends CefJSDialogHandlerAdapter {
  private final BrowserJsDialogPresenter defaultPresenter;
  private final GrapheneTaskExecutor mainThreadExecutor;

  GrapheneCefJsDialogHandler(
      BrowserJsDialogPresenter defaultPresenter, GrapheneTaskExecutor mainThreadExecutor) {
    this.defaultPresenter = Objects.requireNonNull(defaultPresenter, "defaultPresenter");
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
  }

  @Override
  public boolean onJSDialog(
      CefBrowser browser,
      String originUrl,
      CefJSDialogHandler.JSDialogType dialogType,
      String messageText,
      String defaultPromptText,
      CefJSDialogCallback callback,
      BoolRef suppressMessage) {
    if (callback == null) {
      if (suppressMessage != null) {
        suppressMessage.set(true);
      }
      return false;
    }
    BrowserJsDialogPresenter presenter = presenter(browser);
    BrowserJsDialogPresenter.Request request =
        new BrowserJsDialogPresenter.Request(
            dialogType(dialogType),
            Objects.requireNonNullElse(originUrl, ""),
            Objects.requireNonNullElse(messageText, ""),
            Objects.requireNonNullElse(defaultPromptText, ""),
            false);
    mainThreadExecutor
        .supplyStage(() -> presenter.show(request))
        .whenComplete(
            (result, failure) ->
                mainThreadExecutor.execute(
                    () -> {
                      if (failure != null || result == null) {
                        callback.Continue(false, "");
                      } else {
                        callback.Continue(result.accepted(), result.promptText());
                      }
                    }));
    return true;
  }

  @Override
  public boolean onBeforeUnloadDialog(
      CefBrowser browser, String messageText, boolean isReload, CefJSDialogCallback callback) {
    if (browser == null) {
      return false;
    }
    if (callback == null) {
      return true;
    }
    BrowserJsDialogPresenter presenter = presenter(browser);
    BrowserJsDialogPresenter.Request request =
        new BrowserJsDialogPresenter.Request(
            BrowserJsDialogPresenter.Type.BEFORE_UNLOAD,
            Objects.requireNonNullElse(browser.getURL(), ""),
            Objects.requireNonNullElse(messageText, ""),
            "",
            isReload);
    mainThreadExecutor
        .supplyStage(() -> presenter.show(request))
        .whenComplete(
            (result, failure) ->
                mainThreadExecutor.execute(
                    () ->
                        callback.Continue(
                            failure == null && result != null && result.accepted(), "")));
    return true;
  }

  private BrowserJsDialogPresenter presenter(CefBrowser browser) {
    if (browser instanceof BrowserSession session) {
      return session.options().jsDialogPresenter().orElse(defaultPresenter);
    }
    return defaultPresenter;
  }

  private static BrowserJsDialogPresenter.Type dialogType(
      CefJSDialogHandler.JSDialogType dialogType) {
    if (dialogType == null) {
      return BrowserJsDialogPresenter.Type.ALERT;
    }
    return switch (dialogType) {
      case JSDIALOGTYPE_ALERT -> BrowserJsDialogPresenter.Type.ALERT;
      case JSDIALOGTYPE_CONFIRM -> BrowserJsDialogPresenter.Type.CONFIRM;
      case JSDIALOGTYPE_PROMPT -> BrowserJsDialogPresenter.Type.PROMPT;
    };
  }
}
