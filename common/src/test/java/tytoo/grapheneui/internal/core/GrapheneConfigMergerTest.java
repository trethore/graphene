package tytoo.grapheneui.internal.core;

import org.junit.jupiter.api.Test;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneFileSystemAccessMode;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GrapheneConfigMergerTest {
    @Test
    void mergesPermissiveFileSystemAccess() {
        Map<String, GrapheneConfig> configs = new LinkedHashMap<>();
        configs.put("first", GrapheneConfig.defaults());
        configs.put("second", GrapheneConfig.builder()
                .global(GrapheneGlobalConfig.builder().allowFileSystemAccess().build())
                .build());

        GrapheneGlobalConfig mergedConfig = GrapheneConfigMerger.mergeGlobalConfig(configs);

        assertEquals(GrapheneFileSystemAccessMode.ALLOW, mergedConfig.fileSystemAccessMode());
    }

    @Test
    void rejectsConflictingJcefDownloadPaths() {
        Map<String, GrapheneConfig> configs = new LinkedHashMap<>();
        configs.put("first", configWithJcefPath("first"));
        configs.put("second", configWithJcefPath("second"));

        assertThrows(IllegalStateException.class, () -> GrapheneConfigMerger.mergeGlobalConfig(configs));
    }

    @Test
    void snapshotsContainerConfigsByConsumerId() {
        Map<String, GrapheneConfig> configs = Map.of("consumer", GrapheneConfig.defaults());

        assertEquals(
                GrapheneConfig.defaults().container(),
                GrapheneConfigMerger.snapshotContainerConfigs(configs).get("consumer")
        );
    }

    private static GrapheneConfig configWithJcefPath(String path) {
        return GrapheneConfig.builder()
                .global(GrapheneGlobalConfig.builder().jcefDownloadPath(Path.of(path)).build())
                .build();
    }
}
