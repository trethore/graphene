package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.internal.bridge.BridgeBrowser;
import java.util.Objects;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;

final class GrapheneCefBrowserAdapter implements BridgeBrowser {
  private final CefBrowser browser;

  GrapheneCefBrowserAdapter(CefBrowser browser) {
    this.browser = Objects.requireNonNull(browser, "browser");
  }

  @Override
  public void executeScript(String script, String sourceUrl) {
    browser.executeJavaScript(script, sourceUrl, 1);
  }

  @Override
  public String currentUrl() {
    return browser.getURL();
  }

  @Override
  public boolean hasDocument() {
    CefFrame frame = browser.getMainFrame();
    return frame != null && frame.isValid();
  }

  @Override
  public int identifier() {
    return browser.getIdentifier();
  }
}
