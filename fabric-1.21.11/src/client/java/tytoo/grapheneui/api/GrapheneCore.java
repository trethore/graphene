package tytoo.grapheneui.api;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;
import tytoo.grapheneui.api.runtime.GrapheneRuntime;
import tytoo.grapheneui.internal.core.GrapheneConsumerRegistry;
import tytoo.grapheneui.internal.core.GrapheneCoreServices;
import tytoo.grapheneui.internal.core.GrapheneNamespaceValidator;
import tytoo.grapheneui.internal.url.GrapheneHttpUrls;

import java.util.IdentityHashMap;
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
    public static final String ID = GrapheneConstants.ID;
    private static final String ANCHOR_CLASS = "anchorClass";
    private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneCore.class);
    private static final GrapheneCoreServices SERVICES = GrapheneCoreServices.get();
    private static final Map<Class<?>, String> RESOLVED_MOD_IDS_BY_ANCHOR_CLASS = new IdentityHashMap<>();
    private static final GrapheneConsumerRegistry<GrapheneMod> CONSUMERS = new GrapheneConsumerRegistry<>(
            () -> SERVICES.runtimeInternal().isInitialized(),
            GrapheneMod::new
    );

    static {
        GrapheneHttpUrls.configureHttpServerSupplier(() -> GrapheneCore.runtime().httpServer());
    }

    public static synchronized GrapheneHandle register(Class<?> anchorClass) {
        return register(anchorClass, GrapheneConfig.defaults());
    }

    public static synchronized GrapheneHandle register(Class<?> anchorClass, GrapheneConfig config) {
        CONSUMERS.ensureRegistrationOpen();
        Class<?> validatedAnchorClass = Objects.requireNonNull(anchorClass, ANCHOR_CLASS);
        String modId = resolveModId(validatedAnchorClass);
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");
        return registerConsumer(modId, validatedConfig);
    }

    public static synchronized GrapheneHandle register(String modId) {
        return register(modId, GrapheneConfig.defaults());
    }

    public static synchronized GrapheneHandle register(String modId, GrapheneConfig config) {
        CONSUMERS.ensureRegistrationOpen();
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
        return CONSUMERS.mergeGlobalConfig();
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
        GrapheneConsumerRegistry.Registration<GrapheneMod> registration = CONSUMERS.register(modId, config);
        if (registration.created()) {
            LOGGER.info("Registered Graphene consumer {}", modId);
        }

        return registration.consumer();
    }

    private static synchronized void startIfConsumersRegistered() {
        if (!CONSUMERS.closeRegistration()) {
            return;
        }

        if (CONSUMERS.isEmpty()) {
            LOGGER.info("Graphene is loaded but no consumers are registered; skipping initialization.");
            return;
        }

        SERVICES.runtimeInternal().initializeAsync(CONSUMERS.mergeGlobalConfig(), CONSUMERS.snapshotContainerConfigs());
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
        return GrapheneNamespaceValidator.normalizeNamespace(modId, "modId");
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

        CONSUMERS.closeRegistration();
        SERVICES.runtimeInternal().initialize(CONSUMERS.mergeGlobalConfig(), CONSUMERS.snapshotContainerConfigs());
        LOGGER.info("Graphene initialized with {} registered consumer(s)", CONSUMERS.size());
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(ignoredClient -> startIfConsumersRegistered());
    }
}
