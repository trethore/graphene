package io.github.trethore.graphene.internal.bridge;

import io.github.trethore.graphene.api.GrapheneSubscription;
import io.github.trethore.graphene.api.bridge.GrapheneBridge;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeEventListener;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeJson;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeJsonEventListener;
import io.github.trethore.graphene.api.bridge.GrapheneBridgeRequestHandler;
import java.util.Objects;

public final class GrapheneBridgeInternals {
  private GrapheneBridgeInternals() {}

  public static GrapheneSubscription onEvent(
      GrapheneBridge bridge, String channel, GrapheneBridgeEventListener listener) {
    return endpoint(bridge).onInternalEvent(channel, listener);
  }

  public static <T> GrapheneSubscription onEventJson(
      GrapheneBridge bridge,
      String channel,
      Class<T> payloadType,
      GrapheneBridgeJsonEventListener<T> listener) {
    Objects.requireNonNull(payloadType, "payloadType");
    Objects.requireNonNull(listener, "listener");
    return onEvent(
        bridge,
        channel,
        (receivedChannel, payloadJson) ->
            listener.onEvent(
                receivedChannel, GrapheneBridgeJson.fromJson(payloadJson, payloadType)));
  }

  public static GrapheneSubscription onRequest(
      GrapheneBridge bridge, String channel, GrapheneBridgeRequestHandler handler) {
    return endpoint(bridge).onInternalRequest(channel, handler);
  }

  public static void emitJson(GrapheneBridge bridge, String channel, Object payload) {
    endpoint(bridge).emitInternal(channel, GrapheneBridgeJson.toJson(payload));
  }

  private static GrapheneBridgeEndpoint endpoint(GrapheneBridge bridge) {
    if (Objects.requireNonNull(bridge, "bridge") instanceof GrapheneBridgeEndpoint endpoint) {
      return endpoint;
    }
    throw new IllegalArgumentException("Unsupported Graphene bridge implementation");
  }
}
