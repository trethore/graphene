package io.github.trethore.graphene.fabric.internal.browser;

import io.github.trethore.graphene.api.browser.BrowserFrame;

public final class GrapheneBrowserFrameTexture implements AutoCloseable {
    private final GrapheneBrowserGpuTexture texture = new GrapheneBrowserGpuTexture();
    private final GrapheneBrowserFrameUploader uploader = new GrapheneBrowserFrameUploader();

    public GrapheneBrowserGpuTexture update(BrowserFrame frame, boolean transparent) {
        texture.ensureSize(frame.width(), frame.height());
        uploader.upload(texture, frame, transparent);
        return texture;
    }

    public GrapheneBrowserGpuTexture texture() {
        return texture;
    }

    @Override
    public void close() {
        texture.release();
    }
}
