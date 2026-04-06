package tytoo.grapheneui.api;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.config.*;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.core.GrapheneCoreServices;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The core class of the Graphene library.
 * Each consumer mod must register once from its {@code onInitializeClient()} entrypoint, either
 * with an anchor class that belongs to the mod or with an explicit Fabric mod id.
 * Graphene closes registration before the first client tick and initializes lazily on first use,
 * or automatically after the Minecraft client startup has finished.
 */
public final class GrapheneCore implements ClientModInitializer {
    public static final String ID = "graphene-ui";
    private static final String ANCHOR_CLASS = "anchorClass";
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCore.class);
    private static final GrapheneCoreServices SERVICES = GrapheneCoreServices.get();
    private static final Map<Class<?>, String> RESOLVED_MOD_IDS_BY_ANCHOR_CLASS = new IdentityHashMap<>();
    private static final Map<String, GrapheneConfig> CONSUMER_CONFIGS = new LinkedHashMap<>();
    private static final Map<String, GrapheneMod> CONSUMERS = new LinkedHashMap<>();
    private static volatile boolean registrationClosed;

    public static synchronized GrapheneHandle register(Class<?> anchorClass) {
        return register(anchorClass, GrapheneConfig.defaults());
    }

    public static synchronized GrapheneHandle register(Class<?> anchorClass, GrapheneConfig config) {
        ensureRegistrationOpen();
        Class<?> validatedAnchorClass = Objects.requireNonNull(anchorClass, ANCHOR_CLASS);
        String modId = resolveModId(validatedAnchorClass);
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");
        return registerConsumer(modId, validatedConfig);
    }

    public static synchronized GrapheneHandle register(String modId) {
        return register(modId, GrapheneConfig.defaults());
    }

    public static synchronized GrapheneHandle register(String modId, GrapheneConfig config) {
        ensureRegistrationOpen();
        String validatedModId = validateRegisteredModId(modId);
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");
        return registerConsumer(validatedModId, validatedConfig);
    }

    public static synchronized GrapheneHandle handle(Class<?> anchorClass) {
        Class<?> validatedAnchorClass = Objects.requireNonNull(anchorClass, ANCHOR_CLASS);
        String modId = resolveModId(validatedAnchorClass);
        GrapheneMod consumer = CONSUMERS.get(modId);
        if (consumer != null) {
            return consumer;
        }

        throw new IllegalStateException(
                "No Graphene consumer registered for anchor class "
                        + validatedAnchorClass.getName()
                        + ". Call GrapheneCore.register(anchorClass, config) or GrapheneCore.register(modId, config)"
                        + " from onInitializeClient() before requesting its handle"
        );
    }

    public static synchronized GrapheneHandle handle(String modId) {
        String normalizedModId = normalizeModId(modId);
        GrapheneMod consumer = CONSUMERS.get(normalizedModId);
        if (consumer != null) {
            return consumer;
        }

        throw new IllegalStateException(
                "No Graphene consumer registered for mod id "
                        + normalizedModId
                        + ". Call GrapheneCore.register(modId, config) from onInitializeClient() before requesting its handle"
        );
    }

    public static synchronized GrapheneGlobalConfig globalConfig() {
        return mergeGlobalConfig();
    }

    public static synchronized boolean isInitialized() {
        return SERVICES.runtimeInternal().isInitialized();
    }

    public static void closeOwnedSurfaces(Object owner) {
        SERVICES.surfaceManager().closeOwner(Objects.requireNonNull(owner, "owner"));
    }

    public static GrapheneRuntime runtime() {
        synchronized (GrapheneCore.class) {
            ensureInitialized();
        }
        return SERVICES.runtime();
    }

    private static GrapheneHandle registerConsumer(String modId, GrapheneConfig config) {
        GrapheneMod existingConsumer = CONSUMERS.get(modId);
        if (existingConsumer != null) {
            GrapheneConfig existingConfig = CONSUMER_CONFIGS.get(modId);
            if (!Objects.equals(existingConfig, config)) {
                throw new IllegalStateException(
                        "Graphene consumer "
                                + modId
                                + " is already registered with a different config"
                );
            }

            return existingConsumer;
        }

        if (SERVICES.runtimeInternal().isInitialized()) {
            throw new IllegalStateException(
                    "Graphene runtime is already initialized; register all consumers before Graphene starts"
            );
        }

        GrapheneMod consumer = new GrapheneMod(modId, config);
        CONSUMERS.put(modId, consumer);
        CONSUMER_CONFIGS.put(modId, config);
        LOGGER.info("Registered Graphene consumer {}", modId);
        return consumer;
    }

    private static synchronized void startIfConsumersRegistered() {
        if (registrationClosed) {
            return;
        }

        registrationClosed = true;
        if (CONSUMERS.isEmpty()) {
            LOGGER.info("Graphene is loaded but no consumers are registered; skipping initialization.");
            return;
        }

        SERVICES.runtimeInternal().initializeAsync(mergeGlobalConfig(), snapshotContainerConfigs());
    }

    private static void ensureRegistrationOpen() {
        if (!registrationClosed) {
            return;
        }

        throw new IllegalStateException(
                "Graphene consumer registration is closed; register from onInitializeClient() before the first client tick"
        );
    }

    private static String resolveModId(Class<?> anchorClass) {
        String cachedModId = RESOLVED_MOD_IDS_BY_ANCHOR_CLASS.get(anchorClass);
        if (cachedModId != null) {
            return cachedModId;
        }

        String classFilePath = anchorClass.getName().replace('.', '/') + ".class";
        String resolvedModId = null;

        for (ModContainer modContainer : FabricLoader.getInstance().getAllMods()) {
            if (modContainer.findPath(classFilePath).isPresent()) {
                String candidateModId = normalizeModId(modContainer.getMetadata().getId());
                if (resolvedModId == null) {
                    resolvedModId = candidateModId;
                } else if (!resolvedModId.equals(candidateModId)) {
                    throw new IllegalStateException(
                            "Graphene anchor class "
                                    + anchorClass.getName()
                                    + " resolved to multiple mod containers: "
                                    + resolvedModId
                                    + " and "
                                    + candidateModId
                    );
                }
            }
        }

        if (resolvedModId == null) {
            throw new IllegalArgumentException(
                    "Failed to resolve Graphene consumer mod id for anchor class " + anchorClass.getName()
            );
        }

        RESOLVED_MOD_IDS_BY_ANCHOR_CLASS.put(anchorClass, resolvedModId);
        return resolvedModId;
    }

    private static String validateRegisteredModId(String modId) {
        String validatedModId = normalizeModId(modId);
        if (FabricLoader.getInstance().getModContainer(validatedModId).isEmpty()) {
            throw new IllegalArgumentException(
                    "No loaded Fabric mod with id " + validatedModId + " is available for Graphene registration"
            );
        }

        return validatedModId;
    }

    private static String normalizeModId(String modId) {
        String normalizedModId = Objects.requireNonNull(modId, "modId").trim();
        if (normalizedModId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }

        if (!Identifier.isValidNamespace(normalizedModId)) {
            throw new IllegalArgumentException(
                    "modId must be a valid namespace using lowercase letters, digits, '.', '_' or '-'"
            );
        }

        return normalizedModId;
    }

    private static void ensureInitialized() {
        if (SERVICES.runtimeInternal().isInitialized()) {
            return;
        }

        if (CONSUMERS.isEmpty()) {
            throw new IllegalStateException(
                    "No Graphene consumer registered. Call GrapheneCore.register(...) before Graphene is used"
            );
        }

        registrationClosed = true;
        SERVICES.runtimeInternal().initialize(mergeGlobalConfig(), snapshotContainerConfigs());
        LOGGER.info("Graphene initialized with {} registered consumer(s)", CONSUMERS.size());
    }

    private static Map<String, GrapheneContainerConfig> snapshotContainerConfigs() {
        LinkedHashMap<String, GrapheneContainerConfig> containerConfigs = new LinkedHashMap<>();
        for (Map.Entry<String, GrapheneConfig> consumerConfigEntry : CONSUMER_CONFIGS.entrySet()) {
            containerConfigs.put(consumerConfigEntry.getKey(), consumerConfigEntry.getValue().container());
        }

        return Map.copyOf(containerConfigs);
    }

    private static GrapheneGlobalConfig mergeGlobalConfig() {
        GrapheneGlobalConfig.Builder mergedConfigBuilder = GrapheneGlobalConfig.builder();
        OwnedValue<Path> selectedJcefPath = null;
        OwnedValue<GrapheneRemoteDebugConfig> selectedRemoteDebugConfig = null;
        GrapheneFileSystemAccessMode mergedFileSystemAccessMode = GrapheneFileSystemAccessMode.DENY;

        for (Map.Entry<String, GrapheneConfig> consumerConfigEntry : CONSUMER_CONFIGS.entrySet()) {
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

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(ignoredClient -> startIfConsumersRegistered());
    }

    private record OwnedValue<T>(T value, String owner) {
    }
}
