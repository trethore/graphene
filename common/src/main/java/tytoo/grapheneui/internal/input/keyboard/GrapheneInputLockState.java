package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

public final class GrapheneInputLockState {
    private final LongSupplier windowHandleSupplier;
    private boolean lockKeyModifiersEnabled;
    private boolean cachedNumLockState;
    private boolean cachedNumLockStateKnown;

    public GrapheneInputLockState() {
        this(GrapheneInputLockState::defaultWindowHandle);
    }

    public GrapheneInputLockState(LongSupplier windowHandleSupplier) {
        this.windowHandleSupplier = Objects.requireNonNull(windowHandleSupplier, "windowHandleSupplier");
        ensureLockKeyModifiersEnabled();
        Optional<Boolean> toolkitNumLockState = readToolkitNumLockState();
        if (toolkitNumLockState.isPresent()) {
            cachedNumLockState = toolkitNumLockState.get();
            cachedNumLockStateKnown = true;
        }
    }

    public static long defaultWindowHandle() {
        return GLFW.glfwGetCurrentContext();
    }

    private static Optional<Boolean> readToolkitNumLockState() {
        try {
            return Optional.of(Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_NUM_LOCK));
        } catch (Exception ignored) {
            // Toolkit lock state is not available on all platforms/toolkits.
            return Optional.empty();
        }
    }

    public boolean isNumLockEnabled(int modifiers) {
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

    public void updateCachedNumLockState(int keyCode, boolean pressed) {
        if (!pressed || keyCode != GLFW.GLFW_KEY_NUM_LOCK) {
            return;
        }

        cachedNumLockState = !cachedNumLockState;
        cachedNumLockStateKnown = true;
    }

    public void ensureLockKeyModifiersEnabled() {
        if (lockKeyModifiersEnabled) {
            return;
        }

        try {
            long windowHandle = windowHandleSupplier.getAsLong();
            if (windowHandle == 0L) {
                return;
            }

            GLFW.glfwSetInputMode(windowHandle, GLFW.GLFW_LOCK_KEY_MODS, GLFW.GLFW_TRUE);
            lockKeyModifiersEnabled = true;
        } catch (Exception ignored) {
            // Lock key modifiers are optional at runtime.
        }
    }
}
