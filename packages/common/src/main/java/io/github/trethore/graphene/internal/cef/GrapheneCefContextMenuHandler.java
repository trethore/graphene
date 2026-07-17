package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserSession;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuContext;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuItem;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPolicy;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.callback.CefRunContextMenuCallback;
import org.cef.handler.CefContextMenuHandlerAdapter;

final class GrapheneCefContextMenuHandler extends CefContextMenuHandlerAdapter {
  private final BrowserContextMenuPresenter defaultPresenter;
  private final GrapheneTaskExecutor mainThreadExecutor;

  GrapheneCefContextMenuHandler(
      BrowserContextMenuPresenter defaultPresenter, GrapheneTaskExecutor mainThreadExecutor) {
    this.defaultPresenter = Objects.requireNonNull(defaultPresenter, "defaultPresenter");
    this.mainThreadExecutor = Objects.requireNonNull(mainThreadExecutor, "mainThreadExecutor");
  }

  @Override
  public void onBeforeContextMenu(
      CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
    if (!(browser instanceof BrowserSession) || model == null) {
      clear(model);
    }
  }

  @Override
  public boolean runContextMenu(
      CefBrowser browser,
      CefFrame frame,
      CefContextMenuParams params,
      CefMenuModel model,
      CefRunContextMenuCallback callback) {
    if (!(browser instanceof BrowserSession session)) {
      return false;
    }
    if (callback == null) {
      return true;
    }
    if (params == null || model == null) {
      callback.Cancel();
      return true;
    }
    try {
      present(session, params, model, callback);
    } catch (RuntimeException exception) {
      callback.Cancel();
    }
    return true;
  }

  private void present(
      BrowserSession session,
      CefContextMenuParams params,
      CefMenuModel model,
      CefRunContextMenuCallback callback) {
    BrowserContextMenuContext context = GrapheneCefContextMenuMapper.context(session, params);
    Map<BrowserContextMenuItem.CommandId, Integer> commands = new HashMap<>();
    List<BrowserContextMenuItem> proposedItems = snapshotItems(model, commands);
    List<BrowserContextMenuItem> configuredItems =
        configuredItems(session.options().contextMenuPolicy(), context, proposedItems, commands);
    if (configuredItems.isEmpty()) {
      callback.Cancel();
      return;
    }
    BrowserContextMenuPresenter presenter =
        session.options().contextMenuPresenter().orElse(defaultPresenter);
    BrowserContextMenuPresenter.Request request =
        new BrowserContextMenuPresenter.Request(context, configuredItems);
    mainThreadExecutor
        .supplyStage(() -> presenter.show(request))
        .whenComplete(
            (result, failure) ->
                mainThreadExecutor.execute(
                    () -> complete(callback, result, failure, commands, session.isClosed())));
  }

  private static void complete(
      CefRunContextMenuCallback callback,
      BrowserContextMenuPresenter.Result result,
      Throwable failure,
      Map<BrowserContextMenuItem.CommandId, Integer> commands,
      boolean sessionClosed) {
    if (failure != null || result == null || sessionClosed || result.selectedCommand().isEmpty()) {
      callback.Cancel();
      return;
    }
    Integer commandId = commands.get(result.selectedCommand().orElseThrow());
    if (commandId == null) {
      callback.Cancel();
      return;
    }
    callback.Continue(commandId, GrapheneCefInputTranslator.modifiers(result.modifiers()));
  }

  private static List<BrowserContextMenuItem> configuredItems(
      BrowserContextMenuPolicy policy,
      BrowserContextMenuContext context,
      List<BrowserContextMenuItem> proposedItems,
      Map<BrowserContextMenuItem.CommandId, Integer> commands) {
    try {
      List<BrowserContextMenuItem> configured =
          policy.configure(new BrowserContextMenuPolicy.Request(context, proposedItems));
      return normalize(configured, commands.keySet());
    } catch (RuntimeException exception) {
      return List.of();
    }
  }

  private static List<BrowserContextMenuItem> normalize(
      List<BrowserContextMenuItem> items, Set<BrowserContextMenuItem.CommandId> availableCommands) {
    if (items == null || items.isEmpty()) {
      return List.of();
    }
    ArrayList<BrowserContextMenuItem> normalized = new ArrayList<>();
    boolean pendingSeparator = false;
    for (BrowserContextMenuItem item : items) {
      if (item instanceof BrowserContextMenuItem.Separator) {
        pendingSeparator = !normalized.isEmpty();
      } else if (item instanceof BrowserContextMenuItem.Command command
          && availableCommands.contains(command.id())) {
        if (pendingSeparator) {
          normalized.add(new BrowserContextMenuItem.Separator());
          pendingSeparator = false;
        }
        normalized.add(command);
      }
    }
    return List.copyOf(normalized);
  }

  private static List<BrowserContextMenuItem> snapshotItems(
      CefMenuModel model, Map<BrowserContextMenuItem.CommandId, Integer> commands) {
    ArrayList<BrowserContextMenuItem> items = new ArrayList<>();
    int count = Math.max(model.getCount(), 0);
    for (int index = 0; index < count; index++) {
      BrowserContextMenuItem item =
          switch (model.getTypeAt(index)) {
            case MENUITEMTYPE_SEPARATOR -> new BrowserContextMenuItem.Separator();
            case MENUITEMTYPE_SUBMENU -> {
              appendSubmenu(model.getSubMenuAt(index), items, commands);
              yield null;
            }
            default -> snapshotCommand(model, index, commands);
          };
      if (item != null) {
        items.add(item);
      }
    }
    return List.copyOf(items);
  }

  private static void appendSubmenu(
      CefMenuModel submenu,
      List<BrowserContextMenuItem> items,
      Map<BrowserContextMenuItem.CommandId, Integer> commands) {
    if (submenu == null) {
      return;
    }
    List<BrowserContextMenuItem> submenuItems = snapshotItems(submenu, commands);
    if (submenuItems.isEmpty()) {
      return;
    }
    if (!items.isEmpty()) {
      items.add(new BrowserContextMenuItem.Separator());
    }
    items.addAll(submenuItems);
  }

  private static BrowserContextMenuItem.Command snapshotCommand(
      CefMenuModel model, int index, Map<BrowserContextMenuItem.CommandId, Integer> commands) {
    int cefCommandId = model.getCommandIdAt(index);
    if (cefCommandId < 0 || !model.isVisibleAt(index)) {
      return null;
    }
    BrowserContextMenuItem.CommandId commandId =
        new BrowserContextMenuItem.CommandId(commands.size() + 1L);
    commands.put(commandId, cefCommandId);
    return new BrowserContextMenuItem.Command(
        commandId,
        GrapheneCefContextMenuMapper.action(cefCommandId),
        GrapheneCefContextMenuMapper.label(model.getLabelAt(index)),
        model.isEnabledAt(index),
        model.isCheckedAt(index));
  }

  private static void clear(CefMenuModel model) {
    if (model != null) {
      model.clear();
    }
  }
}
