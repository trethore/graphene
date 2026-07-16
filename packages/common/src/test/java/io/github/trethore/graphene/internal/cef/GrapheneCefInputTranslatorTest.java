package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.input.BrowserKey;
import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserKeyLocation;
import io.github.trethore.graphene.api.browser.input.BrowserKeyPlatform;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserPointerAction;
import io.github.trethore.graphene.api.browser.input.BrowserPointerButton;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserRawKeyMetadata;
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
  void mapsPhysicalKeysAndRawPlatformMetadata() {
    BrowserKeyInput input =
        new BrowserKeyInput(
            BrowserKeyAction.RELEASE,
            BrowserKey.KEY_A,
            BrowserKeyLocation.STANDARD,
            Set.of(BrowserModifier.CONTROL),
            new BrowserRawKeyMetadata(BrowserKeyPlatform.LINUX, 44, 'a'));

    CefKeyEvent event = GrapheneCefInputTranslator.key(input);

    assertEquals(CefKeyEvent.KEYEVENT_KEYUP, event.type);
    assertEquals(65, event.windows_key_code);
    assertEquals(44, event.native_key_code);
    assertEquals(44, event.scan_code);
    assertEquals(EventFlags.EVENTFLAG_CONTROL_DOWN, event.modifiers);
    assertEquals('\u0001', event.character);
    assertEquals('a', event.unmodified_character);
  }

  @Test
  void preservesOriginatingKeyCodesForCharacterEvents() {
    BrowserKeyInput keyInput =
        new BrowserKeyInput(
            BrowserKeyAction.PRESS,
            BrowserKey.KEY_A,
            BrowserKeyLocation.STANDARD,
            Set.of(),
            new BrowserRawKeyMetadata(BrowserKeyPlatform.LINUX, 38, 'a'));
    CefKeyEvent event =
        GrapheneCefInputTranslator.text(new BrowserTextInput("a", Set.of()), keyInput)[0];

    assertEquals(CefKeyEvent.KEYEVENT_CHAR, event.type);
    assertEquals(65, event.windows_key_code);
    assertEquals(38, event.native_key_code);
    assertEquals(38, event.scan_code);
    assertEquals('a', event.character);
    assertEquals('a', event.unmodified_character);
  }

  @Test
  void emitsOneCharacterEventPerUtf16CodeUnit() {
    CefKeyEvent[] events =
        GrapheneCefInputTranslator.text(new BrowserTextInput("\uD83D\uDE00", Set.of()), null);

    assertEquals(2, events.length);
    assertEquals('\uD83D', events[0].character);
    assertEquals('\uDE00', events[1].character);
  }

  @Test
  void mapsPhysicalKeyLocationToCefFlags() {
    BrowserKeyInput input =
        new BrowserKeyInput(
            BrowserKeyAction.PRESS, BrowserKey.ENTER, BrowserKeyLocation.NUMPAD, Set.of());

    CefKeyEvent event = GrapheneCefInputTranslator.key(input);

    assertEquals(EventFlags.EVENTFLAG_IS_KEY_PAD, event.modifiers);
  }

  @Test
  void preservesPhysicalIdentityWhileApplyingTheHostLayout() {
    BrowserKeyInput input =
        new BrowserKeyInput(
            BrowserKeyAction.PRESS,
            BrowserKey.KEY_A,
            BrowserKeyLocation.STANDARD,
            Set.of(),
            new BrowserRawKeyMetadata(BrowserKeyPlatform.LINUX, 38, 'q'));

    CefKeyEvent event = GrapheneCefInputTranslator.key(input);

    assertEquals('Q', event.windows_key_code);
    assertEquals('q', event.unmodified_character);
  }

  @Test
  void appliesNumLockToPhysicalNumpadKeys() {
    BrowserKeyInput navigationInput =
        new BrowserKeyInput(
            BrowserKeyAction.PRESS, BrowserKey.DIGIT_1, BrowserKeyLocation.NUMPAD, Set.of());
    BrowserKeyInput digitInput =
        new BrowserKeyInput(
            BrowserKeyAction.PRESS,
            BrowserKey.DIGIT_1,
            BrowserKeyLocation.NUMPAD,
            Set.of(BrowserModifier.NUM_LOCK));

    assertEquals(0x23, GrapheneCefInputTranslator.key(navigationInput).windows_key_code);
    assertEquals(0x61, GrapheneCefInputTranslator.key(digitInput).windows_key_code);
  }

  @Test
  void derivesWindowsNativeMetadataFromPhysicalKeys() {
    BrowserKeyInput input =
        new BrowserKeyInput(
            BrowserKeyAction.RELEASE,
            BrowserKey.ARROW_LEFT,
            BrowserKeyLocation.STANDARD,
            Set.of(BrowserModifier.ALT),
            new BrowserRawKeyMetadata(BrowserKeyPlatform.WINDOWS, 0, 0));

    CefKeyEvent event = GrapheneCefInputTranslator.key(input);

    assertEquals(75, event.scan_code);
    assertEquals(CefKeyEvent.buildWindowsNativeKeyCode(75, true, true), event.native_key_code);
    assertTrue(event.is_system_key);
  }

  @Test
  void derivesMacNativeMetadataFromPhysicalKeys() {
    BrowserKeyInput input =
        new BrowserKeyInput(
            BrowserKeyAction.PRESS,
            BrowserKey.KEY_B,
            BrowserKeyLocation.STANDARD,
            Set.of(),
            new BrowserRawKeyMetadata(BrowserKeyPlatform.MACOS, 0, 'b'));

    CefKeyEvent event = GrapheneCefInputTranslator.key(input);

    assertEquals(11, event.scan_code);
    assertEquals(11, event.native_key_code);
  }
}
