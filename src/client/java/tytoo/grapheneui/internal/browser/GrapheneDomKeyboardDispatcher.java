package tytoo.grapheneui.internal.browser;

import com.google.gson.JsonObject;
import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneInputModifierUtil;
import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyData;
import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyboardMapper;
import tytoo.grapheneui.internal.input.keyboard.GrapheneInputLockState;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.*;
import java.util.function.BiConsumer;

final class GrapheneDomKeyboardDispatcher {
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;
    private static final String DEVTOOLS_METHOD_DISPATCH_KEY_EVENT = "Input.dispatchKeyEvent";
    private static final String DEVTOOLS_METHOD_INSERT_TEXT = "Input.insertText";
    private static final String KEY_EVENT_TYPE_RAW_KEY_DOWN = "rawKeyDown";
    private static final String KEY_EVENT_TYPE_KEY_DOWN = "keyDown";
    private static final String KEY_EVENT_TYPE_KEY_UP = "keyUp";
    private static final String PROPERTY_TYPE = "type";
    private static final String PROPERTY_MODIFIERS = "modifiers";
    private static final String PROPERTY_CODE = "code";
    private static final String PROPERTY_KEY = "key";
    private static final String PROPERTY_TEXT = "text";
    private static final String PROPERTY_UNMODIFIED_TEXT = "unmodifiedText";
    private static final String PROPERTY_WINDOWS_VIRTUAL_KEY_CODE = "windowsVirtualKeyCode";
    private static final String PROPERTY_NATIVE_VIRTUAL_KEY_CODE = "nativeVirtualKeyCode";
    private static final String PROPERTY_AUTO_REPEAT = "autoRepeat";
    private static final String PROPERTY_IS_KEYPAD = "isKeypad";
    private static final String PROPERTY_IS_SYSTEM_KEY = "isSystemKey";
    private static final String PROPERTY_LOCATION = "location";
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(GrapheneDomKeyboardDispatcher.class);

    private final BiConsumer<String, JsonObject> devToolsMethodExecutor;
    private final GrapheneDomKeyboardMapper keyboardMapper = new GrapheneDomKeyboardMapper();
    private final GrapheneInputLockState lockState = new GrapheneInputLockState();
    private final Map<Integer, GrapheneDomKeyData> activeKeyDataByKeyCode = new HashMap<>();
    private final Set<Integer> pressedKeys = new HashSet<>();

    private PendingKeyDown pendingKeyDown;
    private PendingKeyUp pendingKeyUp;
    private String pendingSyntheticText = "";
    private long pendingSyntheticTextTimestamp;

    GrapheneDomKeyboardDispatcher(GrapheneBrowser browser) {
        GrapheneDevToolsMethodExecutor executor = new GrapheneDevToolsMethodExecutor(Objects.requireNonNull(browser, "browser"));
        this.devToolsMethodExecutor = (method, payload) -> executor.executeMethod(method, payload, DEBUG_LOGGER);
    }

    GrapheneDomKeyboardDispatcher(BiConsumer<String, JsonObject> devToolsMethodExecutor) {
        this.devToolsMethodExecutor = Objects.requireNonNull(devToolsMethodExecutor, "devToolsMethodExecutor");
    }

    void keyPressed(int keyCode, int scanCode, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();
        int resolvedModifiers = GrapheneInputModifierUtil.mergeWithCurrentModifiers(modifiers);
        lockState.updateCachedNumLockState(keyCode, true);
        boolean numLockEnabled = lockState.isNumLockEnabled(resolvedModifiers);
        flushPendingKeyDown();
        GrapheneDomKeyData keyData = keyboardMapper.mapKeyEvent(keyCode, scanCode, resolvedModifiers, true, numLockEnabled);
        boolean autoRepeat = !pressedKeys.add(keyCode);
        if (shouldWaitForTextInput(keyData, resolvedModifiers)) {
            pendingKeyDown = new PendingKeyDown(keyCode, scanCode, resolvedModifiers, numLockEnabled, keyData, autoRepeat);
            return;
        }

        dispatchRawKeyDown(keyCode, keyData, autoRepeat);

        String syntheticText = keyboardMapper.resolveSyntheticText(keyCode, keyData, numLockEnabled);
        if (!syntheticText.isEmpty()) {
            rememberSyntheticText(syntheticText);
            dispatchInsertText(syntheticText);
        }
    }

    void keyReleased(int keyCode, int scanCode, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();
        int resolvedModifiers = GrapheneInputModifierUtil.mergeWithCurrentModifiers(modifiers);
        boolean numLockEnabled = lockState.isNumLockEnabled(resolvedModifiers);
        if (isPendingKeyDown(keyCode)) {
            pressedKeys.remove(keyCode);
            pendingKeyUp = new PendingKeyUp(keyCode, scanCode, resolvedModifiers, numLockEnabled);
            return;
        }

        flushPendingKeyDown();
        pressedKeys.remove(keyCode);
        GrapheneDomKeyData keyData = activeKeyDataByKeyCode.remove(keyCode);
        if (keyData == null) {
            keyData = keyboardMapper.mapKeyEvent(keyCode, scanCode, resolvedModifiers, false, numLockEnabled);
        }
        dispatchKeyEvent(KEY_EVENT_TYPE_KEY_UP, keyData, false);
    }

    void textInput(String text) {
        lockState.ensureLockKeyModifiersEnabled();
        String normalizedText = keyboardMapper.normalizeTypedText(text);
        if (normalizedText.isEmpty() || isDuplicateSyntheticText(normalizedText)) {
            flushPendingKeyDown();
            return;
        }

        if (dispatchPendingKeyDown(normalizedText)) {
            flushPendingKeyUp();
            return;
        }

        flushPendingKeyUp();

        if (shouldLetKeyEventHandleText(normalizedText)) {
            return;
        }

        dispatchInsertText(normalizedText);
    }

    void resetState() {
        pressedKeys.clear();
        activeKeyDataByKeyCode.clear();
        pendingKeyDown = null;
        pendingKeyUp = null;
        pendingSyntheticText = "";
        pendingSyntheticTextTimestamp = 0L;
    }

    private void dispatchRawKeyDown(int keyCode, GrapheneDomKeyData keyData, boolean autoRepeat) {
        activeKeyDataByKeyCode.put(keyCode, keyData);
        dispatchKeyEvent(KEY_EVENT_TYPE_RAW_KEY_DOWN, keyData, autoRepeat);
    }

    private void dispatchKeyEvent(String type, GrapheneDomKeyData keyData, boolean autoRepeat) {
        dispatchKeyEvent(type, keyData, autoRepeat, "");
    }

    private void dispatchKeyEvent(String type, GrapheneDomKeyData keyData, boolean autoRepeat, String text) {
        JsonObject payload = new JsonObject();
        payload.addProperty(PROPERTY_TYPE, type);
        payload.addProperty(PROPERTY_MODIFIERS, keyData.modifiers());
        appendKeyDataProperties(payload, keyData);
        if (KEY_EVENT_TYPE_RAW_KEY_DOWN.equals(type) || KEY_EVENT_TYPE_KEY_DOWN.equals(type)) {
            payload.addProperty(PROPERTY_TEXT, text);
            payload.addProperty(PROPERTY_UNMODIFIED_TEXT, text);
        }
        payload.addProperty(PROPERTY_AUTO_REPEAT, autoRepeat);
        payload.addProperty(PROPERTY_IS_SYSTEM_KEY, keyData.systemKey());
        executeDevToolsMethod(DEVTOOLS_METHOD_DISPATCH_KEY_EVENT, payload);
    }

    private void dispatchInsertText(String text) {
        JsonObject payload = new JsonObject();
        payload.addProperty(PROPERTY_TEXT, text);
        executeDevToolsMethod(DEVTOOLS_METHOD_INSERT_TEXT, payload);
    }

    private void flushPendingKeyDown() {
        if (pendingKeyDown == null) {
            return;
        }

        PendingKeyDown keyDown = pendingKeyDown;
        pendingKeyDown = null;
        dispatchRawKeyDown(keyDown.keyCode(), keyDown.keyData(), keyDown.autoRepeat());
        flushPendingKeyUp();
    }

    private boolean dispatchPendingKeyDown(String text) {
        if (pendingKeyDown == null) {
            return false;
        }

        PendingKeyDown keyDown = pendingKeyDown;
        pendingKeyDown = null;
        GrapheneDomKeyData keyData = resolvePendingKeyData(keyDown, text);
        activeKeyDataByKeyCode.put(keyDown.keyCode(), keyData);
        dispatchKeyEvent(KEY_EVENT_TYPE_KEY_DOWN, keyData, keyDown.autoRepeat(), text);
        return true;
    }

    private void flushPendingKeyUp() {
        if (pendingKeyUp == null) {
            return;
        }

        PendingKeyUp keyUp = pendingKeyUp;
        pendingKeyUp = null;
        GrapheneDomKeyData keyData = activeKeyDataByKeyCode.remove(keyUp.keyCode());
        if (keyData == null) {
            keyData = keyboardMapper.mapKeyEvent(
                    keyUp.keyCode(),
                    keyUp.scanCode(),
                    keyUp.modifiers(),
                    false,
                    keyUp.numLockEnabled()
            );
        }
        dispatchKeyEvent(KEY_EVENT_TYPE_KEY_UP, keyData, false);
    }

    private boolean isPendingKeyDown(int keyCode) {
        return pendingKeyDown != null && pendingKeyDown.keyCode() == keyCode;
    }

    private GrapheneDomKeyData resolvePendingKeyData(PendingKeyDown keyDown, String text) {
        if (text.codePointCount(0, text.length()) != 1) {
            return keyDown.keyData();
        }

        int codePoint = text.codePointAt(0);
        if (Character.isSupplementaryCodePoint(codePoint)) {
            return keyDown.keyData();
        }

        return keyboardMapper.mapKeyEventWithCharacter(
                keyDown.keyCode(),
                keyDown.scanCode(),
                keyDown.modifiers(),
                true,
                keyDown.numLockEnabled(),
                (char) codePoint
        );
    }

    private boolean shouldWaitForTextInput(GrapheneDomKeyData keyData, int modifiers) {
        if (keyData.keypad()) {
            return false;
        }

        boolean mayProduceText = keyData.key().length() == 1 || "Unidentified".equals(keyData.key());
        return mayProduceText && (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) == 0;
    }

    private boolean shouldLetKeyEventHandleText(String text) {
        if (text.codePointCount(0, text.length()) != 1) {
            return false;
        }

        int codePoint = text.codePointAt(0);
        return Character.isISOControl(codePoint);
    }

    private void appendKeyDataProperties(JsonObject payload, GrapheneDomKeyData keyData) {
        if (!keyData.code().isEmpty()) {
            payload.addProperty(PROPERTY_CODE, keyData.code());
        }
        payload.addProperty(PROPERTY_KEY, keyData.key());
        payload.addProperty(PROPERTY_WINDOWS_VIRTUAL_KEY_CODE, keyData.windowsVirtualKeyCode());
        payload.addProperty(PROPERTY_NATIVE_VIRTUAL_KEY_CODE, keyData.nativeVirtualKeyCode());
        payload.addProperty(PROPERTY_IS_KEYPAD, keyData.keypad());
        payload.addProperty(PROPERTY_LOCATION, keyData.location());
    }

    private void executeDevToolsMethod(String method, JsonObject payload) {
        devToolsMethodExecutor.accept(method, payload);
    }

    private void rememberSyntheticText(String text) {
        pendingSyntheticText = text;
        pendingSyntheticTextTimestamp = System.currentTimeMillis();
    }

    private boolean isDuplicateSyntheticText(String text) {
        if (pendingSyntheticText.isEmpty()) {
            return false;
        }

        long now = System.currentTimeMillis();
        boolean duplicate = now - pendingSyntheticTextTimestamp <= SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS
                && pendingSyntheticText.equals(text);
        pendingSyntheticText = "";
        pendingSyntheticTextTimestamp = 0L;
        return duplicate;
    }

    private record PendingKeyDown(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean numLockEnabled,
            GrapheneDomKeyData keyData,
            boolean autoRepeat
    ) {
    }

    private record PendingKeyUp(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean numLockEnabled
    ) {
    }
}
