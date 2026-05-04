package tytoo.grapheneui.internal.core;

import tytoo.grapheneui.api.config.GrapheneConfig;
import tytoo.grapheneui.api.config.GrapheneContainerConfig;
import tytoo.grapheneui.api.config.GrapheneGlobalConfig;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

public final class GrapheneConsumerRegistry<T> {
    private final BooleanSupplier runtimeInitialized;
    private final BiFunction<String, GrapheneConfig, T> consumerFactory;
    private final Map<String, GrapheneConfig> consumerConfigs = new LinkedHashMap<>();
    private final Map<String, T> consumers = new LinkedHashMap<>();
    private boolean registrationClosed;

    public GrapheneConsumerRegistry(
            BooleanSupplier runtimeInitialized,
            BiFunction<String, GrapheneConfig, T> consumerFactory
    ) {
        this.runtimeInitialized = Objects.requireNonNull(runtimeInitialized, "runtimeInitialized");
        this.consumerFactory = Objects.requireNonNull(consumerFactory, "consumerFactory");
    }

    public synchronized Registration<T> register(String consumerId, GrapheneConfig config) {
        ensureRegistrationOpen();
        String normalizedConsumerId = GrapheneNamespaceValidator.normalizeNamespace(consumerId, "consumerId");
        GrapheneConfig validatedConfig = Objects.requireNonNull(config, "config");

        T existingConsumer = consumers.get(normalizedConsumerId);
        if (existingConsumer != null) {
            GrapheneConfig existingConfig = consumerConfigs.get(normalizedConsumerId);
            if (!Objects.equals(existingConfig, validatedConfig)) {
                throw new IllegalStateException(
                        "Graphene consumer "
                                + normalizedConsumerId
                                + " is already registered with a different config"
                );
            }

            return new Registration<>(existingConsumer, false);
        }

        if (runtimeInitialized.getAsBoolean()) {
            throw new IllegalStateException(
                    "Graphene runtime is already initialized; register all consumers before Graphene starts"
            );
        }

        T consumer = consumerFactory.apply(normalizedConsumerId, validatedConfig);
        consumers.put(normalizedConsumerId, consumer);
        consumerConfigs.put(normalizedConsumerId, validatedConfig);
        return new Registration<>(consumer, true);
    }

    public synchronized T get(String consumerId) {
        String normalizedConsumerId = GrapheneNamespaceValidator.normalizeNamespace(consumerId, "consumerId");
        return consumers.get(normalizedConsumerId);
    }

    public synchronized boolean isEmpty() {
        return consumers.isEmpty();
    }

    public synchronized int size() {
        return consumers.size();
    }

    public synchronized boolean closeRegistration() {
        if (registrationClosed) {
            return false;
        }

        registrationClosed = true;
        return true;
    }

    public synchronized void ensureRegistrationOpen() {
        if (!registrationClosed) {
            return;
        }

        throw new IllegalStateException(
                "Graphene consumer registration is closed; register from onInitializeClient() before the first client tick"
        );
    }

    public synchronized GrapheneGlobalConfig mergeGlobalConfig() {
        return GrapheneConfigMerger.mergeGlobalConfig(consumerConfigs);
    }

    public synchronized Map<String, GrapheneContainerConfig> snapshotContainerConfigs() {
        return GrapheneConfigMerger.snapshotContainerConfigs(consumerConfigs);
    }

    public record Registration<T>(T consumer, boolean created) {
    }
}
