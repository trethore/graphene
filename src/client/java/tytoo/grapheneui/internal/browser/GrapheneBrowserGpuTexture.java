package tytoo.grapheneui.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;

final class GrapheneBrowserGpuTexture implements AutoCloseable {
    private static final long NEVER_UPLOADED = Long.MIN_VALUE;

    private final String label;
    private GpuTexture texture;
    private GpuTextureView view;
    private long lastUploadedVersion = NEVER_UPLOADED;

    GrapheneBrowserGpuTexture(String label) {
        this.label = label;
    }

    void ensureSize(int width, int height) {
        if (texture != null && texture.getWidth(0) == width && texture.getHeight(0) == height) {
            return;
        }

        close();
        texture = RenderSystem.getDevice().createTexture(
                () -> label,
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width,
                height,
                1,
                1
        );
        view = RenderSystem.getDevice().createTextureView(texture);
        lastUploadedVersion = NEVER_UPLOADED;
    }

    GpuTexture texture() {
        return texture;
    }

    GpuTextureView view() {
        return view;
    }

    boolean isUploaded(long frameVersion) {
        return lastUploadedVersion == frameVersion;
    }

    void markUploaded(long frameVersion) {
        lastUploadedVersion = frameVersion;
    }

    @Override
    public void close() {
        if (view != null) {
            view.close();
            view = null;
        }

        if (texture != null) {
            texture.close();
            texture = null;
        }

        lastUploadedVersion = NEVER_UPLOADED;
    }
}
