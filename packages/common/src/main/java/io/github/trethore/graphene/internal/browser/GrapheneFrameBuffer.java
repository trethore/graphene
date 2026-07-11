package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class GrapheneFrameBuffer {
  private final AtomicReference<BrowserFrame> latestFrame = new AtomicReference<>();
  private BrowserFrame mainFrame;
  private BrowserFrame popupFrame;
  private BrowserDirtyRegion popupBounds;
  private long sequence;

  public synchronized BrowserFrame capture(
      int width, int height, List<BrowserDirtyRegion> dirtyRegions, ByteBuffer pixels) {
    mainFrame =
        new BrowserFrame(
            width,
            height,
            ++sequence,
            Objects.requireNonNull(dirtyRegions, "dirtyRegions"),
            pixels);
    BrowserFrame frame =
        popupFrame == null || popupBounds == null ? mainFrame : compose(mainFrame.dirtyRegions());
    latestFrame.set(frame);
    return frame;
  }

  public synchronized BrowserFrame capturePopup(int width, int height, ByteBuffer pixels) {
    popupFrame =
        new BrowserFrame(
            width, height, 1, List.of(new BrowserDirtyRegion(0, 0, width, height)), pixels);
    BrowserFrame frame = compose(popupBounds == null ? List.of() : List.of(popupBounds));
    latestFrame.set(frame);
    return frame;
  }

  public synchronized void setPopupBounds(BrowserDirtyRegion bounds) {
    popupBounds = bounds;
    if (mainFrame != null && popupFrame != null) {
      latestFrame.set(compose(List.of(bounds)));
    }
  }

  public synchronized void closePopup() {
    BrowserDirtyRegion previousBounds = popupBounds;
    popupBounds = null;
    popupFrame = null;
    if (mainFrame != null) {
      latestFrame.set(copyMain(previousBounds == null ? List.of() : List.of(previousBounds)));
    }
  }

  public BrowserFrame latestFrame() {
    return latestFrame.get();
  }

  public void clear() {
    synchronized (this) {
      mainFrame = null;
      popupFrame = null;
      popupBounds = null;
    }
    latestFrame.set(null);
  }

  private BrowserFrame compose(List<BrowserDirtyRegion> dirtyRegions) {
    if (mainFrame == null || popupFrame == null || popupBounds == null) {
      return mainFrame;
    }
    ByteBuffer pixels = writableCopy(mainFrame.pixels());
    int copyWidth = Math.min(popupFrame.width(), mainFrame.width() - popupBounds.x());
    int copyHeight = Math.min(popupFrame.height(), mainFrame.height() - popupBounds.y());
    if (copyWidth > 0 && copyHeight > 0) {
      ByteBuffer popupPixels = popupFrame.pixels();
      for (int row = 0; row < copyHeight; row++) {
        int sourceOffset = row * popupFrame.width() * 4;
        int targetOffset = ((popupBounds.y() + row) * mainFrame.width() + popupBounds.x()) * 4;
        for (int column = 0; column < copyWidth * 4; column++) {
          pixels.put(targetOffset + column, popupPixels.get(sourceOffset + column));
        }
      }
    }
    return new BrowserFrame(
        mainFrame.width(), mainFrame.height(), ++sequence, dirtyRegions, pixels);
  }

  private BrowserFrame copyMain(List<BrowserDirtyRegion> dirtyRegions) {
    if (mainFrame == null) {
      return null;
    }
    return new BrowserFrame(
        mainFrame.width(), mainFrame.height(), ++sequence, dirtyRegions, mainFrame.pixels());
  }

  private static ByteBuffer writableCopy(ByteBuffer source) {
    ByteBuffer copy = ByteBuffer.allocateDirect(source.remaining());
    copy.put(source.duplicate());
    copy.flip();
    return copy;
  }
}
