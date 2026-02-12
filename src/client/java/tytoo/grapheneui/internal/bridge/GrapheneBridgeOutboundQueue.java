package tytoo.grapheneui.internal.bridge;

import tytoo.grapheneui.api.GrapheneCore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class GrapheneBridgeOutboundQueue {
    private final Object lock = new Object();
    private final ArrayDeque<String> queuedMessages = new ArrayDeque<>();
    private final Consumer<String> dispatcher;
    private final int maxQueuedMessages;
    private final GrapheneBridgeQueueOverflowPolicy overflowPolicy;
    private final GrapheneBridgeDiagnostics diagnostics;
    private State state = State.NOT_READY;
    GrapheneBridgeOutboundQueue(
            Consumer<String> dispatcher,
            int maxQueuedMessages,
            GrapheneBridgeQueueOverflowPolicy overflowPolicy,
            GrapheneBridgeDiagnostics diagnostics
    ) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        if (maxQueuedMessages < 1) {
            throw new IllegalArgumentException("maxQueuedMessages must be >= 1");
        }
        this.maxQueuedMessages = maxQueuedMessages;
        this.overflowPolicy = Objects.requireNonNull(overflowPolicy, "overflowPolicy");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    boolean isReady() {
        synchronized (lock) {
            return state == State.READY;
        }
    }

    void markNotReady() {
        synchronized (lock) {
            state = State.NOT_READY;
        }
    }

    void markReadyAndFlush() {
        while (true) {
            List<String> messagesToDispatch;
            synchronized (lock) {
                if (state == State.READY) {
                    return;
                }

                state = State.FLUSHING;
                if (queuedMessages.isEmpty()) {
                    state = State.READY;
                    return;
                }

                messagesToDispatch = drainQueuedMessagesLocked();
            }

            for (String message : messagesToDispatch) {
                try {
                    dispatcher.accept(message);
                } catch (RuntimeException exception) {
                    GrapheneCore.LOGGER.warn("Failed to dispatch queued Graphene bridge message", exception);
                }
            }
        }
    }

    void queueOrDispatch(String outboundPacketJson) {
        Objects.requireNonNull(outboundPacketJson, "outboundPacketJson");

        synchronized (lock) {
            if (state == State.READY) {
                dispatcher.accept(outboundPacketJson);
                return;
            }

            queueMessageLocked(outboundPacketJson);
        }
    }

    void clear() {
        synchronized (lock) {
            queuedMessages.clear();
        }
    }

    private void queueMessageLocked(String outboundPacketJson) {
        if (queuedMessages.size() < maxQueuedMessages) {
            queuedMessages.addLast(outboundPacketJson);
            return;
        }

        if (overflowPolicy == GrapheneBridgeQueueOverflowPolicy.DROP_NEWEST) {
            diagnostics.onOutboundMessageDropped(outboundPacketJson, overflowPolicy, maxQueuedMessages);
            return;
        }

        if (overflowPolicy == GrapheneBridgeQueueOverflowPolicy.DROP_OLDEST) {
            String droppedMessage = queuedMessages.removeFirst();
            queuedMessages.addLast(outboundPacketJson);
            diagnostics.onOutboundMessageDropped(droppedMessage, overflowPolicy, maxQueuedMessages);
            return;
        }

        throw new IllegalStateException("Bridge outbound queue reached max size " + maxQueuedMessages);
    }

    private List<String> drainQueuedMessagesLocked() {
        List<String> messages = new ArrayList<>(queuedMessages.size());
        while (!queuedMessages.isEmpty()) {
            messages.add(queuedMessages.removeFirst());
        }

        return messages;
    }

    private enum State {
        NOT_READY,
        FLUSHING,
        READY
    }
}
