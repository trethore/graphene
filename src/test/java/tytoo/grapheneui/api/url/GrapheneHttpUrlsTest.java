package tytoo.grapheneui.api.url;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

final class GrapheneHttpUrlsTest {
    @Test
    void assetThrowsWhenHttpServerIsNotRunning() {
        assertThrows(IllegalStateException.class, () -> GrapheneHttpUrls.asset("my-mod-id", "web/index.html"));
    }
}
