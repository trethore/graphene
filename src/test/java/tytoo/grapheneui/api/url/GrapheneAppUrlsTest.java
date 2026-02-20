package tytoo.grapheneui.api.url;

import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneAppUrlsTest {
    @Test
    void assetBuildsExpectedAppUrl() {
        String url = GrapheneAppUrls.asset("graphene-ui", "graphene_test/welcome.html");

        assertEquals("app://assets/graphene-ui/graphene_test/welcome.html", url);
    }

    @Test
    void assetBuildsExpectedAppUrlForCustomNamespace() {
        String url = GrapheneAppUrls.asset("my-mod-id", "/web/index.html");

        assertEquals("app://assets/my-mod-id/web/index.html", url);
    }

    @Test
    void assetBuildsExpectedAppUrlFromIdentifier() {
        Identifier assetId = Identifier.fromNamespaceAndPath("my-mod-id", "web/index.html");
        String url = GrapheneAppUrls.asset(assetId);

        assertEquals("app://assets/my-mod-id/web/index.html", url);
    }

    @Test
    void normalizeResourcePathStripsQueryAndFragmentAndDecodes() {
        String normalized = GrapheneAppUrls.normalizeResourcePath(
                "app://assets/graphene-ui/graphene_test/a%20b.html?x=1#section"
        );

        assertEquals("assets/graphene-ui/graphene_test/a b.html", normalized);
    }

    @Test
    void normalizeResourcePathSupportsNamespaceHostUrls() {
        String normalized = GrapheneAppUrls.normalizeResourcePath(
                "app://my-mod-id/web/a%20b.html?x=1#section"
        );

        assertEquals("assets/my-mod-id/web/a b.html", normalized);
    }

    @Test
    void normalizeResourcePathSupportsHostlessAssetsForm() {
        String normalized = GrapheneAppUrls.normalizeResourcePath(
                "app:///assets/my-mod-id/web/a%20b.html?x=1#section"
        );

        assertEquals("assets/my-mod-id/web/a b.html", normalized);
    }

    @Test
    void normalizeResourcePathReturnsEmptyForNonAppScheme() {
        String normalized = GrapheneAppUrls.normalizeResourcePath("https://example.com/index.html");

        assertEquals("", normalized);
    }
}
