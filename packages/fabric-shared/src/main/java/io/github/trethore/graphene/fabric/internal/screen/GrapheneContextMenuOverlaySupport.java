package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import io.github.trethore.graphene.fabric.internal.render.GrapheneGuiGraphics;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

interface GrapheneContextMenuOverlaySupport {
    CompletableFuture<BrowserContextMenuPresenter.Result> completion();

    void render(GrapheneGuiGraphics graphics, int mouseX, int mouseY);

    boolean mouseClicked(MouseButtonEvent event);

    void keyPressed(KeyEvent event);

    void cancel();
}
