package io.github.trethore.graphene.api.browser.menu;

import java.util.Objects;

/** An item that may be presented in a browser context menu. */
public sealed interface BrowserContextMenuItem
    permits BrowserContextMenuItem.Command, BrowserContextMenuItem.Separator {
  /** An executable browser command proposed for a context menu. */
  record Command(
      CommandId id, BrowserContextMenuAction action, String label, boolean enabled, boolean checked)
      implements BrowserContextMenuItem {
    public Command {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(action, "action");
      Objects.requireNonNull(label, "label");
    }
  }

  /** A visual separator between context-menu commands. */
  record Separator() implements BrowserContextMenuItem {}

  /** Identifier used to select a proposed context-menu command. */
  record CommandId(long value) {
    public CommandId {
      if (value <= 0) {
        throw new IllegalArgumentException("value must be positive");
      }
    }
  }
}
