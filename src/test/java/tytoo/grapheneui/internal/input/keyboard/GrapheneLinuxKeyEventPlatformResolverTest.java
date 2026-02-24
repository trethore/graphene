package tytoo.grapheneui.internal.input.keyboard;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneLinuxKeyEventPlatformResolverTest {
    private final GrapheneLinuxKeyEventPlatformResolver resolver = new GrapheneLinuxKeyEventPlatformResolver();

    @Test
    void sanitizeCharEventModifiersLeavesModifiersWhenRightAltIsNotPressed() {
        int modifiers = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT;

        int sanitizedModifiers = resolver.sanitizeCharEventModifiers(modifiers, false);

        assertEquals(modifiers, sanitizedModifiers);
    }

    @Test
    void sanitizeCharEventModifiersClearsAltGrControlAndAltModifiers() {
        int modifiers = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT;

        int sanitizedModifiers = resolver.sanitizeCharEventModifiers(modifiers, true);

        assertEquals(GLFW.GLFW_MOD_SHIFT, sanitizedModifiers);
    }

    @Test
    void sanitizeCharEventModifiersClearsAltWhenAltGrReportsNoControlModifier() {
        int modifiers = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT;

        int sanitizedModifiers = resolver.sanitizeCharEventModifiers(modifiers, true);

        assertEquals(GLFW.GLFW_MOD_SHIFT, sanitizedModifiers);
    }
}
