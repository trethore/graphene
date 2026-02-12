package tytoo.grapheneui.internal.bridge;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneBridgeOutboundQueueTest {
    @Test
    void queuesBeforeReadyAndFlushesInOrder() {
        List<String> dispatchedMessages = new ArrayList<>();
        GrapheneBridgeOutboundQueue queue = new GrapheneBridgeOutboundQueue(dispatchedMessages::add);

        queue.queueOrDispatch("first");
        queue.queueOrDispatch("second");
        assertTrue(dispatchedMessages.isEmpty());

        queue.markReadyAndFlush();
        assertEquals(List.of("first", "second"), dispatchedMessages);

        queue.queueOrDispatch("third");
        assertEquals(List.of("first", "second", "third"), dispatchedMessages);
    }

    @Test
    void clearDropsQueuedMessages() {
        List<String> dispatchedMessages = new ArrayList<>();
        GrapheneBridgeOutboundQueue queue = new GrapheneBridgeOutboundQueue(dispatchedMessages::add);

        queue.queueOrDispatch("queued");
        queue.clear();
        queue.markReadyAndFlush();

        assertTrue(dispatchedMessages.isEmpty());
    }
}
