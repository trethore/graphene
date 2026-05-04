package tytoo.grapheneui.internal.core;

import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneFileSystemAccessMode;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.config.GrapheneRemoteDebugConfig;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class GrapheneConfigMerger {
    private GrapheneConfigMerger() {
    }

    public static Map<String, GrapheneContainerConfig> snapshotContainerConfigs(Map<String, GrapheneConfig> consumerConfigs) {
        LinkedHashMap<String, GrapheneContainerConfig> containerConfigs = new LinkedHashMap<>();
        for (Map.Entry<String, GrapheneConfig> consumerConfigEntry : consumerConfigs.entrySet()) {
            containerConfigs.put(consumerConfigEntry.getKey(), consumerConfigEntry.getValue().container());
        }

        return Map.copyOf(containerConfigs);
    }

    public static GrapheneGlobalConfig mergeGlobalConfig(Map<String, GrapheneConfig> consumerConfigs) {
        GrapheneGlobalConfig.Builder mergedConfigBuilder = GrapheneGlobalConfig.builder();
        OwnedValue<Path> selectedJcefPath = null;
        OwnedValue<GrapheneRemoteDebugConfig> selectedRemoteDebugConfig = null;
        GrapheneFileSystemAccessMode mergedFileSystemAccessMode = GrapheneFileSystemAccessMode.DENY;

        for (Map.Entry<String, GrapheneConfig> consumerConfigEntry : consumerConfigs.entrySet()) {
            String consumerId = consumerConfigEntry.getKey();
            GrapheneGlobalConfig consumerGlobalConfig = consumerConfigEntry.getValue().global();

            selectedJcefPath = mergeOwnedValue(
                    selectedJcefPath,
                    normalizeConfiguredJcefPath(consumerGlobalConfig),
                    consumerId,
                    "jcefDownloadPath"
            );
            mergeExtensionFolders(mergedConfigBuilder, consumerGlobalConfig);
            selectedRemoteDebugConfig = mergeOwnedValue(
                    selectedRemoteDebugConfig,
                    consumerGlobalConfig.remoteDebugging().orElse(null),
                    consumerId,
                    "remote debugging config"
            );
            mergedFileSystemAccessMode = mergeFileSystemAccessMode(
                    mergedFileSystemAccessMode,
                    consumerGlobalConfig.fileSystemAccessMode()
            );
        }

        if (selectedJcefPath != null) {
            mergedConfigBuilder.jcefDownloadPath(selectedJcefPath.value());
        }

        if (selectedRemoteDebugConfig != null) {
            mergedConfigBuilder.remoteDebugging(selectedRemoteDebugConfig.value());
        }

        mergedConfigBuilder.fileSystemAccessMode(mergedFileSystemAccessMode);

        return mergedConfigBuilder.build();
    }

    private static Path normalizeConfiguredJcefPath(GrapheneGlobalConfig consumerGlobalConfig) {
        return consumerGlobalConfig.jcefDownloadPath()
                .map(path -> path.toAbsolutePath().normalize())
                .orElse(null);
    }

    private static void mergeExtensionFolders(
            GrapheneGlobalConfig.Builder mergedConfigBuilder,
            GrapheneGlobalConfig consumerGlobalConfig
    ) {
        for (Path extensionFolder : consumerGlobalConfig.extensionFolders()) {
            mergedConfigBuilder.extensionFolder(extensionFolder);
        }
    }

    private static <T> OwnedValue<T> mergeOwnedValue(
            OwnedValue<T> selectedValue,
            T candidateValue,
            String candidateOwner,
            String settingName
    ) {
        if (candidateValue == null) {
            return selectedValue;
        }

        if (selectedValue == null) {
            return new OwnedValue<>(candidateValue, candidateOwner);
        }

        if (Objects.equals(selectedValue.value(), candidateValue)) {
            return selectedValue;
        }

        throw new IllegalStateException(
                "Conflicting Graphene "
                        + settingName
                        + " between consumers "
                        + selectedValue.owner()
                        + " and "
                        + candidateOwner
        );
    }

    private static GrapheneFileSystemAccessMode mergeFileSystemAccessMode(
            GrapheneFileSystemAccessMode mergedMode,
            GrapheneFileSystemAccessMode candidateMode
    ) {
        if (mergedMode == GrapheneFileSystemAccessMode.ALLOW || candidateMode == GrapheneFileSystemAccessMode.ALLOW) {
            return GrapheneFileSystemAccessMode.ALLOW;
        }

        return GrapheneFileSystemAccessMode.DENY;
    }

    private record OwnedValue<T>(T value, String owner) {
    }
}
