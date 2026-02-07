package tytoo.grapheneui.cef;

import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;
import tytoo.grapheneui.browser.GrapheneBrowser;

final class GrapheneCefDisplayHandler extends CefDisplayHandlerAdapter {
    @Override
    public void onTitleChange(CefBrowser browser, String title) {
        if (browser instanceof GrapheneBrowser grapheneBrowser) {
            grapheneBrowser.onTitleChange(title);
        }
    }
}
