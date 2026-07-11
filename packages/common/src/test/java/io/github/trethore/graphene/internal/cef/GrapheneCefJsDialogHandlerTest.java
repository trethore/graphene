package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.internal.platform.GrapheneJsDialogPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.concurrent.CompletableFuture;
import org.cef.callback.CefJSDialogCallback;
import org.cef.handler.CefJSDialogHandler;
import org.cef.misc.BoolRef;
import org.junit.jupiter.api.Test;

class GrapheneCefJsDialogHandlerTest {
  @Test
  void mapsPromptAndCompletesCallback() {
    RecordingCallback callback = new RecordingCallback();
    GrapheneCefJsDialogHandler handler =
        new GrapheneCefJsDialogHandler(
            (type, origin, message, prompt) -> {
              assertEquals(GrapheneJsDialogPresenter.DialogType.PROMPT, type);
              assertEquals("default", prompt);
              return CompletableFuture.completedFuture(
                  new GrapheneJsDialogPresenter.Result(true, "answer"));
            },
            GrapheneTaskExecutor.direct());

    assertTrue(
        handler.onJSDialog(
            null,
            "https://example.invalid",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_PROMPT,
            "Question",
            "default",
            callback,
            new BoolRef(false)));
    assertTrue(callback.accepted);
    assertEquals("answer", callback.text);
  }

  @Test
  void suppressesMissingCallbacksAndRejectsPresenterFailures() {
    GrapheneCefJsDialogHandler handler =
        new GrapheneCefJsDialogHandler(
            (type, origin, message, prompt) ->
                CompletableFuture.failedFuture(new IllegalStateException("failed")),
            GrapheneTaskExecutor.direct());
    BoolRef suppress = new BoolRef(false);

    assertFalse(
        handler.onJSDialog(
            null,
            "",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT,
            "Alert",
            "",
            null,
            suppress));
    assertTrue(suppress.get());

    RecordingCallback callback = new RecordingCallback();
    assertTrue(
        handler.onJSDialog(
            null,
            "",
            CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_CONFIRM,
            "Confirm",
            "",
            callback,
            new BoolRef(false)));
    assertFalse(callback.accepted);
    assertEquals("", callback.text);
  }

  private static final class RecordingCallback implements CefJSDialogCallback {
    private boolean accepted;
    private String text;

    @Override
    public void Continue(boolean success, String userInput) {
      accepted = success;
      text = userInput;
    }
  }
}
