package io.github.trethore.graphene.fabric.internal.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.browser.input.BrowserKey;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserKeyLocation;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

class GrapheneKeyboardMapperTest {
  @Test
  void mapsGlfwKeysToPhysicalKeys() {
    assertEquals(BrowserKey.KEY_A, GrapheneKeyboardMapper.key(GLFW.GLFW_KEY_A));
    assertEquals(BrowserKey.DIGIT_1, GrapheneKeyboardMapper.key(GLFW.GLFW_KEY_1));
    assertEquals(BrowserKey.DIGIT_1, GrapheneKeyboardMapper.key(GLFW.GLFW_KEY_KP_1));
    assertEquals(BrowserKey.ENTER, GrapheneKeyboardMapper.key(GLFW.GLFW_KEY_KP_ENTER));
    assertEquals(BrowserKey.F25, GrapheneKeyboardMapper.key(GLFW.GLFW_KEY_F25));
    assertEquals(BrowserKey.INTERNATIONAL_2, GrapheneKeyboardMapper.key(GLFW.GLFW_KEY_WORLD_2));
    assertEquals(BrowserKey.UNKNOWN, GrapheneKeyboardMapper.key(GLFW.GLFW_KEY_UNKNOWN));
  }

  @Test
  void mapsKeyLocationsSeparatelyFromModifierState() {
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

    assertEquals(BrowserKey.ADD, keypad.key());
    assertEquals(BrowserKeyLocation.NUMPAD, keypad.location());
    assertEquals(BrowserKey.CONTROL, leftControl.key());
    assertEquals(BrowserKeyLocation.LEFT, leftControl.location());
    assertEquals(29, leftControl.rawMetadata().orElseThrow().scanCode());
  }
}
