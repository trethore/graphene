package tytoo.grapheneui.internal.browser.input;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.api.bridge.GrapheneBridge;
import tytoo.grapheneui.api.bridge.GrapheneBridgeEventListener;
import tytoo.grapheneui.api.bridge.GrapheneBridgeRequestHandler;
import tytoo.grapheneui.api.bridge.GrapheneBridgeSubscription;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneExtraMouseButtonEmitterTest {
    @Test
    void emitIgnoresNonExtraMouseButtons() {
        RecordingBridge bridge = new RecordingBridge();
        GrapheneExtraMouseButtonEmitter emitter = new GrapheneExtraMouseButtonEmitter(bridge);

        emitter.emit(GLFW.GLFW_MOUSE_BUTTON_1, true);

        assertEquals(0, bridge.events.size());
    }

    @Test
    void emitSendsExtraMouseButtonPayload() {
        RecordingBridge bridge = new RecordingBridge();
        GrapheneExtraMouseButtonEmitter emitter = new GrapheneExtraMouseButtonEmitter(bridge);

        emitter.emit(GLFW.GLFW_MOUSE_BUTTON_6, true);

        assertEquals(List.of(new EmittedEvent(
                "graphene:mouse:extra-button",
                "{\"button\":" + GLFW.GLFW_MOUSE_BUTTON_6 + ",\"pressed\":true}"
        )), bridge.events);
    }

    @Test
    void resetSendsResetEvent() {
        RecordingBridge bridge = new RecordingBridge();
        GrapheneExtraMouseButtonEmitter emitter = new GrapheneExtraMouseButtonEmitter(bridge);

        emitter.reset();

        assertEquals(List.of(new EmittedEvent("graphene:mouse:extra-reset", "{}")), bridge.events);
    }

    @Test
    void emitAndResetIgnoreBridgeShutdown() {
        RecordingBridge bridge = new RecordingBridge();
        bridge.throwOnEmit = true;
        GrapheneExtraMouseButtonEmitter emitter = new GrapheneExtraMouseButtonEmitter(bridge);

        emitter.emit(GLFW.GLFW_MOUSE_BUTTON_6, true);
        emitter.reset();

        assertEquals(0, bridge.events.size());
    }

    private record EmittedEvent(String channel, String payloadJson) {
    }

    private static final class RecordingBridge implements GrapheneBridge {
        private final List<EmittedEvent> events = new ArrayList<>();
        private boolean throwOnEmit;

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public GrapheneBridgeSubscription onReady(Runnable listener) {
            return () -> {
            };
        }

        @Override
        public GrapheneBridgeSubscription onEvent(String channel, GrapheneBridgeEventListener listener) {
            return () -> {
            };
        }

        @Override
        public GrapheneBridgeSubscription onRequest(String channel, GrapheneBridgeRequestHandler handler) {
            return () -> {
            };
        }

        @Override
        public void emit(String channel, String payloadJson) {
            if (throwOnEmit) {
                throw new IllegalStateException("closed");
            }

            events.add(new EmittedEvent(channel, payloadJson));
        }

        @Override
        public CompletableFuture<String> request(String channel, String payloadJson, Duration timeout) {
            return CompletableFuture.completedFuture("");
        }
    }
}
