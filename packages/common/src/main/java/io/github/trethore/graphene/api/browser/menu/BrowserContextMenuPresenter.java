package io.github.trethore.graphene.api.browser.menu;

import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface BrowserContextMenuPresenter {
  CompletionStage<Result> show(Request request);

  record Request(BrowserContextMenuContext context, List<BrowserContextMenuItem> items) {
    public Request {
      Objects.requireNonNull(context, "context");
      items = List.copyOf(Objects.requireNonNull(items, "items"));
    }
  }

  record Result(
      Optional<BrowserContextMenuItem.CommandId> selectedCommand, Set<BrowserModifier> modifiers) {
    public Result {
      Objects.requireNonNull(selectedCommand, "selectedCommand");
      modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
    }

    public static Result select(BrowserContextMenuItem.Command command) {
      return select(command, Set.of());
    }

    public static Result select(
        BrowserContextMenuItem.Command command, Set<BrowserModifier> modifiers) {
      return new Result(Optional.of(Objects.requireNonNull(command, "command").id()), modifiers);
    }

    public static Result cancel() {
      return new Result(Optional.empty(), Set.of());
    }
  }
}
