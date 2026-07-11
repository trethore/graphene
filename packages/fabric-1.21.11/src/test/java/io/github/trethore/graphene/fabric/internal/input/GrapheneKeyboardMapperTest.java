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
    assertEquals(0xBA, GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_SEMICOLON, ':'));
    assertEquals('A', GrapheneKeyboardMapper.windowsVirtualKey(GLFW.GLFW_KEY_A, 'a'));
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
