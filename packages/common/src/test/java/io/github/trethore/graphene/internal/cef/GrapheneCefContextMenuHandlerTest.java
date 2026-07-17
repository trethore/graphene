package io.github.trethore.graphene.internal.cef;

import static io.github.trethore.graphene.internal.cef.CefTestProxies.defaultValue;
import static io.github.trethore.graphene.internal.cef.CefTestProxies.objectMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserOptions;
import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuAction;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuContext;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuItem;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPolicy;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.cef.browser.CefBrowser;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.callback.CefRunContextMenuCallback;
import org.junit.jupiter.api.Test;

class GrapheneCefContextMenuHandlerTest {
  @Test
  void snapshotsMetadataAndRunsSelectedCommand() {
    AtomicReference<BrowserContextMenuPresenter.Request> capturedRequest = new AtomicReference<>();
    BrowserContextMenuPresenter presenter =
        request -> {
          capturedRequest.set(request);
          BrowserContextMenuItem.Command copy =
              request.items().stream()
                  .filter(BrowserContextMenuItem.Command.class::isInstance)
                  .map(BrowserContextMenuItem.Command.class::cast)
                  .filter(command -> command.action() == BrowserContextMenuAction.COPY)
                  .findFirst()
                  .orElseThrow();
          return CompletableFuture.completedFuture(BrowserContextMenuPresenter.Result.select(copy));
        };
    BrowserOptions options =
        BrowserOptions.builder()
            .contextMenuPolicy(BrowserContextMenuPolicy.standard())
            .contextMenuPresenter(presenter)
            .build();
    RecordingCallback callback = new RecordingCallback();
    GrapheneCefContextMenuHandler handler =
        new GrapheneCefContextMenuHandler(
            request ->
                CompletableFuture.completedFuture(BrowserContextMenuPresenter.Result.cancel()),
            GrapheneTaskExecutor.direct());

    assertTrue(handler.runContextMenu(browser(options), null, params(), menuModel(), callback));

    assertEquals(CefMenuModel.MenuId.MENU_ID_COPY, callback.commandId);
    assertFalse(callback.cancelled);
    BrowserContextMenuPresenter.Request request = capturedRequest.get();
    assertEquals(2, request.items().size());
    BrowserContextMenuItem.Command firstCommand =
        (BrowserContextMenuItem.Command) request.items().getFirst();
    assertEquals("Copy & details", firstCommand.label());
    BrowserContextMenuContext context = request.context();
    assertEquals(12, context.position().x());
    assertEquals(34, context.position().y());
    assertEquals("https://example.invalid", context.document().pageUrl());
    assertEquals("selected", context.selectionText());
    assertTrue(context.targetTypes().contains(BrowserContextMenuContext.TargetType.SELECTION));
    assertEquals(List.of("example"), context.editing().dictionarySuggestions());
  }

  @Test
  void disabledPolicyAndPresenterFailuresCancelWithoutNativeFallback() {
    RecordingCallback disabledCallback = new RecordingCallback();
    GrapheneCefContextMenuHandler handler =
        new GrapheneCefContextMenuHandler(
            request -> CompletableFuture.failedFuture(new IllegalStateException("failed")),
            GrapheneTaskExecutor.direct());
    BrowserOptions disabledOptions = BrowserOptions.defaults();

    assertTrue(
        handler.runContextMenu(
            browser(disabledOptions), null, params(), menuModel(), disabledCallback));
    assertTrue(disabledCallback.cancelled);

    RecordingCallback failedCallback = new RecordingCallback();
    BrowserOptions enabledOptions =
        BrowserOptions.builder().contextMenuPolicy(BrowserContextMenuPolicy.standard()).build();
    assertTrue(
        handler.runContextMenu(
            browser(enabledOptions), null, params(), menuModel(), failedCallback));
    assertTrue(failedCallback.cancelled);
  }

  @Test
  void clearsMenusForBrowsersNotOwnedByGraphene() {
    boolean[] cleared = {false};
    CefMenuModel model =
        (CefMenuModel)
            Proxy.newProxyInstance(
                CefMenuModel.class.getClassLoader(),
                new Class<?>[] {CefMenuModel.class},
                (proxy, method, arguments) -> {
                  if (method.getName().equals("clear")) {
                    cleared[0] = true;
                    return true;
                  }
                  return defaultValue(method.getReturnType());
                });
    GrapheneCefContextMenuHandler handler =
        new GrapheneCefContextMenuHandler(
            request ->
                CompletableFuture.completedFuture(BrowserContextMenuPresenter.Result.cancel()),
            GrapheneTaskExecutor.direct());

    handler.onBeforeContextMenu(null, null, null, model);

    assertTrue(cleared[0]);
  }

  @Test
  void rejectsCommandsNotProposedByGraphene() {
    boolean[] presenterCalled = {false};
    BrowserOptions options =
        BrowserOptions.builder()
            .contextMenuPolicy(
                request ->
                    List.of(
                        new BrowserContextMenuItem.Command(
                            new BrowserContextMenuItem.CommandId(999),
                            BrowserContextMenuAction.COPY,
                            "Forged",
                            true,
                            false)))
            .contextMenuPresenter(
                request -> {
                  presenterCalled[0] = true;
                  return CompletableFuture.completedFuture(
                      BrowserContextMenuPresenter.Result.cancel());
                })
            .build();
    RecordingCallback callback = new RecordingCallback();
    GrapheneCefContextMenuHandler handler =
        new GrapheneCefContextMenuHandler(
            request ->
                CompletableFuture.completedFuture(BrowserContextMenuPresenter.Result.cancel()),
            GrapheneTaskExecutor.direct());

    assertTrue(handler.runContextMenu(browser(options), null, params(), menuModel(), callback));

    assertTrue(callback.cancelled);
    assertFalse(presenterCalled[0]);
  }

  private static CefBrowser browser(BrowserOptions options) {
    return (CefBrowser)
        Proxy.newProxyInstance(
            CefBrowser.class.getClassLoader(),
            new Class<?>[] {CefBrowser.class, BrowserSession.class},
            (proxy, method, arguments) ->
                switch (method.getName()) {
                  case "options" -> options;
                  case "isClosed" -> false;
                  case "equals", "hashCode", "toString" ->
                      objectMethod(proxy, method.getName(), arguments);
                  default -> defaultValue(method.getReturnType());
                });
  }

  private static CefContextMenuParams params() {
    return (CefContextMenuParams)
        Proxy.newProxyInstance(
            CefContextMenuParams.class.getClassLoader(),
            new Class<?>[] {CefContextMenuParams.class},
            (proxy, method, arguments) ->
                switch (method.getName()) {
                  case "getXCoord" -> 12;
                  case "getYCoord" -> 34;
                  case "getTypeFlags" -> CefContextMenuParams.TypeFlags.CM_TYPEFLAG_SELECTION;
                  case "getPageUrl" -> "https://example.invalid";
                  case "getFrameUrl" -> "https://example.invalid/frame";
                  case "getSelectionText" -> "selected";
                  case "getDictionarySuggestions" -> {
                    @SuppressWarnings("unchecked")
                    Vector<String> suggestions = (Vector<String>) arguments[0];
                    suggestions.add("example");
                    yield true;
                  }
                  case "getMediaType" -> CefContextMenuParams.MediaType.CM_MEDIATYPE_NONE;
                  case "equals", "hashCode", "toString" ->
                      objectMethod(proxy, method.getName(), arguments);
                  default -> defaultValue(method.getReturnType());
                });
  }

  private static CefMenuModel menuModel() {
    int[] commandIds = {
      CefMenuModel.MenuId.MENU_ID_COPY,
      CefMenuModel.MenuId.MENU_ID_VIEW_SOURCE,
      CefMenuModel.MenuId.MENU_ID_RELOAD
    };
    String[] labels = {"&Copy && details", "&View source", "&Reload"};
    return (CefMenuModel)
        Proxy.newProxyInstance(
            CefMenuModel.class.getClassLoader(),
            new Class<?>[] {CefMenuModel.class},
            (proxy, method, arguments) ->
                switch (method.getName()) {
                  case "getCount" -> commandIds.length;
                  case "getTypeAt" -> CefMenuModel.MenuItemType.MENUITEMTYPE_COMMAND;
                  case "getCommandIdAt" -> commandIds[(int) arguments[0]];
                  case "getLabelAt" -> labels[(int) arguments[0]];
                  case "isVisibleAt", "isEnabledAt" -> true;
                  case "isCheckedAt" -> false;
                  case "equals", "hashCode", "toString" ->
                      objectMethod(proxy, method.getName(), arguments);
                  default -> defaultValue(method.getReturnType());
                });
  }

  private static final class RecordingCallback implements CefRunContextMenuCallback {
    private int commandId = -1;
    private boolean cancelled;

    @Override
    public void Continue(int selectedCommandId, int eventFlags) {
      commandId = selectedCommandId;
    }

    @Override
    public void Cancel() {
      cancelled = true;
    }
  }
}
