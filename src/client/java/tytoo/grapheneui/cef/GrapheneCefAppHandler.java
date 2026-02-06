package tytoo.grapheneui.cef;

import me.tytoo.jcefgithub.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.callback.CefSchemeRegistrar;

public final class GrapheneCefAppHandler extends MavenCefAppHandlerAdapter {
    private static final String CUSTOM_SCHEME_NAME = "classpath";
    private boolean schemeHandlerRegistered = false;

    @Override
    public synchronized void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        super.onRegisterCustomSchemes(registrar);
        registrar.addCustomScheme(CUSTOM_SCHEME_NAME, true, false, false, true, true, true, true);
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
