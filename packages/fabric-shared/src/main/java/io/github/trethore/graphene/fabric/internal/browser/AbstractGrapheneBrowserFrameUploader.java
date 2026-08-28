package io.github.trethore.graphene.fabric.internal.browser;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import io.github.trethore.graphene.internal.browser.GrapheneFramePixelConverter;
import io.github.trethore.graphene.internal.browser.GrapheneFrameUploadPlanner;
import java.nio.ByteBuffer;

abstract class AbstractGrapheneBrowserFrameUploader {
    private final GrapheneFramePixelConverter pixelConverter = new GrapheneFramePixelConverter();

    final void upload(GrapheneBrowserGpuTexture texture, BrowserFrame frame, boolean transparent) {
        if (texture.isUploaded(frame.sequence())) {
            return;
        }
        boolean consecutiveSequence = texture.canApplyDirtyRegions(frame.sequence());
        for (BrowserDirtyRegion region : GrapheneFrameUploadPlanner.regions(frame, consecutiveSequence)) {
            ByteBuffer converted = pixelConverter.convert(frame, region, transparent);
            writeTexture(texture, region, converted);
        }
        texture.markUploaded(frame.sequence());
    }

    protected abstract void writeTexture(
            GrapheneBrowserGpuTexture texture, BrowserDirtyRegion region, ByteBuffer converted);
}
