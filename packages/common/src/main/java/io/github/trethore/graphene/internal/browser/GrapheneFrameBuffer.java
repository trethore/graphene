package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class GrapheneFrameBuffer {
  private final AtomicReference<BrowserFrame> latestFrame = new AtomicReference<>();
  private long sequence;

  public synchronized BrowserFrame capture(
      int width, int height, List<BrowserDirtyRegion> dirtyRegions, ByteBuffer pixels) {
    BrowserFrame frame =
        new BrowserFrame(
            width,
            height,
            ++sequence,
            Objects.requireNonNull(dirtyRegions, "dirtyRegions"),
            pixels);
    latestFrame.set(frame);
    return frame;
  }

  public BrowserFrame latestFrame() {
    return latestFrame.get();
  }

  public void clear() {
    latestFrame.set(null);
  }
}
