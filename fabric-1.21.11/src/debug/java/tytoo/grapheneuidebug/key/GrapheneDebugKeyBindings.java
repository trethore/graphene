package tytoo.grapheneuidebug.key;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.mc.McClient;
import tytoo.grapheneuidebug.GrapheneDebugClient;
import tytoo.grapheneuidebug.screen.GrapheneBrowserDebugScreen;

public final class GrapheneDebugKeyBindings {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(GrapheneDebugClient.ID, "main")
    );

    private static final KeyMapping OPEN_BROWSER = new KeyMapping(
            "key.graphene-ui-debug.open_browser",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F10,
            CATEGORY
    );

    private static boolean registered = false;

    private GrapheneDebugKeyBindings() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        KeyBindingHelper.registerKeyBinding(OPEN_BROWSER);
        ClientTickEvents.END_CLIENT_TICK.register(ignored -> onClientTick());
        registered = true;
    }

    private static void onClientTick() {
        while (OPEN_BROWSER.consumeClick()) {
            McClient.setScreen(new GrapheneBrowserDebugScreen());
        }
    }
}
