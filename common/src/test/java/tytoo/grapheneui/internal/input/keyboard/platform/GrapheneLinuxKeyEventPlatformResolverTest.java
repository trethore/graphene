package tytoo.grapheneui.internal.input.keyboard.platform;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneLinuxKeyEventPlatformResolverTest {
    private final GrapheneLinuxKeyEventPlatformResolver resolver = new GrapheneLinuxKeyEventPlatformResolver();

    @Test
    void getNativeVirtualKeyCodeUsesX11KeySymForNavigationKeys() {
        int nativeKeyCode = resolver.getNativeVirtualKeyCode(GLFW.GLFW_KEY_LEFT, 113, KeyEvent.CHAR_UNDEFINED, true);

        assertEquals(0xFF51, nativeKeyCode);
    }

    @Test
    void getNativeVirtualKeyCodeUsesPrintableCharacterBeforeScanCode() {
        int nativeKeyCode = resolver.getNativeVirtualKeyCode(GLFW.GLFW_KEY_A, 38, 'a', true);

        assertEquals('a', nativeKeyCode);
    }

    @Test
    void isSystemKeyUsesAltModifier() {
        assertTrue(resolver.isSystemKey(GLFW.GLFW_MOD_ALT));
    }
}
