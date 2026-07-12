package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserPointerAction;
import io.github.trethore.graphene.api.browser.input.BrowserPointerButton;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import java.util.Set;
import org.cef.input.CefKeyEvent;
import org.cef.input.CefMouseEvent;
import org.cef.misc.EventFlags;
import org.junit.jupiter.api.Test;

class GrapheneCefInputTranslatorTest {
  @Test
  void translatesPointerButtonsAndModifiers() {
    BrowserPointerInput input =
        new BrowserPointerInput(
            BrowserPointerAction.PRESS,
            12,
            24,
            BrowserPointerButton.LEFT,
            2,
            Set.of(BrowserModifier.SHIFT));

    CefMouseEvent event = GrapheneCefInputTranslator.pointer(input);

    assertEquals(CefMouseEvent.MOUSEEVENT_PRESSED, event.type);
    assertEquals(CefMouseEvent.BUTTON_LEFT, event.button);
    assertEquals(
        EventFlags.EVENTFLAG_SHIFT_DOWN | EventFlags.EVENTFLAG_LEFT_MOUSE_BUTTON, event.modifiers);
  }

  @Test
  void preservesPressedButtonDuringPointerDrag() {
    BrowserPointerInput input =
        new BrowserPointerInput(
            BrowserPointerAction.DRAG, 18, 30, BrowserPointerButton.LEFT, 0, Set.of());

    CefMouseEvent event = GrapheneCefInputTranslator.pointer(input);

    assertEquals(CefMouseEvent.MOUSEEVENT_DRAGGED, event.type);
    assertEquals(CefMouseEvent.BUTTON_LEFT, event.button);
    assertEquals(EventFlags.EVENTFLAG_LEFT_MOUSE_BUTTON, event.modifiers);
  }

  @Test
  void preservesRawKeyboardCodes() {
    BrowserKeyInput input =
        new BrowserKeyInput(
            BrowserKeyAction.RELEASE,
            65,
            30,
            44,
            false,
            '\u0001',
            'a',
            Set.of(BrowserModifier.CONTROL));

    CefKeyEvent event = GrapheneCefInputTranslator.key(input);

    assertEquals(CefKeyEvent.KEYEVENT_KEYUP, event.type);
    assertEquals(65, event.windows_key_code);
    assertEquals(30, event.native_key_code);
    assertEquals(44, event.scan_code);
    assertEquals(EventFlags.EVENTFLAG_CONTROL_DOWN, event.modifiers);
    assertEquals('\u0001', event.character);
    assertEquals('a', event.unmodified_character);
  }

  @Test
  void preservesOriginatingKeyCodesForCharacterEvents() {
    BrowserKeyInput keyInput =
        new BrowserKeyInput(BrowserKeyAction.PRESS, 65, 38, 38, false, 'a', 'a', Set.of());
    CefKeyEvent event =
        GrapheneCefInputTranslator.text(new BrowserTextInput('a', Set.of()), keyInput);

    assertEquals(CefKeyEvent.KEYEVENT_CHAR, event.type);
    assertEquals(65, event.windows_key_code);
    assertEquals(38, event.native_key_code);
    assertEquals(38, event.scan_code);
    assertEquals('a', event.character);
    assertEquals('a', event.unmodified_character);
  }
}
