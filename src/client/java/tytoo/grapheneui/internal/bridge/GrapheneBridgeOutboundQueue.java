package tytoo.grapheneui.internal.bridge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

final class GrapheneBridgeOutboundQueue {
    private final Object lock = new Object();
    private final ArrayDeque<String> queuedMessages = new ArrayDeque<>();
    private final AtomicBoolean ready = new AtomicBoolean(false);
    private final Consumer<String> dispatcher;

    GrapheneBridgeOutboundQueue(Consumer<String> dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    boolean isReady() {
        return ready.get();
    }

    void markNotReady() {
        ready.set(false);
    }

    void markReadyAndFlush() {
        ready.set(true);
        List<String> messagesToDispatch = drainQueuedMessages();
        for (String message : messagesToDispatch) {
            dispatcher.accept(message);
        }
    }

    void queueOrDispatch(String outboundPacketJson) {
        if (ready.get()) {
            dispatcher.accept(outboundPacketJson);
            return;
        }

        synchronized (lock) {
            if (ready.get()) {
                dispatcher.accept(outboundPacketJson);
                return;
            }

            queuedMessages.addLast(outboundPacketJson);
        }
    }

    void clear() {
        synchronized (lock) {
            queuedMessages.clear();
        }
    }

    private List<String> drainQueuedMessages() {
        synchronized (lock) {
            List<String> messages = new ArrayList<>(queuedMessages.size());
            while (!queuedMessages.isEmpty()) {
                messages.add(queuedMessages.removeFirst());
            }

            return messages;
        }
    }
}
