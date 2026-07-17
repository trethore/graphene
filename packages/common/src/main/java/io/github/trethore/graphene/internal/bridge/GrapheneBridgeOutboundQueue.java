package io.github.trethore.graphene.internal.bridge;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GrapheneBridgeOutboundQueue {
  private static final Logger LOGGER = LoggerFactory.getLogger(GrapheneBridgeOutboundQueue.class);

  private final Object lock = new Object();
  private final ArrayDeque<String> queuedMessages = new ArrayDeque<>();
  private final Consumer<String> dispatcher;
  private final int maxQueuedMessages;
  private State state = State.NOT_READY;

  GrapheneBridgeOutboundQueue(Consumer<String> dispatcher, int maxQueuedMessages) {
    this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    if (maxQueuedMessages < 1) {
      throw new IllegalArgumentException("maxQueuedMessages must be >= 1");
    }
    this.maxQueuedMessages = maxQueuedMessages;
  }

  boolean isReady() {
    synchronized (lock) {
      return state == State.READY;
    }
  }

  void markNotReady() {
    synchronized (lock) {
      state = State.NOT_READY;
      LOGGER.debug("Bridge outbound queue marked NOT_READY queued={}", queuedMessages.size());
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
          LOGGER.warn("Failed to dispatch queued Graphene bridge message", exception);
        }
      }

      LOGGER.debug("Flushed {} queued bridge outbound message(s)", messagesToDispatch.size());
    }
  }

  void queueOrDispatch(String outboundPacketJson) {
    Objects.requireNonNull(outboundPacketJson, "outboundPacketJson");

    synchronized (lock) {
      if (state != State.READY) {
        queueMessageLocked(outboundPacketJson);
        LOGGER.debug(
            "Queued bridge outbound message size={} queued={}",
            outboundPacketJson.length(),
            queuedMessages.size());
        return;
      }

      try {
        dispatcher.accept(outboundPacketJson);
        LOGGER.debug(
            "Dispatched bridge outbound message immediately size={}", outboundPacketJson.length());
      } catch (RuntimeException exception) {
        LOGGER.warn("Failed to dispatch immediate Graphene bridge message", exception);
      }
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

    String droppedMessage = queuedMessages.removeFirst();
    queuedMessages.addLast(outboundPacketJson);
    LOGGER.debug(
        "Dropped oldest bridge outbound message droppedSize={} newSize={} maxQueued={}",
        droppedMessage.length(),
        outboundPacketJson.length(),
        maxQueuedMessages);
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
