package io.github.trethore.graphene.internal.cef;

import org.cef.browser.CefBrowser;
import org.cef.callback.CefBeforeDownloadCallback;
import org.cef.callback.CefDownloadItem;
import org.cef.callback.CefDownloadItemCallback;
import org.cef.handler.CefDownloadHandlerAdapter;

final class GrapheneCefDownloadHandler extends CefDownloadHandlerAdapter {
  @Override
  public boolean onBeforeDownload(
      CefBrowser browser,
      CefDownloadItem downloadItem,
      String suggestedName,
      CefBeforeDownloadCallback callback) {
    if (!(browser instanceof GrapheneCefBrowserSession session)) {
      return false;
    }
    return session.downloadRegistry().onBeforeDownload(downloadItem, suggestedName, callback);
  }

  @Override
  public void onDownloadUpdated(
      CefBrowser browser, CefDownloadItem downloadItem, CefDownloadItemCallback callback) {
    if (browser instanceof GrapheneCefBrowserSession session) {
      session.downloadRegistry().onDownloadUpdated(downloadItem, callback);
    }
  }
}
