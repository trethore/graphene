package io.github.trethore.graphene.internal.browser.menu;

import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuItem;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.ToIntFunction;

public final class GrapheneContextMenuModel {
    public static final int HORIZONTAL_PADDING = 7;
    public static final int VERTICAL_PADDING = 3;
    public static final int COMMAND_HEIGHT = 16;
    public static final int SEPARATOR_HEIGHT = 5;
    public static final int SCREEN_MARGIN = 2;

    private final CompletableFuture<BrowserContextMenuPresenter.Result> completion = new CompletableFuture<>();
    private final List<BrowserContextMenuItem> items;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private int keyboardSelection = -1;

    public GrapheneContextMenuModel(
            BrowserContextMenuPresenter.Request request,
            ToIntFunction<String> textWidth,
            int anchorX,
            int anchorY,
            int screenWidth,
            int screenHeight) {
        BrowserContextMenuPresenter.Request validatedRequest = Objects.requireNonNull(request, "request");
        ToIntFunction<String> validatedTextWidth = Objects.requireNonNull(textWidth, "textWidth");
        items = validatedRequest.items();
        int contentWidth = items.stream()
                .filter(BrowserContextMenuItem.Command.class::isInstance)
                .map(BrowserContextMenuItem.Command.class::cast)
                .mapToInt(command -> validatedTextWidth.applyAsInt(command.label()))
                .max()
                .orElse(80);
        width = Math.min(contentWidth + HORIZONTAL_PADDING * 2, screenWidth - SCREEN_MARGIN * 2);
        height = items.stream().mapToInt(GrapheneContextMenuModel::itemHeight).sum() + VERTICAL_PADDING * 2 + 2;
        x = clampToScreen(anchorX, Math.max(SCREEN_MARGIN, screenWidth - width - SCREEN_MARGIN));
        y = clampToScreen(anchorY, Math.max(SCREEN_MARGIN, screenHeight - height - SCREEN_MARGIN));
    }

    public CompletableFuture<BrowserContextMenuPresenter.Result> completion() {
        return completion;
    }

    public List<BrowserContextMenuItem> items() {
        return items;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int selectedRow(double mouseX, double mouseY) {
        int hoveredRow = rowAt(mouseX, mouseY);
        return hoveredRow >= 0 ? hoveredRow : keyboardSelection;
    }

    public int itemTop(int index) {
        int itemY = y + VERTICAL_PADDING + 1;
        for (int itemIndex = 0; itemIndex < index; itemIndex++) {
            itemY += itemHeight(items.get(itemIndex));
        }
        return itemY;
    }

    public void click(double mouseX, double mouseY, boolean primaryButton, Set<BrowserModifier> modifiers) {
        int rowIndex = rowAt(mouseX, mouseY);
        if (primaryButton && rowIndex >= 0) {
            select(rowIndex, modifiers);
        } else {
            cancel();
        }
    }

    public void keyPressed(KeyAction action, Set<BrowserModifier> modifiers) {
        switch (Objects.requireNonNull(action, "action")) {
            case CANCEL -> cancel();
            case PREVIOUS -> moveSelection(-1);
            case NEXT -> moveSelection(1);
            case SELECT -> selectKeyboardSelection(modifiers);
            case IGNORE -> {
                // Ignore keys that do not control the context menu.
            }
        }
    }

    public void cancel() {
        completion.complete(BrowserContextMenuPresenter.Result.cancel());
    }

    public static int itemHeight(BrowserContextMenuItem item) {
        return item instanceof BrowserContextMenuItem.Separator ? SEPARATOR_HEIGHT : COMMAND_HEIGHT;
    }

    private void select(int index, Set<BrowserModifier> modifiers) {
        BrowserContextMenuItem item = items.get(index);
        if (!(item instanceof BrowserContextMenuItem.Command command) || !command.enabled()) {
            return;
        }
        completion.complete(BrowserContextMenuPresenter.Result.select(
                command, Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"))));
    }

    private void selectKeyboardSelection(Set<BrowserModifier> modifiers) {
        if (keyboardSelection >= 0) {
            select(keyboardSelection, modifiers);
        }
    }

    private void moveSelection(int direction) {
        if (items.isEmpty()) {
            return;
        }
        int index = keyboardSelection;
        for (int count = 0; count < items.size(); count++) {
            index = Math.floorMod(index + direction, items.size());
            BrowserContextMenuItem item = items.get(index);
            if (item instanceof BrowserContextMenuItem.Command command && command.enabled()) {
                keyboardSelection = index;
                return;
            }
        }
    }

    private int rowAt(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return -1;
        }
        int itemY = y + VERTICAL_PADDING + 1;
        for (int index = 0; index < items.size(); index++) {
            BrowserContextMenuItem item = items.get(index);
            int itemHeight = itemHeight(item);
            if (mouseY >= itemY && mouseY < itemY + itemHeight) {
                return item instanceof BrowserContextMenuItem.Command ? index : -1;
            }
            itemY += itemHeight;
        }
        return -1;
    }

    private static int clampToScreen(int value, int maximum) {
        return Math.clamp(value, SCREEN_MARGIN, maximum);
    }

    public enum KeyAction {
        CANCEL,
        PREVIOUS,
        NEXT,
        SELECT,
        IGNORE
    }
}
