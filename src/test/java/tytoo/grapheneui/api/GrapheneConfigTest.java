package tytoo.grapheneui.api;

import org.junit.jupiter.api.Test;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneFileSystemAccessMode;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.config.GrapheneHttpConfig;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneConfigTest {
    @Test
    void defaultsUseEmptyContainerAndDenyGlobalFileAccess() {
        GrapheneConfig config = GrapheneConfig.defaults();

        assertTrue(config.container().http().isEmpty());
        assertTrue(config.global().jcefDownloadPath().isEmpty());
        assertTrue(config.global().extensionFolders().isEmpty());
        assertEquals(GrapheneFileSystemAccessMode.DENY, config.global().fileSystemAccessMode());
    }

    @Test
    void builderCanConfigureContainerHttp() {
        GrapheneHttpConfig httpConfig = GrapheneHttpConfig.builder()
                .randomPortInRange(20_000, 20_010)
                .build();
        GrapheneConfig config = GrapheneConfig.builder()
                .container(GrapheneContainerConfig.builder()
                        .http(httpConfig)
                        .build())
                .build();

        assertTrue(config.container().http().isPresent());
    }

    @Test
    void builderCanAllowGlobalFileSystemAccess() {
        GrapheneConfig config = GrapheneConfig.builder()
                .global(GrapheneGlobalConfig.builder()
                        .allowFileSystemAccess()
                        .build())
                .build();

        assertEquals(GrapheneFileSystemAccessMode.ALLOW, config.global().fileSystemAccessMode());
    }

    @Test
    void builderCanDefineGlobalJcefAndMultipleExtensionFolders() {
        GrapheneConfig config = GrapheneConfig.builder()
                .global(GrapheneGlobalConfig.builder()
                        .jcefDownloadPath("./custom-jcef")
                        .extensionFolder("./extensions/mod-a")
                        .extensionFolder(Path.of("./extensions/mod-b"))
                        .build())
                .build();

        assertEquals(Path.of("custom-jcef"), config.global().jcefDownloadPath().orElseThrow());
        assertEquals(2, config.global().extensionFolders().size());
    }
}
