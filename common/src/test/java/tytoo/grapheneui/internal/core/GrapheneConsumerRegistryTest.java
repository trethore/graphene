package tytoo.grapheneui.internal.core;

import org.junit.jupiter.api.Test;
import tytoo.grapheneui.api.config.GrapheneConfig;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrapheneConsumerRegistryTest {
    @Test
    void registersConsumer() {
        GrapheneConsumerRegistry<TestConsumer> registry = createRegistry(false);

        GrapheneConsumerRegistry.Registration<TestConsumer> registration = registry.register("consumer", GrapheneConfig.defaults());

        assertTrue(registration.created());
        assertEquals("consumer", registration.consumer().id());
        assertSame(registration.consumer(), registry.get("consumer"));
    }

    @Test
    void returnsExistingConsumerForSameConfig() {
        GrapheneConsumerRegistry<TestConsumer> registry = createRegistry(false);
        TestConsumer firstConsumer = registry.register("consumer", GrapheneConfig.defaults()).consumer();

        GrapheneConsumerRegistry.Registration<TestConsumer> secondRegistration = registry.register("consumer", GrapheneConfig.defaults());

        assertFalse(secondRegistration.created());
        assertSame(firstConsumer, secondRegistration.consumer());
    }

    @Test
    void rejectsRegistrationAfterClose() {
        GrapheneConsumerRegistry<TestConsumer> registry = createRegistry(false);
        registry.closeRegistration();

        assertThrows(IllegalStateException.class, () -> registry.register("consumer", GrapheneConfig.defaults()));
    }

    @Test
    void rejectsRegistrationAfterRuntimeInitialized() {
        GrapheneConsumerRegistry<TestConsumer> registry = createRegistry(true);

        assertThrows(IllegalStateException.class, () -> registry.register("consumer", GrapheneConfig.defaults()));
    }

    private static GrapheneConsumerRegistry<TestConsumer> createRegistry(boolean runtimeInitialized) {
        AtomicBoolean initialized = new AtomicBoolean(runtimeInitialized);
        return new GrapheneConsumerRegistry<>(initialized::get, TestConsumer::new);
    }

    private record TestConsumer(String id, GrapheneConfig config) {
    }
}
