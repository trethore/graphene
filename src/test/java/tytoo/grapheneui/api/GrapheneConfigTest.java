package tytoo.grapheneui.api;

import org.junit.jupiter.api.Test;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneFileSystemAccessMode;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneConfigTest {
    @Test
    void defaultsDoNotEnableHttpServer() {
        GrapheneConfig config = GrapheneConfig.defaults();

        assertTrue(config.jcefDownloadPath().isEmpty());
        assertTrue(config.extensionFolders().isEmpty());
        assertTrue(config.http().isEmpty());
        assertEquals(GrapheneFileSystemAccessMode.DENY, config.fileSystemAccessMode());
    }

    @Test
    void builderCanEnableHttpServer() {
        GrapheneHttpConfig httpConfig = GrapheneHttpConfig.builder()
                .randomPortInRange(20_000, 20_010)
                .build();
        GrapheneConfig config = GrapheneConfig.builder()
                .http(httpConfig)
                .build();

        assertTrue(config.http().isPresent());
    }

    @Test
    void builderCanAllowFileSystemAccess() {
        GrapheneConfig config = GrapheneConfig.builder()
                .allowFileSystemAccess()
                .build();

        assertEquals(GrapheneFileSystemAccessMode.ALLOW, config.fileSystemAccessMode());
    }

    @Test
    void builderCanDefineJcefAndMultipleExtensionFolders() {
        GrapheneConfig config = GrapheneConfig.builder()
                .jcefDownloadPath("./custom-jcef")
                .extensionFolder("./extensions/mod-a")
                .extensionFolder(Path.of("./extensions/mod-b"))
                .build();

        assertEquals(Path.of("custom-jcef"), config.jcefDownloadPath().orElseThrow());
        assertEquals(2, config.extensionFolders().size());
    }
}
