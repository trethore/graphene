package tytoo.grapheneui.internal.browser.input;

import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.internal.input.mouse.GrapheneMouseButtonUtil;

import java.util.Objects;

public final class GrapheneExtraMouseButtonEmitter {
    private static final String MOUSE_EXTRA_BUTTON_CHANNEL = "graphene:mouse:extra-button";
    private static final String MOUSE_EXTRA_RESET_CHANNEL = "graphene:mouse:extra-reset";

    private final GrapheneBridge bridge;

    public GrapheneExtraMouseButtonEmitter(GrapheneBridge bridge) {
        this.bridge = Objects.requireNonNull(bridge, "bridge");
    }

    public void emit(int button, boolean pressed) {
        if (!GrapheneMouseButtonUtil.isExtraMouseButton(button)) {
            return;
        }

        try {
            bridge.emit(MOUSE_EXTRA_BUTTON_CHANNEL, payload(button, pressed));
        } catch (IllegalStateException ignored) {
            // Ignore events while the bridge is shutting down.
        }
    }

    public void reset() {
        try {
            bridge.emit(MOUSE_EXTRA_RESET_CHANNEL, "{}");
        } catch (IllegalStateException ignored) {
            // Ignore events while the bridge is shutting down.
        }
    }

    private String payload(int button, boolean pressed) {
        return "{\"button\":" + button + ",\"pressed\":" + pressed + "}";
    }
}
