package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.platform.GrapheneJsDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;
import org.cef.handler.CefJSDialogHandlerAdapter;
import org.cef.misc.BoolRef;

final class GrapheneCefJsDialogHandler extends CefJSDialogHandlerAdapter {
  private final GrapheneJsDialogPresenter presenter;
  private final GrapheneTaskExecutor mainThreadExecutor;

  GrapheneCefJsDialogHandler(
      GrapheneJsDialogPresenter presenter, GrapheneTaskExecutor mainThreadExecutor) {
    this.presenter = Objects.requireNonNull(presenter, "presenter");
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
    presenter
        .show(dialogType(dialogType), originUrl, messageText, defaultPromptText)
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
    presenter
        .show(GrapheneJsDialogPresenter.DialogType.BEFORE_UNLOAD, browser.getURL(), messageText, "")
        .whenComplete(
            (result, failure) ->
                mainThreadExecutor.execute(
                    () ->
                        callback.Continue(
                            failure == null && result != null && result.accepted(), "")));
    return true;
  }

  private static GrapheneJsDialogPresenter.DialogType dialogType(
      CefJSDialogHandler.JSDialogType dialogType) {
    if (dialogType == null) {
      return GrapheneJsDialogPresenter.DialogType.ALERT;
    }
    return switch (dialogType) {
      case JSDIALOGTYPE_ALERT -> GrapheneJsDialogPresenter.DialogType.ALERT;
      case JSDIALOGTYPE_CONFIRM -> GrapheneJsDialogPresenter.DialogType.CONFIRM;
      case JSDIALOGTYPE_PROMPT -> GrapheneJsDialogPresenter.DialogType.PROMPT;
    };
  }
}
