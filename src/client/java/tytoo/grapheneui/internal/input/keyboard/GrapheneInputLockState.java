package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.mc.McClient;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;

final class GrapheneInputLockState {
    private boolean lockKeyModifiersEnabled;
    private boolean cachedNumLockState;
    private boolean cachedNumLockStateKnown;

    GrapheneInputLockState() {
        ensureLockKeyModifiersEnabled();
        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            cachedNumLockState = toolkitNumLockState.get();
            cachedNumLockStateKnown = true;
        }
    }

    private static Optional<Boolean> readToolkitNumLockState() {
        try {
            return Optional.of(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK));
        } catch (Exception ignored) {
            // Toolkit lock state is not available on all platforms/toolkits.
            return Optional.empty();
        }
    }

    boolean isNumLockEnabled(int modifiers) {
        boolean numLockModifierSet = (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0;
        if (numLockModifierSet) {
            cachedNumLockState = true;
            cachedNumLockStateKnown = true;
            return true;
        }

        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            cachedNumLockState = toolkitNumLockState.get();
            cachedNumLockStateKnown = true;
            return cachedNumLockState;
        }

        if (cachedNumLockStateKnown) {
            return cachedNumLockState;
        }

        return false;
    }

    void updateCachedNumLockState(int keyCode, boolean pressed) {
        if (!pressed || keyCode != GLFW.GLFW_KEY_NUM_LOCK) {
            return;
        }

        cachedNumLockState = !cachedNumLockState;
        cachedNumLockStateKnown = true;
    }

    void ensureLockKeyModifiersEnabled() {
        if (lockKeyModifiersEnabled) {
            return;
        }

        try {
            long windowHandle = McClient.mc().getWindow().handle();
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_LOCK_KEY_MODS, GLFW.GLFW_TRUE);
            lockKeyModifiersEnabled = true;
        } catch (Exception ignored) {
            // Lock key modifiers are optional at runtime.
        }
    }
}
