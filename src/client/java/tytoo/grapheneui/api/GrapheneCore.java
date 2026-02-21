package tytoo.grapheneui.api;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.core.GrapheneCoreServices;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The core class of the Graphene library.
 * Each consumer mod should register once with {@link #register(String)} or {@link #register(String, GrapheneConfig)}.
 * Runtime startup happens after client startup has finished.
 */
public final class GrapheneCore implements ClientModInitializer {
    public static final String ID = "graphene-ui";
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCore.class);
    private static final GrapheneCoreServices SERVICES = GrapheneCoreServices.get();
    private static final Map<String, GrapheneConfig> CONSUMER_CONFIGS = new LinkedHashMap<>();
    private static final Map<String, GrapheneMod> CONSUMERS = new LinkedHashMap<>();
    private static final String MOD_ID_NAME = "modId";

    public static synchronized GrapheneMod register(String modId) {
        return register(modId, GrapheneConfig.defaults());
    }

    public static synchronized GrapheneMod register(String modId, GrapheneConfig config) {
        String normalizedModId = normalizeModId(modId);
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");

        GrapheneMod existingConsumer = CONSUMERS.get(normalizedModId);
        if (existingConsumer != null) {
            GrapheneConfig existingConfig = CONSUMER_CONFIGS.get(normalizedModId);
            if (!Objects.equals(existingConfig, validatedConfig)) {
                throw new IllegalStateException(
                        "Graphene consumer "
                                + normalizedModId
                                + " is already registered with a different config"
                );
            }

            return existingConsumer;
        }

        if (SERVICES.runtimeInternal().isInitialized()) {
            throw new IllegalStateException(
                    "Graphene runtime is already initialized; register all consumers before first Graphene usage"
            );
        }

        GrapheneMod consumer = new GrapheneMod(normalizedModId);
        CONSUMERS.put(normalizedModId, consumer);
        CONSUMER_CONFIGS.put(normalizedModId, validatedConfig);
        LOGGER.info("Registered Graphene consumer {}", normalizedModId);
        return consumer;
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

    static synchronized void start() {
        ensureInitialized();
    }

    private static String normalizeModId(String modId) {
        String normalizedModId = Objects.requireNonNull(modId, MOD_ID_NAME).trim();
        if (normalizedModId.isBlank()) {
            throw new IllegalArgumentException(MOD_ID_NAME + " must not be blank");
        }

        if (!Identifier.isValidNamespace(normalizedModId)) {
            throw new IllegalArgumentException(
                    MOD_ID_NAME + " must be a valid namespace using lowercase letters, digits, '.', '_' or '-'"
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
                    "No Graphene consumer registered. Call GrapheneCore.register(modId, config) before Graphene is used"
            );
        }

        GrapheneConfig mergedConfig = mergeSharedConfig();
        SERVICES.runtimeInternal().initialize(mergedConfig);
        LOGGER.info("Graphene initialized with {} registered consumer(s)", CONSUMERS.size());
    }

    private static GrapheneConfig mergeSharedConfig() {
        GrapheneConfig.Builder mergedConfigBuilder = GrapheneConfig.builder();
        OwnedValue<Path> selectedJcefPath = null;
        OwnedValue<GrapheneHttpConfig> selectedHttpConfig = null;

        for (Map.Entry<String, GrapheneConfig> consumerConfigEntry : CONSUMER_CONFIGS.entrySet()) {
            String consumerId = consumerConfigEntry.getKey();
            GrapheneConfig consumerConfig = consumerConfigEntry.getValue();

            selectedJcefPath = mergeOwnedValue(
                    selectedJcefPath,
                    normalizeConfiguredJcefPath(consumerConfig),
                    consumerId,
                    "jcefDownloadPath"
            );
            mergeExtensionFolders(mergedConfigBuilder, consumerConfig);
            selectedHttpConfig = mergeOwnedValue(
                    selectedHttpConfig,
                    consumerConfig.http().orElse(null),
                    consumerId,
                    "HTTP config"
            );
        }

        if (selectedJcefPath != null) {
            mergedConfigBuilder.jcefDownloadPath(selectedJcefPath.value());
        }

        if (selectedHttpConfig != null) {
            mergedConfigBuilder.http(selectedHttpConfig.value());
        }

        return mergedConfigBuilder.build();
    }

    private static Path normalizeConfiguredJcefPath(GrapheneConfig consumerConfig) {
        return consumerConfig.jcefDownloadPath()
                .map(path -> path.toAbsolutePath().normalize())
                .orElse(null);
    }

    private static void mergeExtensionFolders(GrapheneConfig.Builder mergedConfigBuilder, GrapheneConfig consumerConfig) {
        for (Path extensionFolder : consumerConfig.extensionFolders()) {
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

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(ignoredClient -> start());
    }

    private record OwnedValue<T>(T value, String owner) {
    }
}
