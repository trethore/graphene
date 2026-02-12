package tytoo.grapheneui.cef;

import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefContextMenuParams;
import org.cef.callback.CefMenuModel;
import org.cef.handler.CefContextMenuHandlerAdapter;

final class GrapheneCefContextMenuHandler extends CefContextMenuHandlerAdapter {
    @Override
    public void onBeforeContextMenu(CefBrowser browser, CefFrame frame, CefContextMenuParams params, CefMenuModel model) {
        if (model != null) {
            model.clear();
        }
    }
}
