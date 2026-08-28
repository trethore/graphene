package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GrapheneFrameUploadPlanner {
    private static final int MAX_PARTIAL_UPLOADS = 64;
    private static final double FULL_UPLOAD_THRESHOLD = 0.45;

    private GrapheneFrameUploadPlanner() {}

    public static List<BrowserDirtyRegion> regions(BrowserFrame frame, boolean consecutiveSequence) {
        BrowserFrame validatedFrame = Objects.requireNonNull(frame, "frame");
        if (shouldUploadFullFrame(validatedFrame, consecutiveSequence)) {
            return List.of(new BrowserDirtyRegion(0, 0, validatedFrame.width(), validatedFrame.height()));
        }
        List<BrowserDirtyRegion> regions =
                new ArrayList<>(validatedFrame.dirtyRegions().size());
        for (BrowserDirtyRegion region : validatedFrame.dirtyRegions()) {
            int width = Math.min(region.width(), validatedFrame.width() - region.x());
            int height = Math.min(region.height(), validatedFrame.height() - region.y());
            if (width > 0 && height > 0) {
                regions.add(new BrowserDirtyRegion(region.x(), region.y(), width, height));
            }
        }
        return List.copyOf(regions);
    }

    public static boolean shouldUploadFullFrame(BrowserFrame frame, boolean consecutiveSequence) {
        BrowserFrame validatedFrame = Objects.requireNonNull(frame, "frame");
        if (!consecutiveSequence || validatedFrame.dirtyRegions().size() >= MAX_PARTIAL_UPLOADS) {
            return true;
        }
        long dirtyPixels = 0;
        long framePixels = (long) validatedFrame.width() * validatedFrame.height();
        for (BrowserDirtyRegion region : validatedFrame.dirtyRegions()) {
            int availableWidth = Math.max(0, validatedFrame.width() - region.x());
            int availableHeight = Math.max(0, validatedFrame.height() - region.y());
            int width = Math.clamp(region.width(), 0, availableWidth);
            int height = Math.clamp(region.height(), 0, availableHeight);
            dirtyPixels += (long) width * height;
            if ((double) dirtyPixels / framePixels >= FULL_UPLOAD_THRESHOLD) {
                return true;
            }
        }
        return dirtyPixels == 0;
    }
}
