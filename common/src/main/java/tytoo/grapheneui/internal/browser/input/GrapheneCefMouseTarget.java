package tytoo.grapheneui.internal.browser.input;

import org.cef.input.CefMouseEvent;
import org.cef.input.CefMouseWheelEvent;

public interface GrapheneCefMouseTarget {
    void dispatchMouseEvent(CefMouseEvent event);

    void dispatchMouseWheelEvent(CefMouseWheelEvent event);
}
