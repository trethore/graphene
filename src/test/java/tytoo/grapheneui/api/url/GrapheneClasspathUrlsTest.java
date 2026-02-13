package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneClasspathUrlsTest {
    @Test
    void assetBuildsExpectedClasspathUrl() {
        String url = GrapheneClasspathUrls.asset("graphene-ui", "graphene_test/welcome.html");

        assertEquals("classpath:///assets/graphene-ui/graphene_test/welcome.html", url);
    }

    @Test
    void assetBuildsExpectedClasspathUrlForCustomNamespace() {
        String url = GrapheneClasspathUrls.asset("my-mod-id", "/web/index.html");

        assertEquals("classpath:///assets/my-mod-id/web/index.html", url);
    }

    @Test
    void assetBuildsExpectedClasspathUrlFromIdentifier() {
        Identifier assetId = Identifier.fromNamespaceAndPath("my-mod-id", "web/index.html");
        String url = GrapheneClasspathUrls.asset(assetId);

        assertEquals("classpath:///assets/my-mod-id/web/index.html", url);
    }

    @Test
    void normalizeResourcePathStripsQueryAndFragmentAndDecodes() {
        String normalized = GrapheneClasspathUrls.normalizeResourcePath(
                "classpath:///assets/graphene-ui/graphene_test/a%20b.html?x=1#section"
        );

        assertEquals("assets/graphene-ui/graphene_test/a b.html", normalized);
    }

    @Test
    void normalizeResourcePathReturnsEmptyForNonClasspathScheme() {
        String normalized = GrapheneClasspathUrls.normalizeResourcePath("https://example.com/index.html");

        assertEquals("", normalized);
    }
}
