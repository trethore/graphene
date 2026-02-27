package tytoo.grapheneui.internal.cef;

import io.github.trethore.jcefgithub.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefSchemeRegistrar;
import tytoo.grapheneui.api.url.GrapheneAppUrls;
import tytoo.grapheneui.api.url.GrapheneClasspathUrls;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

public final class GrapheneCefAppHandler extends MavenCefAppHandlerAdapter {
    private static final String APP_SCHEME_NAME = GrapheneAppUrls.SCHEME;
    private static final String CLASSPATH_SCHEME_NAME = GrapheneClasspathUrls.SCHEME;
    private boolean schemeHandlerRegistered = false;

    @Override
    public synchronized void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        super.onRegisterCustomSchemes(registrar);
        // linux does not support isStandard.
        registrar.addCustomScheme(APP_SCHEME_NAME, !GraphenePlatform.isLinux(), false, false, true, true, false, true);
        registrar.addCustomScheme(CLASSPATH_SCHEME_NAME, false, false, false, false, false, false, false);
    }

    @Override
    public synchronized void onContextInitialized() {
        super.onContextInitialized();

        if (schemeHandlerRegistered) {
            return;
        }

        // Allow file system access.
        CefRequestContext ctx = CefRequestContext.getGlobalContext();
        final int ALLOW = 1;
        ctx.setPreference(
                "profile.default_content_setting_values.file_system_read_guard", ALLOW);
        ctx.setPreference(
                "profile.default_content_setting_values.file_system_write_guard", ALLOW);
        ctx.setPreference(
                "profile.default_content_setting_values.file_system_access_extended_permission", ALLOW);

        CefApp cefApp = CefApp.getInstance();
        if (cefApp != null) {
            GrapheneClasspathSchemeHandlerFactory schemeHandlerFactory = new GrapheneClasspathSchemeHandlerFactory();
            cefApp.registerSchemeHandlerFactory(APP_SCHEME_NAME, "", schemeHandlerFactory);
            cefApp.registerSchemeHandlerFactory(CLASSPATH_SCHEME_NAME, "", schemeHandlerFactory);
            schemeHandlerRegistered = true;
        }
    }
}
