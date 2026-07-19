package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.config.BrowserFileAccessPolicy;
import io.github.trethore.graphene.api.url.GrapheneClasspathUrls;
import io.github.trethore.graphene.internal.url.GrapheneAppUrls;
import io.github.trethore.jcefgithub.MavenCefAppHandlerAdapter;
import java.util.Objects;
import org.cef.CefApp;
import org.cef.browser.CefRequestContext;
import org.cef.callback.CefSchemeRegistrar;

final class GrapheneCefAppHandler extends MavenCefAppHandlerAdapter {
  private static final String READ_GUARD =
      "profile.default_content_setting_values.file_system_read_guard";
  private static final String WRITE_GUARD =
      "profile.default_content_setting_values.file_system_write_guard";
  private static final String EXTENDED_PERMISSION =
      "profile.default_content_setting_values.file_system_access_extended_permission";

  private final BrowserFileAccessPolicy fileAccessPolicy;
  private boolean handlersRegistered;

  GrapheneCefAppHandler(BrowserFileAccessPolicy fileAccessPolicy) {
    this.fileAccessPolicy = Objects.requireNonNull(fileAccessPolicy, "fileAccessPolicy");
  }

  @Override
  public synchronized void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
    super.onRegisterCustomSchemes(registrar);
    boolean standardAppScheme = !GrapheneCefInstaller.isLinux();
    registrar.addCustomScheme(
        GrapheneAppUrls.SCHEME, standardAppScheme, false, false, true, true, false, true);
    registrar.addCustomScheme(
        GrapheneClasspathUrls.SCHEME, false, false, false, false, false, false, false);
  }

  @Override
  public synchronized void onContextInitialized() {
    super.onContextInitialized();
    if (handlersRegistered) {
      return;
    }
    int contentSetting = fileAccessPolicy == BrowserFileAccessPolicy.ALLOW ? 1 : 2;
    CefRequestContext context = CefRequestContext.getGlobalContext();
    context.setPreference(READ_GUARD, contentSetting);
    context.setPreference(WRITE_GUARD, contentSetting);
    context.setPreference(EXTENDED_PERMISSION, contentSetting);
    CefApp cefApp = CefApp.getInstance();
    if (cefApp != null) {
      GrapheneClasspathSchemeHandlerFactory factory = new GrapheneClasspathSchemeHandlerFactory();
      cefApp.registerSchemeHandlerFactory(GrapheneAppUrls.SCHEME, "", factory);
      cefApp.registerSchemeHandlerFactory(GrapheneClasspathUrls.SCHEME, "", factory);
      handlersRegistered = true;
    }
  }
}
