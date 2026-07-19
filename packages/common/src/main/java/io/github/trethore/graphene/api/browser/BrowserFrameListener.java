package io.github.trethore.graphene.api.browser;

/** Receives complete browser frame snapshots. */
@FunctionalInterface
public interface BrowserFrameListener {
  void onFrame(BrowserFrame frame);
}
