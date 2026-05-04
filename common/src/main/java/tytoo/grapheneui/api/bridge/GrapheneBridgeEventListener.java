package tytoo.grapheneui.api.bridge;

@FunctionalInterface
public interface GrapheneBridgeEventListener {
    void onEvent(String channel, String payloadJson);
}
