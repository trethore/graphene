package tytoo.grapheneui.api.bridge;

public final class GrapheneBridgeRequestException extends RuntimeException {
    private final String code;
    private final String requestId;
    private final String channel;

    public GrapheneBridgeRequestException(String code, String message, String requestId, String channel) {
        super(message == null ? "Bridge request failed" : message);
        this.code = code == null ? "bridge_error" : code;
        this.requestId = requestId;
        this.channel = channel;
    }

    public String getCode() {
        return code;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getChannel() {
        return channel;
    }
}
