package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefKeyboardHandler;
import org.cef.handler.CefKeyboardHandlerAdapter;
import org.cef.misc.EventFlags;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

final class GrapheneCefKeyboardHandler extends CefKeyboardHandlerAdapter {
    @Override
    public boolean onKeyEvent(CefBrowser browser, CefKeyboardHandler.CefKeyEvent event) {
        if (!GraphenePlatform.isMac()) {
            return false;
        }

        if (event.type != CefKeyboardHandler.CefKeyEvent.EventType.KEYEVENT_RAWKEYDOWN) {
            return false;
        }

        if (!event.is_system_key || (event.modifiers & EventFlags.EVENTFLAG_COMMAND_DOWN) == 0) {
            return false;
        }

        CefFrame focusedFrame = browser.getFocusedFrame();
        if (focusedFrame == null) {
            return false;
        }

        boolean shiftPressed = (event.modifiers & EventFlags.EVENTFLAG_SHIFT_DOWN) != 0;
        return switch (Character.toLowerCase(event.unmodified_character)) {
            case 'a' -> {
                focusedFrame.selectAll();
                yield true;
            }
            case 'c' -> {
                focusedFrame.copy();
                yield true;
            }
            case 'v' -> {
                focusedFrame.paste();
                yield true;
            }
            case 'x' -> {
                focusedFrame.cut();
                yield true;
            }
            case 'y' -> {
                focusedFrame.redo();
                yield true;
            }
            case 'z' -> {
                if (shiftPressed) {
                    focusedFrame.redo();
                } else {
                    focusedFrame.undo();
                }
                yield true;
            }
            default -> false;
        };
    }
}
