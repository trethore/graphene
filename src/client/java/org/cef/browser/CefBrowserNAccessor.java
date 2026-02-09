package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;

import java.awt.*;

public abstract class CefBrowserNAccessor extends CefBrowser_N {
    protected CefBrowserNAccessor(
            CefClient client,
            String url,
            CefRequestContext context,
            CefBrowser_N parent,
            Point inspectAt,
            CefBrowserSettings settings
    ) {
        super(client, url, context, parent, inspectAt, settings);
    }

    protected CefBrowserNAccessor(
            CefClient client,
            String url,
            CefRequestContext context,
            CefBrowserNAccessor parent,
            Point inspectAt,
            CefBrowserSettings settings
    ) {
        super(client, url, context, parent, inspectAt, settings);
    }

    protected abstract CefBrowserNAccessor createDevToolsBrowserAccessor(
            CefClient client,
            String url,
            CefRequestContext context,
            CefBrowserNAccessor parent,
            Point inspectAt
    );

    @Override
    protected final CefBrowserNAccessor createDevToolsBrowser(
            CefClient client,
            String url,
            CefRequestContext context,
            CefBrowser_N parent,
            Point inspectAt
    ) {
        CefBrowserNAccessor accessorParent = parent instanceof CefBrowserNAccessor cefBrowserNAccessor ? cefBrowserNAccessor : null;
        return createDevToolsBrowserAccessor(client, url, context, accessorParent, inspectAt);
    }
}
