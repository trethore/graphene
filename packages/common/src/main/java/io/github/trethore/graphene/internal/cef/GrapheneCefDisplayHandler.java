package io.github.trethore.graphene.internal.cef;

import io.github.trethore.graphene.api.browser.BrowserConsoleSeverity;
import io.github.trethore.graphene.internal.platform.GrapheneTaskExecutor;
import java.util.Objects;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;

final class GrapheneCefDisplayHandler extends CefDisplayHandlerAdapter {
  private final GrapheneTaskExecutor taskExecutor;

  GrapheneCefDisplayHandler(GrapheneTaskExecutor taskExecutor) {
    this.taskExecutor = Objects.requireNonNull(taskExecutor, "taskExecutor");
  }

  @Override
  public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
    if (!(browser instanceof GrapheneCefBrowserSession session) || !isMainFrame(frame)) {
      return;
    }
    schedule(session.updateUrl(url));
  }

  @Override
  public void onTitleChange(CefBrowser browser, String title) {
    if (browser instanceof GrapheneCefBrowserSession session) {
      schedule(session.updateTitle(title));
    }
  }

  @Override
  public boolean onConsoleMessage(
      CefBrowser browser, CefSettings.LogSeverity level, String message, String source, int line) {
    if (browser instanceof GrapheneCefBrowserSession session) {
      schedule(session.consoleMessage(severity(level), message, source, line));
    }
    return false;
  }

  static BrowserConsoleSeverity severity(CefSettings.LogSeverity severity) {
    if (severity == null) {
      return BrowserConsoleSeverity.UNKNOWN;
    }
    return switch (severity) {
      case LOGSEVERITY_DEFAULT, LOGSEVERITY_INFO -> BrowserConsoleSeverity.INFO;
      case LOGSEVERITY_VERBOSE -> BrowserConsoleSeverity.VERBOSE;
      case LOGSEVERITY_WARNING -> BrowserConsoleSeverity.WARNING;
      case LOGSEVERITY_ERROR -> BrowserConsoleSeverity.ERROR;
      case LOGSEVERITY_FATAL -> BrowserConsoleSeverity.FATAL;
      case LOGSEVERITY_DISABLE -> BrowserConsoleSeverity.UNKNOWN;
    };
  }

  private static boolean isMainFrame(CefFrame frame) {
    return frame == null || frame.isMain();
  }

  private void schedule(Runnable notification) {
    if (notification != null) {
      taskExecutor.execute(notification);
    }
  }
}
