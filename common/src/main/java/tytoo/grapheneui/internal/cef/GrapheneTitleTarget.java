package tytoo.grapheneui.internal.cef;

import org.cef.browser.CefBrowser;

public interface GrapheneTitleTarget {
    boolean updateTitle(String title);

    String currentTitle();

    CefBrowser browser();
}
