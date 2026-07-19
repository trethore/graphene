package io.github.trethore.graphene.internal.bridge;

import java.util.concurrent.TimeUnit;

final class GrapheneBridgeClipboardAccess {
  private static final String WRITE_CHANNEL = "graphene:clipboard:write";
  private static final long AUTHORIZATION_NANOS = TimeUnit.SECONDS.toNanos(3);
  private static final int MAX_AUTHORIZED_MESSAGES = 2;

  private long authorizedDocumentGeneration = -1L;
  private long authorizationDeadlineNanos;
  private int remainingAuthorizedMessages;

  synchronized void authorize(long documentGeneration) {
    authorizedDocumentGeneration = documentGeneration;
    authorizationDeadlineNanos = System.nanoTime() + AUTHORIZATION_NANOS;
    remainingAuthorizedMessages = MAX_AUTHORIZED_MESSAGES;
  }

  synchronized boolean allows(
      BridgeFrame frame, GrapheneBridgePacket packet, long documentGeneration) {
    if (!frame.mainFrame()
        || packet == null
        || !GrapheneBridgeProtocol.KIND_EVENT.equals(packet.kind)
        || !WRITE_CHANNEL.equals(packet.channel)
        || authorizedDocumentGeneration != documentGeneration
        || System.nanoTime() > authorizationDeadlineNanos
        || remainingAuthorizedMessages <= 0) {
      return false;
    }

    remainingAuthorizedMessages--;
    return true;
  }
}
