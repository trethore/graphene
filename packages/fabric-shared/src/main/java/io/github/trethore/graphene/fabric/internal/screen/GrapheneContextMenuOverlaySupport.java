package io.github.trethore.graphene.fabric.internal.screen;

import io.github.trethore.graphene.api.browser.menu.BrowserContextMenuPresenter;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;

interface GrapheneContextMenuOverlaySupport {
    CompletableFuture<BrowserContextMenuPresenter.Result> completion();

    boolean mouseClicked(MouseButtonEvent event);

    void keyPressed(KeyEvent event);

    void cancel();
}
