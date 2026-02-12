package tytoo.grapheneui.internal.browser;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.mc.McClient;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;

final class GrapheneInputLockState {
    private boolean lockKeyModifiersEnabled;
    private boolean fallbackNumLockState;
    private boolean fallbackNumLockStateKnown;

    GrapheneInputLockState() {
        ensureLockKeyModifiersEnabled();
        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            fallbackNumLockState = toolkitNumLockState.get();
            fallbackNumLockStateKnown = true;
        }
    }

    private static Optional<Boolean> readToolkitNumLockState() {
        try {
            return Optional.of(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK));
        } catch (Exception _) {
            // Toolkit lock state is not available on all platforms/toolkits.
            return Optional.empty();
        }
    }

    boolean isNumLockEnabled(int modifiers) {
        boolean numLockModifierSet = (modifiers & GLFW.GLFW_MOD_NUM_LOCK) != 0;
        if (numLockModifierSet) {
            fallbackNumLockState = true;
            fallbackNumLockStateKnown = true;
            return true;
        }

        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            fallbackNumLockState = toolkitNumLockState.get();
            fallbackNumLockStateKnown = true;
            return fallbackNumLockState;
        }

        if (fallbackNumLockStateKnown) {
            return fallbackNumLockState;
        }

        return false;
    }

    void updateFallbackNumLockState(int keyCode, boolean pressed) {
        if (!pressed || keyCode != GLFW.GLFW_KEY_NUM_LOCK) {
            return;
        }

        fallbackNumLockState = !fallbackNumLockState;
        fallbackNumLockStateKnown = true;
    }

    void ensureLockKeyModifiersEnabled() {
        if (lockKeyModifiersEnabled) {
            return;
        }

        try {
            long windowHandle = McClient.mc().getWindow().handle();
            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_LOCK_KEY_MODS, GLFW.GLFW_TRUE);
            lockKeyModifiersEnabled = true;
        } catch (Exception _) {
            // Lock key modifiers are optional at runtime.
        }
    }
}
