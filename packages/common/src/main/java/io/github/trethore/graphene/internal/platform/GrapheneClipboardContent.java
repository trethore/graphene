package io.github.trethore.graphene.internal.platform;

import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public record GrapheneClipboardContent(String text, String html, byte[] png) {
    public GrapheneClipboardContent {
        png = png == null ? new byte[0] : Arrays.copyOf(png, png.length);
    }

    @Override
    public byte[] png() {
        return Arrays.copyOf(png, png.length);
    }

    @Override
    public boolean equals(Object object) {
        return this == object
                || object instanceof GrapheneClipboardContent(String text1, String html1, byte[] png1)
                        && Objects.equals(text, text1)
                        && Objects.equals(html, html1)
                        && Arrays.equals(png, png1);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(text, html) + Arrays.hashCode(png);
    }

    @Override
    @NotNull
    public String toString() {
        return "GrapheneClipboardContent[text=" + text + ", html=" + html + ", png=" + Arrays.toString(png) + ']';
    }

    public boolean isEmpty() {
        return (text == null || text.isEmpty()) && (html == null || html.isEmpty()) && png.length == 0;
    }

    public GrapheneClipboardContent reconcileNativeText(String nativeText) {
        String normalizedNativeText = emptyToNull(nativeText);
        if (Objects.equals(normalizedNativeText, emptyToNull(text))) {
            return this;
        }
        return new GrapheneClipboardContent(normalizedNativeText, null, null);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
