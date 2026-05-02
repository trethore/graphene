package tytoo.grapheneui.internal.browser.input.devtools;

import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyData;

public final class GraphenePendingKeyboardInput {
    private static final long SYNTHETIC_TYPED_DUPLICATE_WINDOW_MS = 250L;

    private PendingKeyDown pendingKeyDown;
    private PendingKeyUp pendingKeyUp;
    private String pendingSyntheticText = "";
    private long pendingSyntheticTextTimestamp;

    void reset() {
        pendingKeyDown = null;
        pendingKeyUp = null;
        pendingSyntheticText = "";
        pendingSyntheticTextTimestamp = 0L;
    }

    boolean isKeyDownPending(int keyCode) {
        return pendingKeyDown != null && pendingKeyDown.keyCode() == keyCode;
    }

    void setKeyDown(PendingKeyDown keyDown) {
        pendingKeyDown = keyDown;
    }

    PendingKeyDown clearKeyDown() {
        PendingKeyDown keyDown = pendingKeyDown;
        pendingKeyDown = null;
        return keyDown;
    }

    boolean hasKeyDown() {
        return pendingKeyDown != null;
    }

    void setKeyUp(PendingKeyUp keyUp) {
        pendingKeyUp = keyUp;
    }

    PendingKeyUp clearKeyUp() {
        PendingKeyUp keyUp = pendingKeyUp;
        pendingKeyUp = null;
        return keyUp;
    }

    boolean hasKeyUp() {
        return pendingKeyUp != null;
    }

    void rememberSyntheticText(String text) {
        pendingSyntheticText = text;
        pendingSyntheticTextTimestamp = System.currentTimeMillis();
    }

    boolean isDuplicateSyntheticText(String text) {
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

    record PendingKeyDown(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean numLockEnabled,
            GrapheneDomKeyData keyData,
            boolean autoRepeat
    ) {
    }

    record PendingKeyUp(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean numLockEnabled
    ) {
    }
}
