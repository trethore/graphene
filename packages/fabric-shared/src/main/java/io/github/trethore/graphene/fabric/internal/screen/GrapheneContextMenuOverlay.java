package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuItem;
import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import io.github.trethore.graphene.internal.browser.menu.GrapheneContextMenuModel;
import io.github.trethore.graphene.minecraft.internal.input.GrapheneInputModifiers;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.Font;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.lwjgl.glfw.GLFW;

final class GrapheneContextMenuOverlay implements GrapheneContextMenuOverlaySupport {
    private static final int BORDER_COLOR = 0xFF808080;
    private static final int BACKGROUND_COLOR = 0xF0101010;
    private static final int HOVER_COLOR = 0xFF2F5A88;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int DISABLED_TEXT_COLOR = 0xFF808080;
    private static final int SEPARATOR_COLOR = 0xFF606060;

    private final GrapheneContextMenuModel model;
    private final Font font;

    GrapheneContextMenuOverlay(
            BrowserContextMenuPresenter.Request request,
            Font font,
            int anchorX,
            int anchorY,
            int screenWidth,
            int screenHeight) {
        this.font = font;
        model = new GrapheneContextMenuModel(request, font::width, anchorX, anchorY, screenWidth, screenHeight);
    }

    @Override
    public CompletableFuture<BrowserContextMenuPresenter.Result> completion() {
        return model.completion();
    }

    @Override
    public void render(GrapheneGuiGraphics graphics, int mouseX, int mouseY) {
        int selectedRow = model.selectedRow(mouseX, mouseY);
        int x = model.x();
        int y = model.y();
        int width = model.width();
        int height = model.height();
        graphics.graphene$nextStratum();
        graphics.graphene$fill(x, y, x + width, y + height, BORDER_COLOR);
        graphics.graphene$fill(x + 1, y + 1, x + width - 1, y + height - 1, BACKGROUND_COLOR);
        List<BrowserContextMenuItem> items = model.items();
        for (int index = 0; index < items.size(); index++) {
            BrowserContextMenuItem item = items.get(index);
            int itemY = model.itemTop(index);
            int itemHeight = GrapheneContextMenuModel.itemHeight(item);
            if (item instanceof BrowserContextMenuItem.Separator) {
                int separatorY = itemY + itemHeight / 2;
                graphics.graphene$fill(
                        x + GrapheneContextMenuModel.HORIZONTAL_PADDING,
                        separatorY,
                        x + width - GrapheneContextMenuModel.HORIZONTAL_PADDING,
                        separatorY + 1,
                        SEPARATOR_COLOR);
            } else if (item instanceof BrowserContextMenuItem.Command command) {
                if (index == selectedRow && command.enabled()) {
                    graphics.graphene$fill(x + 1, itemY, x + width - 1, itemY + itemHeight, HOVER_COLOR);
                }
                String label = font.plainSubstrByWidth(
                        command.label(), width - GrapheneContextMenuModel.HORIZONTAL_PADDING * 2);
                graphics.graphene$text(
                        font,
                        label,
                        x + GrapheneContextMenuModel.HORIZONTAL_PADDING,
                        itemY + (GrapheneContextMenuModel.COMMAND_HEIGHT - font.lineHeight) / 2,
                        command.enabled() ? TEXT_COLOR : DISABLED_TEXT_COLOR,
                        false);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event) {
        model.click(
                event.x(),
                event.y(),
                event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT,
                GrapheneInputModifiers.fromGlfw(event.modifiers()));
        return true;
    }

    @Override
    public void keyPressed(KeyEvent event) {
        GrapheneContextMenuModel.KeyAction action =
                switch (event.key()) {
                    case GLFW.GLFW_KEY_ESCAPE -> GrapheneContextMenuModel.KeyAction.CANCEL;
                    case GLFW.GLFW_KEY_UP -> GrapheneContextMenuModel.KeyAction.PREVIOUS;
                    case GLFW.GLFW_KEY_DOWN -> GrapheneContextMenuModel.KeyAction.NEXT;
                    case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> GrapheneContextMenuModel.KeyAction.SELECT;
                    default -> GrapheneContextMenuModel.KeyAction.IGNORE;
                };
        model.keyPressed(action, GrapheneInputModifiers.fromGlfw(event.modifiers()));
    }

    @Override
    public void cancel() {
        model.cancel();
    }
}
