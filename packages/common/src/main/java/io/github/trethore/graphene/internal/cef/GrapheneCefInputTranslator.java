package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.input.BrowserKeyAction;
import io.github.trethore.graphene.api.browser.input.BrowserKeyInput;
import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserPointerAction;
import io.github.trethore.graphene.api.browser.input.BrowserPointerButton;
import io.github.trethore.graphene.api.browser.input.BrowserPointerInput;
import io.github.trethore.graphene.api.browser.input.BrowserScrollInput;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import java.util.Set;
import org.cef.input.CefKeyEvent;
import org.cef.input.CefMouseEvent;
import org.cef.input.CefMouseWheelEvent;
import org.cef.misc.EventFlags;

final class GrapheneCefInputTranslator {
  private GrapheneCefInputTranslator() {}

  static CefMouseEvent pointer(BrowserPointerInput input) {
    int modifiers = modifiers(input.modifiers()) | buttonModifier(input.button(), input.action());
    return new CefMouseEvent(
        pointerAction(input.action()),
        input.x(),
        input.y(),
        modifiers,
        pointerButton(input.button()),
        input.clickCount());
  }

  static CefMouseWheelEvent scroll(BrowserScrollInput input) {
    return new CefMouseWheelEvent(
        input.x(), input.y(), modifiers(input.modifiers()), input.deltaX(), input.deltaY());
  }

  static CefKeyEvent key(BrowserKeyInput input) {
    return new CefKeyEvent(
        input.action() == BrowserKeyAction.PRESS
            ? CefKeyEvent.KEYEVENT_RAWKEYDOWN
            : CefKeyEvent.KEYEVENT_KEYUP,
        modifiers(input.modifiers()),
        input.keyCode(),
        input.nativeKeyCode(),
        input.systemKey(),
        (char) 0,
        (char) 0,
        input.scanCode());
  }

  static CefKeyEvent text(BrowserTextInput input) {
    return new CefKeyEvent(
        CefKeyEvent.KEYEVENT_CHAR,
        modifiers(input.modifiers()),
        input.character(),
        0,
        false,
        input.character(),
        input.character(),
        0);
  }

  static int modifiers(Set<BrowserModifier> modifiers) {
    int flags = EventFlags.EVENTFLAG_NONE;
    for (BrowserModifier modifier : modifiers) {
      flags |= modifierFlag(modifier);
    }
    return flags;
  }

  private static int modifierFlag(BrowserModifier modifier) {
    return switch (modifier) {
      case SHIFT -> EventFlags.EVENTFLAG_SHIFT_DOWN;
      case CONTROL -> EventFlags.EVENTFLAG_CONTROL_DOWN;
      case ALT -> EventFlags.EVENTFLAG_ALT_DOWN;
      case META -> EventFlags.EVENTFLAG_COMMAND_DOWN;
      case CAPS_LOCK -> EventFlags.EVENTFLAG_CAPS_LOCK_ON;
      case NUM_LOCK -> EventFlags.EVENTFLAG_NUM_LOCK_ON;
      case KEYPAD -> EventFlags.EVENTFLAG_IS_KEY_PAD;
      case LEFT -> EventFlags.EVENTFLAG_IS_LEFT;
      case RIGHT -> EventFlags.EVENTFLAG_IS_RIGHT;
    };
  }

  private static int pointerAction(BrowserPointerAction action) {
    return switch (action) {
      case MOVE -> CefMouseEvent.MOUSEEVENT_MOVED;
      case DRAG -> CefMouseEvent.MOUSEEVENT_DRAGGED;
      case ENTER -> CefMouseEvent.MOUSEEVENT_ENTERED;
      case EXIT -> CefMouseEvent.MOUSEEVENT_EXITED;
      case PRESS -> CefMouseEvent.MOUSEEVENT_PRESSED;
      case RELEASE -> CefMouseEvent.MOUSEEVENT_RELEASED;
    };
  }

  private static int pointerButton(BrowserPointerButton button) {
    return switch (button) {
      case NONE -> CefMouseEvent.BUTTON_NONE;
      case LEFT -> CefMouseEvent.BUTTON_LEFT;
      case MIDDLE -> CefMouseEvent.BUTTON_MIDDLE;
      case RIGHT -> CefMouseEvent.BUTTON_RIGHT;
    };
  }

  private static int buttonModifier(BrowserPointerButton button, BrowserPointerAction action) {
    if (action != BrowserPointerAction.PRESS && action != BrowserPointerAction.DRAG) {
      return EventFlags.EVENTFLAG_NONE;
    }
    return switch (button) {
      case LEFT -> EventFlags.EVENTFLAG_LEFT_MOUSE_BUTTON;
      case MIDDLE -> EventFlags.EVENTFLAG_MIDDLE_MOUSE_BUTTON;
      case RIGHT -> EventFlags.EVENTFLAG_RIGHT_MOUSE_BUTTON;
      case NONE -> EventFlags.EVENTFLAG_NONE;
    };
  }
}
