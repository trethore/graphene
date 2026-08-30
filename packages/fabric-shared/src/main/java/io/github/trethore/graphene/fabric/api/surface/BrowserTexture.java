package io.github.trethore.graphene.fabric.api.surface;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import io.github.trethore.graphene.api.ExperimentalGrapheneApi;
import io.github.trethore.graphene.fabric.internal.browser.GrapheneBrowserGpuTexture;
import java.util.Objects;
import net.minecraft.resources.Identifier;

/**
 * Borrowed access to a browser view's current Minecraft GPU texture.
 *
 * <p>Consumers must not close the returned texture, view, or sampler. The texture and view may be
 * replaced when the browser resolution changes, so callers must not retain them across frames.
 */
@ExperimentalGrapheneApi
@SuppressWarnings("unused")
public final class BrowserTexture {
    private final GrapheneBrowserGpuTexture texture;

    BrowserTexture(GrapheneBrowserGpuTexture texture) {
        this.texture = Objects.requireNonNull(texture, "texture");
    }

    public Identifier identifier() {
        return texture.identifier();
    }

    public GpuTexture texture() {
        return texture.gpuTexture();
    }

    public GpuTextureView view() {
        return texture.view();
    }

    public GpuSampler sampler() {
        return texture.getSampler();
    }

    public int width() {
        return texture.view().getWidth(0);
    }

    public int height() {
        return texture.view().getHeight(0);
    }
}
