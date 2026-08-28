package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import java.nio.ByteBuffer;

final class GrapheneBrowserFrameUploader extends AbstractGrapheneBrowserFrameUploader {
    @Override
    protected void writeTexture(GrapheneBrowserGpuTexture texture, BrowserDirtyRegion region, ByteBuffer converted) {
        RenderSystem.getDevice()
                .createCommandEncoder()
                .writeToTexture(
                        texture.texture(), converted, 0, 0, region.x(), region.y(), region.width(), region.height());
    }
}
