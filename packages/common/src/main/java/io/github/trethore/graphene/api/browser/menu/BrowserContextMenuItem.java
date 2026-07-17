package io.github.trethore.graphene.api.browser.menu;

import java.util.Objects;

public sealed interface BrowserContextMenuItem
    permits BrowserContextMenuItem.Command, BrowserContextMenuItem.Separator {
  record Command(
      CommandId id, BrowserContextMenuAction action, String label, boolean enabled, boolean checked)
      implements BrowserContextMenuItem {
    public Command {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(action, "action");
      Objects.requireNonNull(label, "label");
    }
  }

  record Separator() implements BrowserContextMenuItem {}

  record CommandId(long value) {
    public CommandId {
      if (value <= 0) {
        throw new IllegalArgumentException("value must be positive");
      }
    }
  }
}
