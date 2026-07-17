package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuItem;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.fabric.internal.input.GrapheneInputModifiers;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

final class GrapheneContextMenuOverlay {
  private static final int BORDER_COLOR = 0xFF808080;
  private static final int BACKGROUND_COLOR = 0xF0101010;
  private static final int HOVER_COLOR = 0xFF2F5A88;
  private static final int TEXT_COLOR = 0xFFFFFFFF;
  private static final int DISABLED_TEXT_COLOR = 0xFF808080;
  private static final int SEPARATOR_COLOR = 0xFF606060;
  private static final int HORIZONTAL_PADDING = 7;
  private static final int VERTICAL_PADDING = 3;
  private static final int COMMAND_HEIGHT = 16;
  private static final int SEPARATOR_HEIGHT = 5;
  private static final int SCREEN_MARGIN = 2;

  private final CompletableFuture<BrowserContextMenuPresenter.Result> completion =
      new CompletableFuture<>();
  private final List<BrowserContextMenuItem> items;
  private final Font font;
  private final int x;
  private final int y;
  private final int width;
  private final int height;
  private int keyboardSelection = -1;

  GrapheneContextMenuOverlay(
      BrowserContextMenuPresenter.Request request,
      Font font,
      int anchorX,
      int anchorY,
      int screenWidth,
      int screenHeight) {
    this.font = font;
    this.items = request.items();
    int contentWidth =
        items.stream()
            .filter(BrowserContextMenuItem.Command.class::isInstance)
            .map(BrowserContextMenuItem.Command.class::cast)
            .mapToInt(command -> font.width(command.label()))
            .max()
            .orElse(80);
    width = Math.min(contentWidth + HORIZONTAL_PADDING * 2, screenWidth - SCREEN_MARGIN * 2);
    height =
        items.stream().mapToInt(GrapheneContextMenuOverlay::itemHeight).sum()
            + VERTICAL_PADDING * 2
            + 2;
    x = clampToScreen(anchorX, Math.max(SCREEN_MARGIN, screenWidth - width - SCREEN_MARGIN));
    y = clampToScreen(anchorY, Math.max(SCREEN_MARGIN, screenHeight - height - SCREEN_MARGIN));
  }

  CompletableFuture<BrowserContextMenuPresenter.Result> completion() {
    return completion;
  }

  void render(GuiGraphics graphics, int mouseX, int mouseY) {
    int hoveredRow = rowAt(mouseX, mouseY);
    int selectedRow = hoveredRow >= 0 ? hoveredRow : keyboardSelection;
    graphics.nextStratum();
    graphics.fill(x, y, x + width, y + height, BORDER_COLOR);
    graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND_COLOR);
    int itemY = y + VERTICAL_PADDING + 1;
    for (int index = 0; index < items.size(); index++) {
      BrowserContextMenuItem item = items.get(index);
      int itemHeight = itemHeight(item);
      if (item instanceof BrowserContextMenuItem.Separator) {
        int separatorY = itemY + itemHeight / 2;
        graphics.fill(
            x + HORIZONTAL_PADDING,
            separatorY,
            x + width - HORIZONTAL_PADDING,
            separatorY + 1,
            SEPARATOR_COLOR);
      } else if (item instanceof BrowserContextMenuItem.Command command) {
        if (index == selectedRow && command.enabled()) {
          graphics.fill(x + 1, itemY, x + width - 1, itemY + itemHeight, HOVER_COLOR);
        }
        String label = font.plainSubstrByWidth(command.label(), width - HORIZONTAL_PADDING * 2);
        graphics.drawString(
            font,
            label,
            x + HORIZONTAL_PADDING,
            itemY + (COMMAND_HEIGHT - font.lineHeight) / 2,
            command.enabled() ? TEXT_COLOR : DISABLED_TEXT_COLOR,
            false);
      }
      itemY += itemHeight;
    }
  }

  boolean mouseClicked(MouseButtonEvent event) {
    int rowIndex = rowAt(event.x(), event.y());
    if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && rowIndex >= 0) {
      select((BrowserContextMenuItem.Command) items.get(rowIndex), event.modifiers());
    } else {
      cancel();
    }
    return true;
  }

  void keyPressed(KeyEvent event) {
    switch (event.key()) {
      case GLFW.GLFW_KEY_ESCAPE -> cancel();
      case GLFW.GLFW_KEY_UP -> moveSelection(-1);
      case GLFW.GLFW_KEY_DOWN -> moveSelection(1);
      case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER ->
          selectKeyboardSelection(event.modifiers());
      default -> {
        // Ignore keys that do not control the context menu.
      }
    }
  }

  void cancel() {
    completion.complete(BrowserContextMenuPresenter.Result.cancel());
  }

  private void select(BrowserContextMenuItem.Command command, int modifiers) {
    if (!command.enabled()) {
      return;
    }
    completion.complete(
        BrowserContextMenuPresenter.Result.select(
            command, GrapheneInputModifiers.fromGlfw(modifiers)));
  }

  private void selectKeyboardSelection(int modifiers) {
    if (keyboardSelection < 0) {
      return;
    }
    select((BrowserContextMenuItem.Command) items.get(keyboardSelection), modifiers);
  }

  private void moveSelection(int direction) {
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

  private static int itemHeight(BrowserContextMenuItem item) {
    return item instanceof BrowserContextMenuItem.Separator ? SEPARATOR_HEIGHT : COMMAND_HEIGHT;
  }

  private static int clampToScreen(int value, int maximum) {
    return Math.clamp(value, SCREEN_MARGIN, maximum);
  }
}
