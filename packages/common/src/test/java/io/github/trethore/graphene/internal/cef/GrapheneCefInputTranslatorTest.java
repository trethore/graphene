package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserPointerAction;
import io.github.trethore.graphene.api.browser.input.BrowserPointerButton;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
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
  void preservesRawKeyboardCodes() {
    BrowserKeyInput input =
        new BrowserKeyInput(
            BrowserKeyAction.RELEASE, 65, 30, 44, false, Set.of(BrowserModifier.CONTROL));

    CefKeyEvent event = GrapheneCefInputTranslator.key(input);

    assertEquals(CefKeyEvent.KEYEVENT_KEYUP, event.type);
    assertEquals(65, event.windows_key_code);
    assertEquals(30, event.native_key_code);
    assertEquals(44, event.scan_code);
    assertEquals(EventFlags.EVENTFLAG_CONTROL_DOWN, event.modifiers);
  }
}
