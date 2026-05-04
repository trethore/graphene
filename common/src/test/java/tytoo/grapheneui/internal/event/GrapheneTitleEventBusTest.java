package tytoo.grapheneui.internal.event;

import org.junit.jupiter.api.Test;
import tytoo.grapheneui.api.surface.GrapheneTitleListener;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneTitleEventBusTest {
    @Test
    void dispatchesTitleChangesToRegisteredListeners() {
        GrapheneTitleEventBus eventBus = new GrapheneTitleEventBus();
        AtomicInteger callCount = new AtomicInteger();
        AtomicReference<String> receivedTitle = new AtomicReference<>();
        GrapheneTitleListener listener = (browser, title) -> {
            callCount.incrementAndGet();
            receivedTitle.set(title);
        };

        eventBus.register(listener);
        eventBus.onTitleChange(null, "Graphene UI");

        assertEquals(1, callCount.get());
        assertEquals("Graphene UI", receivedTitle.get());
    }

    @Test
    void clearRemovesRegisteredListeners() {
        GrapheneTitleEventBus eventBus = new GrapheneTitleEventBus();
        AtomicInteger callCount = new AtomicInteger();

        eventBus.register((browser, title) -> callCount.incrementAndGet());
        eventBus.clear();
        eventBus.onTitleChange(null, "ignored");

        assertEquals(0, callCount.get());
    }
}
