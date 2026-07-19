package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.ArrayList;
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
    BrowserFrame previousFrame = latestFrame.get();
    long frameSequence = ++sequence;
    List<BrowserDirtyRegion> normalizedDirtyRegions =
        previousFrame == null || previousFrame.width() != width || previousFrame.height() != height
            ? fullFrame(width, height)
            : normalizePaintDirtyRegions(width, height, dirtyRegions);
    mainFrame = new BrowserFrame(width, height, frameSequence, normalizedDirtyRegions, pixels);
    BrowserFrame frame =
        popupFrame == null || popupBounds == null
            ? mainFrame
            : compose(frameSequence, normalizedDirtyRegions);
    latestFrame.set(frame);
    return frame;
  }

  public synchronized BrowserFrame capturePopup(int width, int height, ByteBuffer pixels) {
    popupFrame =
        new BrowserFrame(
            width, height, 1, List.of(new BrowserDirtyRegion(0, 0, width, height)), pixels);
    if (mainFrame == null || popupBounds == null) {
      return null;
    }
    return publishComposite(List.of(popupBounds));
  }

  public synchronized BrowserFrame setPopupBounds(BrowserDirtyRegion bounds) {
    BrowserDirtyRegion validatedBounds = Objects.requireNonNull(bounds, "bounds");
    BrowserDirtyRegion previousBounds = popupBounds;
    popupBounds = validatedBounds;
    if (validatedBounds.equals(previousBounds)) {
      return null;
    }
    List<BrowserDirtyRegion> changedBounds = new ArrayList<>(2);
    if (previousBounds != null) {
      changedBounds.add(previousBounds);
    }
    changedBounds.add(validatedBounds);
    return publishComposite(changedBounds);
  }

  public synchronized BrowserFrame closePopup() {
    BrowserDirtyRegion previousBounds = popupBounds;
    popupBounds = null;
    popupFrame = null;
    if (mainFrame == null || previousBounds == null) {
      return null;
    }
    List<BrowserDirtyRegion> dirtyRegions =
        clipDirtyRegions(mainFrame.width(), mainFrame.height(), List.of(previousBounds));
    if (dirtyRegions.isEmpty()) {
      return null;
    }
    BrowserFrame frame = copyMain(++sequence, dirtyRegions);
    latestFrame.set(frame);
    return frame;
  }

  public BrowserFrame latestFrame() {
    return latestFrame.get();
  }

  public void clear() {
    synchronized (this) {
      mainFrame = null;
      popupFrame = null;
      popupBounds = null;
      latestFrame.set(null);
    }
  }

  private BrowserFrame publishComposite(List<BrowserDirtyRegion> changedRegions) {
    if (mainFrame == null || popupFrame == null || popupBounds == null) {
      return null;
    }
    List<BrowserDirtyRegion> dirtyRegions =
        clipDirtyRegions(mainFrame.width(), mainFrame.height(), changedRegions);
    if (dirtyRegions.isEmpty()) {
      return null;
    }
    BrowserFrame frame = compose(++sequence, dirtyRegions);
    latestFrame.set(frame);
    return frame;
  }

  private BrowserFrame compose(long frameSequence, List<BrowserDirtyRegion> dirtyRegions) {
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
        mainFrame.width(), mainFrame.height(), frameSequence, dirtyRegions, pixels);
  }

  private BrowserFrame copyMain(long frameSequence, List<BrowserDirtyRegion> dirtyRegions) {
    if (mainFrame == null) {
      return null;
    }
    return new BrowserFrame(
        mainFrame.width(), mainFrame.height(), frameSequence, dirtyRegions, mainFrame.pixels());
  }

  private static List<BrowserDirtyRegion> normalizePaintDirtyRegions(
      int frameWidth, int frameHeight, List<BrowserDirtyRegion> dirtyRegions) {
    List<BrowserDirtyRegion> validatedRegions =
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
    if (validatedRegions.isEmpty()) {
      return fullFrame(frameWidth, frameHeight);
    }
    List<BrowserDirtyRegion> normalizedRegions =
        clipDirtyRegions(frameWidth, frameHeight, validatedRegions);
    return normalizedRegions.isEmpty() ? fullFrame(frameWidth, frameHeight) : normalizedRegions;
  }

  private static List<BrowserDirtyRegion> clipDirtyRegions(
      int frameWidth, int frameHeight, List<BrowserDirtyRegion> dirtyRegions) {
    List<BrowserDirtyRegion> validatedRegions =
        Objects.requireNonNull(dirtyRegions, "dirtyRegions");
    List<BrowserDirtyRegion> normalizedRegions = new ArrayList<>(validatedRegions.size());
    for (BrowserDirtyRegion region : validatedRegions) {
      BrowserDirtyRegion validatedRegion = Objects.requireNonNull(region, "dirtyRegion");
      if (validatedRegion.x() >= frameWidth || validatedRegion.y() >= frameHeight) {
        continue;
      }
      int width = Math.min(validatedRegion.width(), frameWidth - validatedRegion.x());
      int height = Math.min(validatedRegion.height(), frameHeight - validatedRegion.y());
      normalizedRegions.add(
          new BrowserDirtyRegion(validatedRegion.x(), validatedRegion.y(), width, height));
    }
    return normalizedRegions;
  }

  private static List<BrowserDirtyRegion> fullFrame(int width, int height) {
    return List.of(new BrowserDirtyRegion(0, 0, width, height));
  }

  private static ByteBuffer writableCopy(ByteBuffer source) {
    ByteBuffer copy = ByteBuffer.allocateDirect(source.remaining());
    copy.put(source.duplicate());
    copy.flip();
    return copy;
  }
}
