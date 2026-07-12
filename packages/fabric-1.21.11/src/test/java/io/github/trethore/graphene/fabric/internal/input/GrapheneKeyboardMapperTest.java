package io.github.trethore.graphene.fabric.internal.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class GrapheneKeyboardMapperTest {
  @Test
  void mapsDomVirtualKeysAndLayoutCharacters() {
    assertEquals(0x0D, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_ENTER, '\r'));
    assertEquals(0x14, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_CAPS_LOCK, (char) 0));
    assertEquals(0xBA, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_SEMICOLON, ':'));
    assertEquals('A', GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_A, 'a'));
    assertEquals('1', GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_1, '&'));
    assertEquals('2', GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_2, 'é'));
    assertEquals(0x6A, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_KP_MULTIPLY, '*'));
    assertEquals(0x6B, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_KP_ADD, '+'));
    assertEquals(0x6D, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_KP_SUBTRACT, '-'));
    assertEquals(0x6E, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_KP_DECIMAL, '.'));
    assertEquals(0x6F, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_KP_DIVIDE, '/'));
    assertEquals(0xE2, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_WORLD_2, '<'));
  }

  @Test
  void preservesLinuxNativeScanCodes() {
    assertEquals(38, GrapheneKeyboardMapper.linuxNativeKeyCode(38));
    assertEquals(0, GrapheneKeyboardMapper.linuxNativeKeyCode(-1));
  }

  @Test
  void mapsDisabledNumpadDigitsToNavigationKeys() {
    assertEquals(0x2D, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_0, false));
    assertEquals(0x23, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_1, false));
    assertEquals(0x28, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_2, false));
    assertEquals(0x22, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_3, false));
    assertEquals(0x25, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_4, false));
    assertEquals(0x0C, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_5, false));
    assertEquals(0x27, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_6, false));
    assertEquals(0x24, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_7, false));
    assertEquals(0x26, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_8, false));
    assertEquals(0x21, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_9, false));
    assertEquals(0x61, GrapheneKeyboardMapper.keypadDigitVirtualKey(GLFW.GLFW_KEY_KP_1, true));
  }

  @Test
  void marksKeypadAndSideSpecificModifierKeys() {
    BrowserKeyInput keypad =
        GrapheneKeyboardMapper.map(
            GLFW.GLFW_KEY_KP_ADD,
            78,
            true,
            GLFW.GLFW_MOD_NUM_LOCK,
            Set.of(BrowserModifier.NUM_LOCK));
    BrowserKeyInput leftControl =
        GrapheneKeyboardMapper.map(
            GLFW.GLFW_KEY_LEFT_CONTROL,
            29,
            true,
            GLFW.GLFW_MOD_CONTROL,
            Set.of(BrowserModifier.CONTROL));

    assertTrue(keypad.modifiers().contains(BrowserModifier.KEYPAD));
    assertTrue(leftControl.modifiers().contains(BrowserModifier.LEFT));
    assertEquals(0x11, leftControl.keyCode());
  }
}
