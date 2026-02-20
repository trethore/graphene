package tytoo.grapheneui.internal.cef;

import me.tytoo.jcefgithub.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.callback.CefSchemeRegistrar;
import tytoo.grapheneui.api.url.GrapheneAppUrls;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;

public final class GrapheneCefAppHandler extends MavenCefAppHandlerAdapter {
    private static final String APP_SCHEME_NAME = GrapheneAppUrls.SCHEME;
    private static final String CLASSPATH_SCHEME_NAME = GrapheneClasspathUrls.SCHEME;
    private boolean schemeHandlerRegistered = false;

    @Override
    public synchronized void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        super.onRegisterCustomSchemes(registrar);
        registrar.addCustomScheme(APP_SCHEME_NAME, true, false, false, true, true, false, true);
        registrar.addCustomScheme(CLASSPATH_SCHEME_NAME, false, false, false, false, false, false, false);
    }

    @Override
    public synchronized void onContextInitialized() {
        super.onContextInitialized();

        if (schemeHandlerRegistered) {
            return;
        }

        CefApp cefApp = CefApp.getInstance();
        if (cefApp != null) {
            GrapheneClasspathSchemeHandlerFactory schemeHandlerFactory = new GrapheneClasspathSchemeHandlerFactory();
            cefApp.registerSchemeHandlerFactory(APP_SCHEME_NAME, "", schemeHandlerFactory);
            cefApp.registerSchemeHandlerFactory(CLASSPATH_SCHEME_NAME, "", schemeHandlerFactory);
            schemeHandlerRegistered = true;
        }
    }
}
