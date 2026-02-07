package tytoo.grapheneui.bridge;

@FunctionalInterface
public interface GrapheneBridgeEventListener {
    void onEvent(String channel, String payloadJson);
}
