package tytoo.grapheneui.internal.bridge;

final class GrapheneBridgeProtocol {
    static final String NAME = "graphene-ui";
    static final int VERSION = 1;
    static final String KIND_READY = "ready";
    static final String KIND_EVENT = "event";
    static final String KIND_REQUEST = "request";
    static final String KIND_RESPONSE = "response";
    static final String EMPTY_RESPONSE_JSON = "{}";

    private GrapheneBridgeProtocol() {
    }
}
