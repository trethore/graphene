package tytoo.grapheneui.internal.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneMimeTypesTest {
    @Test
    void resolvesKnownMimeTypeFromPath() {
        String mimeType = GrapheneMimeTypes.resolve("assets/my-mod-id/web/app.js");

        assertEquals("application/javascript", mimeType);
    }

    @Test
    void fallsBackToTextPlainForUnknownExtension() {
        String mimeType = GrapheneMimeTypes.resolve("assets/my-mod-id/web/custom.unknown");

        assertEquals(GrapheneMimeTypes.DEFAULT_MIME_TYPE, mimeType);
    }
}
