package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class GrapheneCefFileDialogRoutingTest {
    @Test
    void consumesArmedIntentOnlyOnce() {
        AtomicLong nanoTime = new AtomicLong(100L);
        try (GrapheneCefFileDialogRouting routing = new GrapheneCefFileDialogRouting(nanoTime::get)) {
            routing.armDirectoryIntent();

            assertTrue(routing.consumeDirectoryIntent());
            assertFalse(routing.consumeDirectoryIntent());
        }
    }

    @Test
    void rejectsExpiredIntent() {
        AtomicLong nanoTime = new AtomicLong(100L);
        try (GrapheneCefFileDialogRouting routing = new GrapheneCefFileDialogRouting(nanoTime::get)) {
            routing.armDirectoryIntent();

            nanoTime.addAndGet(2_000_000_001L);

            assertFalse(routing.consumeDirectoryIntent());
        }
    }
}
