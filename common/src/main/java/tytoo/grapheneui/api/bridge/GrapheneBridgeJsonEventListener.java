package tytoo.grapheneui.api.bridge;

@FunctionalInterface
public interface GrapheneBridgeJsonEventListener<T> {
    void onEvent(String channel, T payload);
}
