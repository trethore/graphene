package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.Objects;

public final class GrapheneFramePixelConverter {
    private ByteBuffer buffer;

    public ByteBuffer convert(BrowserFrame frame, BrowserDirtyRegion region, boolean transparent) {
        BrowserFrame validatedFrame = Objects.requireNonNull(frame, "frame");
        BrowserDirtyRegion validatedRegion = Objects.requireNonNull(region, "region");
        int byteCount = Math.multiplyExact(Math.multiplyExact(validatedRegion.width(), validatedRegion.height()), 4);
        ByteBuffer converted = ensureBuffer(byteCount);
        ByteBuffer source = validatedFrame.pixels();
        converted.clear();
        for (int row = 0; row < validatedRegion.height(); row++) {
            int sourceIndex = (validatedRegion.y() + row) * validatedFrame.rowStrideBytes() + validatedRegion.x() * 4;
            for (int column = 0; column < validatedRegion.width(); column++) {
                int blue = Byte.toUnsignedInt(source.get(sourceIndex++));
                int green = Byte.toUnsignedInt(source.get(sourceIndex++));
                int red = Byte.toUnsignedInt(source.get(sourceIndex++));
                int alpha = Byte.toUnsignedInt(source.get(sourceIndex++));
                if (transparent) {
                    converted
                            .put((byte) unpremultiply(red, alpha))
                            .put((byte) unpremultiply(green, alpha))
                            .put((byte) unpremultiply(blue, alpha))
                            .put((byte) alpha);
                } else {
                    converted.put((byte) red).put((byte) green).put((byte) blue).put((byte) 0xFF);
                }
            }
        }
        converted.flip();
        return converted;
    }

    private ByteBuffer ensureBuffer(int capacity) {
        if (buffer == null || buffer.capacity() < capacity) {
            buffer = ByteBuffer.allocateDirect(capacity);
        }
        buffer.position(0);
        buffer.limit(capacity);
        return buffer;
    }

    private static int unpremultiply(int component, int alpha) {
        if (alpha == 0) {
            return 0;
        }
        return Math.min(255, (component * 255 + alpha / 2) / alpha);
    }
}
