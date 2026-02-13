package tytoo.grapheneui.internal.cef;

import me.tytoo.jcefgithub.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.callback.CefSchemeRegistrar;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;

public final class GrapheneCefAppHandler extends MavenCefAppHandlerAdapter {
    private static final String CUSTOM_SCHEME_NAME = GrapheneClasspathUrls.SCHEME;
    private boolean schemeHandlerRegistered = false;

    @Override
    public synchronized void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        super.onRegisterCustomSchemes(registrar);
        registrar.addCustomScheme(CUSTOM_SCHEME_NAME, false, false, false, false, false, false, false);
    }

    @Override
    public synchronized void onContextInitialized() {
        super.onContextInitialized();

        if (schemeHandlerRegistered) {
            return;
        }

        CefApp cefApp = CefApp.getInstance();
        if (cefApp != null) {
            cefApp.registerSchemeHandlerFactory(CUSTOM_SCHEME_NAME, "", new GrapheneClasspathSchemeHandlerFactory());
            schemeHandlerRegistered = true;
        }
    }
}
