package io.github.trethore.graphene.api.browser.menu;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@FunctionalInterface
public interface BrowserContextMenuPolicy {
  List<BrowserContextMenuItem> configure(Request request);

  static BrowserContextMenuPolicy standard() {
    Set<BrowserContextMenuAction> actions =
        Set.copyOf(
            EnumSet.of(
                BrowserContextMenuAction.BACK,
                BrowserContextMenuAction.FORWARD,
                BrowserContextMenuAction.RELOAD,
                BrowserContextMenuAction.RELOAD_WITHOUT_CACHE,
                BrowserContextMenuAction.STOP_LOADING,
                BrowserContextMenuAction.UNDO,
                BrowserContextMenuAction.REDO,
                BrowserContextMenuAction.CUT,
                BrowserContextMenuAction.COPY,
                BrowserContextMenuAction.PASTE,
                BrowserContextMenuAction.DELETE,
                BrowserContextMenuAction.SELECT_ALL,
                BrowserContextMenuAction.SPELLING_SUGGESTION));
    return request -> filter(request.proposedItems(), actions);
  }

  static BrowserContextMenuPolicy disabled() {
    return request -> List.of();
  }

  record Request(BrowserContextMenuContext context, List<BrowserContextMenuItem> proposedItems) {
    public Request {
      Objects.requireNonNull(context, "context");
      proposedItems = List.copyOf(Objects.requireNonNull(proposedItems, "proposedItems"));
    }
  }

  private static List<BrowserContextMenuItem> filter(
      List<BrowserContextMenuItem> items, Set<BrowserContextMenuAction> actions) {
    ArrayList<BrowserContextMenuItem> filtered = new ArrayList<>();
    boolean pendingSeparator = false;
    for (BrowserContextMenuItem item : items) {
      if (item instanceof BrowserContextMenuItem.Separator) {
        pendingSeparator = !filtered.isEmpty();
      } else {
        BrowserContextMenuItem.Command command = (BrowserContextMenuItem.Command) item;
        if (actions.contains(command.action())) {
          if (pendingSeparator) {
            filtered.add(new BrowserContextMenuItem.Separator());
            pendingSeparator = false;
          }
          filtered.add(command);
        }
      }
    }
    return List.copyOf(filtered);
  }
}
