package tytoo.grapheneui.internal.browser;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneDomKeyboardDispatcherTest {
    private static final String DISPATCH_KEY_EVENT_METHOD = "Input.dispatchKeyEvent";

    @Test
    void textInputUpdatesDeferredPrintableKeyDown() {
        List<DispatchedMethod> dispatchedMethods = new ArrayList<>();
        GrapheneDomKeyboardDispatcher dispatcher = new GrapheneDomKeyboardDispatcher(
                (method, payload) -> dispatchedMethods.add(new DispatchedMethod(method, payload.deepCopy()))
        );

        dispatcher.keyPressed(GLFW.GLFW_KEY_GRAVE_ACCENT, 41, 0);
        assertEquals(0, dispatchedMethods.size());

        dispatcher.textInput("\u00B2");

        assertEquals(1, dispatchedMethods.size());
        JsonObject payload = dispatchedMethods.getFirst().payload();
        assertEquals(DISPATCH_KEY_EVENT_METHOD, dispatchedMethods.getFirst().method());
        assertEquals("keyDown", payload.get("type").getAsString());
        assertEquals("\u00B2", payload.get("key").getAsString());
        assertEquals(0xDE, payload.get("windowsVirtualKeyCode").getAsInt());
        assertEquals("\u00B2", payload.get("text").getAsString());
        assertEquals("\u00B2", payload.get("unmodifiedText").getAsString());
    }

    @Test
    void keyReleaseBeforeTextInputUsesCorrectedKeyData() {
        List<DispatchedMethod> dispatchedMethods = new ArrayList<>();
        GrapheneDomKeyboardDispatcher dispatcher = new GrapheneDomKeyboardDispatcher(
                (method, payload) -> dispatchedMethods.add(new DispatchedMethod(method, payload.deepCopy()))
        );

        dispatcher.keyPressed(GLFW.GLFW_KEY_GRAVE_ACCENT, 41, 0);
        dispatcher.keyReleased(GLFW.GLFW_KEY_GRAVE_ACCENT, 41, 0);
        assertEquals(0, dispatchedMethods.size());

        dispatcher.textInput("\u00B2");

        assertEquals(2, dispatchedMethods.size());
        JsonObject keyDownPayload = dispatchedMethods.get(0).payload();
        JsonObject keyUpPayload = dispatchedMethods.get(1).payload();
        assertEquals("keyDown", keyDownPayload.get("type").getAsString());
        assertEquals("\u00B2", keyDownPayload.get("key").getAsString());
        assertEquals(0xDE, keyDownPayload.get("windowsVirtualKeyCode").getAsInt());
        assertEquals("keyUp", keyUpPayload.get("type").getAsString());
        assertEquals("\u00B2", keyUpPayload.get("key").getAsString());
        assertEquals(0xDE, keyUpPayload.get("windowsVirtualKeyCode").getAsInt());
    }

    @Test
    void textInputUpdatesDeferredIsoLessGreaterKeyDown() {
        List<DispatchedMethod> dispatchedMethods = new ArrayList<>();
        GrapheneDomKeyboardDispatcher dispatcher = new GrapheneDomKeyboardDispatcher(
                (method, payload) -> dispatchedMethods.add(new DispatchedMethod(method, payload.deepCopy()))
        );

        dispatcher.keyPressed(GLFW.GLFW_KEY_WORLD_1, 86, GLFW.GLFW_MOD_SHIFT);
        dispatcher.textInput(">");

        assertEquals(1, dispatchedMethods.size());
        JsonObject payload = dispatchedMethods.getFirst().payload();
        assertEquals("keyDown", payload.get("type").getAsString());
        assertEquals(">", payload.get("key").getAsString());
        assertEquals(0xE2, payload.get("windowsVirtualKeyCode").getAsInt());
        assertEquals(">", payload.get("text").getAsString());
    }

    private record DispatchedMethod(String method, JsonObject payload) {
    }
}
