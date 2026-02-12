package tytoo.grapheneui.internal.bridge;

@FunctionalInterface
public interface GrapheneBridgeDiagnostics {
    GrapheneBridgeDiagnostics NO_OP = (outboundPacketJson, overflowPolicy, maxQueuedMessages) -> {
    };

    static GrapheneBridgeDiagnostics noOp() {
        return NO_OP;
    }

    void onOutboundMessageDropped(String outboundPacketJson, GrapheneBridgeQueueOverflowPolicy overflowPolicy, int maxQueuedMessages);

    default void onRequestHandlerReplaced(String channel) {
    }
}
