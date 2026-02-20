package tytoo.grapheneui.internal.resource;

import java.util.Locale;

public final class GrapheneMimeTypes {
    public static final String DEFAULT_MIME_TYPE = "text/plain";

    private static final MimeTypeRule[] MIME_TYPE_RULES = {
            new MimeTypeRule(".html", "text/html"),
            new MimeTypeRule(".htm", "text/html"),
            new MimeTypeRule(".js", "application/javascript"),
            new MimeTypeRule(".mjs", "text/javascript"),
            new MimeTypeRule(".css", "text/css"),
            new MimeTypeRule(".json", "application/json"),
            new MimeTypeRule(".png", "image/png"),
            new MimeTypeRule(".jpg", "image/jpeg"),
            new MimeTypeRule(".jpeg", "image/jpeg"),
            new MimeTypeRule(".gif", "image/gif"),
            new MimeTypeRule(".webp", "image/webp"),
            new MimeTypeRule(".ico", "image/x-icon"),
            new MimeTypeRule(".svg", "image/svg+xml"),
            new MimeTypeRule(".woff", "font/woff"),
            new MimeTypeRule(".woff2", "font/woff2"),
            new MimeTypeRule(".ttf", "font/ttf"),
            new MimeTypeRule(".otf", "font/otf"),
            new MimeTypeRule(".wasm", "application/wasm")
    };

    private GrapheneMimeTypes() {
    }

    public static String resolve(String path) {
        String normalizedPath = path.toLowerCase(Locale.ROOT);
        for (MimeTypeRule mimeTypeRule : MIME_TYPE_RULES) {
            if (normalizedPath.endsWith(mimeTypeRule.extension())) {
                return mimeTypeRule.mimeType();
            }
        }

        return DEFAULT_MIME_TYPE;
    }

    private record MimeTypeRule(String extension, String mimeType) {
    }
}
