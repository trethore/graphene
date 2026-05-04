package tytoo.grapheneui.internal.input.keyboard.platform;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneMacKeyEventPlatformResolverTest {
    private final GrapheneMacKeyEventPlatformResolver resolver = new GrapheneMacKeyEventPlatformResolver();

    @Test
    void getNativeVirtualKeyCodeUsesMacRawCharacterOverrides() {
        int nativeKeyCode = resolver.getNativeVirtualKeyCode(GLFW.GLFW_KEY_LEFT, 0, KeyEvent.CHAR_UNDEFINED, true);

        assertEquals(0xF702, nativeKeyCode);
    }

    @Test
    void getNativeVirtualKeyCodeUsesMacPhysicalMappingForKnownGlfwKey() {
        int nativeKeyCode = resolver.getNativeVirtualKeyCode(GLFW.GLFW_KEY_A, 0, 'a', true);

        assertEquals(0x00, nativeKeyCode);
    }

    @Test
    void isSystemKeyUsesSuperModifier() {
        assertTrue(resolver.isSystemKey(GLFW.GLFW_MOD_SUPER));
    }
}
