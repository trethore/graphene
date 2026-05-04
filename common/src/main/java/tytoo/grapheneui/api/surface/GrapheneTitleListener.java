package tytoo.grapheneui.api.surface;

import org.cef.browser.CefBrowser;

@FunctionalInterface
public interface GrapheneTitleListener {
    void onTitleChange(CefBrowser browser, String title);
}
