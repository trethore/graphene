package io.github.trethore.graphene.fabric.internal.browser;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

public final class GrapheneBrowserGpuTexture extends AbstractTexture {
    private static final AtomicLong NEXT_ID = new AtomicLong();

    private final Identifier identifier = Identifier.fromNamespaceAndPath(
            "grapheneui", "browser/" + Long.toUnsignedString(NEXT_ID.getAndIncrement()));
    private long uploadedSequence = Long.MIN_VALUE;
    private boolean released;

    public GrapheneBrowserGpuTexture() {
        sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
        Minecraft.getInstance().getTextureManager().register(identifier, this);
    }

    void ensureSize(int width, int height) {
        if (texture != null && texture.getWidth(0) == width && texture.getHeight(0) == height) {
            return;
        }
        closeGpuResources();
        texture = GrapheneBrowserGpuTextureFactory.create(width, height);
        textureView = RenderSystem.getDevice().createTextureView(texture);
    }

    GpuTexture texture() {
        return getTexture();
    }

    public Identifier identifier() {
        return identifier;
    }

    public boolean isReady() {
        return texture != null && textureView != null;
    }

    public GpuTexture gpuTexture() {
        return getTexture();
    }

    public GpuTextureView view() {
        return getTextureView();
    }

    boolean isUploaded(long sequence) {
        return uploadedSequence == sequence;
    }

    boolean canApplyDirtyRegions(long sequence) {
        return uploadedSequence != Long.MIN_VALUE && sequence == uploadedSequence + 1;
    }

    void markUploaded(long sequence) {
        uploadedSequence = sequence;
    }

    public void release() {
        if (released) {
            return;
        }
        released = true;
        Minecraft.getInstance().getTextureManager().release(identifier);
    }

    @Override
    public void close() {
        closeGpuResources();
    }

    private void closeGpuResources() {
        if (textureView != null) {
            textureView.close();
            textureView = null;
        }
        if (texture != null) {
            texture.close();
            texture = null;
        }
        uploadedSequence = Long.MIN_VALUE;
    }
}
