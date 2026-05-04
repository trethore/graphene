package tytoo.grapheneui.internal.browser.input.devtools;

import com.google.gson.JsonObject;
import tytoo.grapheneui.internal.input.GrapheneInputModifierUtil;
import tytoo.grapheneui.internal.input.mouse.GrapheneMouseButtonUtil;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class GrapheneDomMouseDispatcher {
    private static final String DEVTOOLS_METHOD_DISPATCH_MOUSE_EVENT = "Input.dispatchMouseEvent";
    private static final String MOUSE_EVENT_TYPE_PRESSED = "mousePressed";
    private static final String MOUSE_EVENT_TYPE_RELEASED = "mouseReleased";
    private static final String PROPERTY_TYPE = "type";
    private static final String PROPERTY_X = "x";
    private static final String PROPERTY_Y = "y";
    private static final String PROPERTY_MODIFIERS = "modifiers";
    private static final String PROPERTY_BUTTON = "button";
    private static final String PROPERTY_BUTTONS = "buttons";
    private static final String PROPERTY_CLICK_COUNT = "clickCount";
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneDomMouseDispatcher.class);

    private final BiConsumer<String, JsonObject> devToolsMethodExecutor;

    public GrapheneDomMouseDispatcher(BiConsumer<String, JsonObject> devToolsMethodExecutor) {
        this.devToolsMethodExecutor = Objects.requireNonNull(devToolsMethodExecutor, "devToolsMethodExecutor");
    }

    public static GrapheneDebugLogger logger() {
        return DEBUG_LOGGER;
    }

    public void navigationButtonInteracted(int x, int y, int modifiers, int button, boolean pressed, int clickCount, int buttons) {
        String devToolsButton = GrapheneMouseButtonUtil.toDevToolsButton(button);
        if (GrapheneMouseButtonUtil.DEVTOOLS_BUTTON_NONE.equals(devToolsButton)) {
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty(PROPERTY_TYPE, pressed ? MOUSE_EVENT_TYPE_PRESSED : MOUSE_EVENT_TYPE_RELEASED);
        payload.addProperty(PROPERTY_X, x);
        payload.addProperty(PROPERTY_Y, y);
        payload.addProperty(PROPERTY_MODIFIERS, GrapheneInputModifierUtil.toDevToolsModifiers(modifiers));
        payload.addProperty(PROPERTY_BUTTON, devToolsButton);
        payload.addProperty(PROPERTY_BUTTONS, buttons);
        payload.addProperty(PROPERTY_CLICK_COUNT, clickCount);
        devToolsMethodExecutor.accept(DEVTOOLS_METHOD_DISPATCH_MOUSE_EVENT, payload);
    }
}
