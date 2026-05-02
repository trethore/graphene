package tytoo.grapheneui.internal.browser.input.devtools;

import com.google.gson.JsonObject;
import tytoo.grapheneui.internal.browser.GrapheneBrowser;
import tytoo.grapheneui.internal.input.GrapheneInputModifierUtil;
import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyData;
import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyboardMapper;
import tytoo.grapheneui.internal.input.keyboard.GrapheneInputLockState;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class GrapheneDomKeyboardDispatcher {
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
    private final GrapheneKeyboardDispatchPolicy dispatchPolicy = new GrapheneKeyboardDispatchPolicy();
    private final GrapheneKeyboardStateTracker stateTracker = new GrapheneKeyboardStateTracker();
    private final GraphenePendingKeyboardInput pendingInput = new GraphenePendingKeyboardInput();

    public GrapheneDomKeyboardDispatcher(GrapheneBrowser browser) {
        GrapheneDevToolsMethodExecutor executor = new GrapheneDevToolsMethodExecutor(Objects.requireNonNull(browser, "browser"));
        this.devToolsMethodExecutor = (method, payload) -> executor.executeMethod(method, payload, DEBUG_LOGGER);
    }

    public GrapheneDomKeyboardDispatcher(BiConsumer<String, JsonObject> devToolsMethodExecutor) {
        this.devToolsMethodExecutor = Objects.requireNonNull(devToolsMethodExecutor, "devToolsMethodExecutor");
    }

    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();
        int resolvedModifiers = GrapheneInputModifierUtil.mergeWithCurrentModifiers(modifiers);
        lockState.updateCachedNumLockState(keyCode, true);
        boolean numLockEnabled = lockState.isNumLockEnabled(resolvedModifiers);
        flushPendingKeyDown();

        GrapheneDomKeyData keyData = keyboardMapper.mapKeyEvent(keyCode, scanCode, resolvedModifiers, true, numLockEnabled);
        boolean autoRepeat = stateTracker.markPressed(keyCode);
        if (dispatchPolicy.shouldWaitForTextInput(keyData, resolvedModifiers)) {
            pendingInput.setKeyDown(new GraphenePendingKeyboardInput.PendingKeyDown(
                    keyCode,
                    scanCode,
                    resolvedModifiers,
                    numLockEnabled,
                    keyData,
                    autoRepeat
            ));
            return;
        }

        dispatchRawKeyDown(keyCode, keyData, autoRepeat);
        dispatchSyntheticTextIfAvailable(keyCode, keyData, numLockEnabled);
    }

    public void keyReleased(int keyCode, int scanCode, int modifiers) {
        lockState.ensureLockKeyModifiersEnabled();
        int resolvedModifiers = GrapheneInputModifierUtil.mergeWithCurrentModifiers(modifiers);
        boolean numLockEnabled = lockState.isNumLockEnabled(resolvedModifiers);
        if (pendingInput.isKeyDownPending(keyCode)) {
            stateTracker.markReleased(keyCode);
            pendingInput.setKeyUp(new GraphenePendingKeyboardInput.PendingKeyUp(keyCode, scanCode, resolvedModifiers, numLockEnabled));
            return;
        }

        flushPendingKeyDown();
        stateTracker.markReleased(keyCode);
        GrapheneDomKeyData keyData = stateTracker.clearActiveKeyData(keyCode);
        if (keyData == null) {
            keyData = keyboardMapper.mapKeyEvent(keyCode, scanCode, resolvedModifiers, false, numLockEnabled);
        }
        dispatchKeyEvent(KEY_EVENT_TYPE_KEY_UP, keyData, false);
    }

    public void textInput(String text) {
        lockState.ensureLockKeyModifiersEnabled();
        String normalizedText = keyboardMapper.normalizeTypedText(text);
        if (normalizedText.isEmpty() || pendingInput.isDuplicateSyntheticText(normalizedText)) {
            flushPendingKeyDown();
            return;
        }

        if (dispatchPendingKeyDown(normalizedText)) {
            flushPendingKeyUp();
            return;
        }

        flushPendingKeyUp();

        if (dispatchPolicy.shouldLetKeyEventHandleText(normalizedText)) {
            return;
        }

        dispatchInsertText(normalizedText);
    }

    public void resetState() {
        stateTracker.reset();
        pendingInput.reset();
    }

    private void dispatchRawKeyDown(int keyCode, GrapheneDomKeyData keyData, boolean autoRepeat) {
        stateTracker.rememberActiveKeyData(keyCode, keyData);
        dispatchKeyEvent(KEY_EVENT_TYPE_RAW_KEY_DOWN, keyData, autoRepeat);
    }

    private void dispatchSyntheticTextIfAvailable(int keyCode, GrapheneDomKeyData keyData, boolean numLockEnabled) {
        String syntheticText = keyboardMapper.resolveSyntheticText(keyCode, keyData, numLockEnabled);
        if (syntheticText.isEmpty()) {
            return;
        }

        pendingInput.rememberSyntheticText(syntheticText);
        dispatchInsertText(syntheticText);
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
        if (!pendingInput.hasKeyDown()) {
            return;
        }

        GraphenePendingKeyboardInput.PendingKeyDown keyDown = pendingInput.clearKeyDown();
        dispatchRawKeyDown(keyDown.keyCode(), keyDown.keyData(), keyDown.autoRepeat());
        flushPendingKeyUp();
    }

    private boolean dispatchPendingKeyDown(String text) {
        if (!pendingInput.hasKeyDown()) {
            return false;
        }

        GraphenePendingKeyboardInput.PendingKeyDown keyDown = pendingInput.clearKeyDown();
        GrapheneDomKeyData keyData = resolvePendingKeyData(keyDown, text);
        stateTracker.rememberActiveKeyData(keyDown.keyCode(), keyData);
        dispatchKeyEvent(KEY_EVENT_TYPE_KEY_DOWN, keyData, keyDown.autoRepeat(), text);
        return true;
    }

    private void flushPendingKeyUp() {
        if (!pendingInput.hasKeyUp()) {
            return;
        }

        GraphenePendingKeyboardInput.PendingKeyUp keyUp = pendingInput.clearKeyUp();
        GrapheneDomKeyData keyData = stateTracker.clearActiveKeyData(keyUp.keyCode());
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

    private GrapheneDomKeyData resolvePendingKeyData(GraphenePendingKeyboardInput.PendingKeyDown keyDown, String text) {
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
}
