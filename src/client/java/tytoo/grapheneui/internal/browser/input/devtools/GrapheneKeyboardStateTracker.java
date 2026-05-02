package tytoo.grapheneui.internal.browser.input.devtools;

import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class GrapheneKeyboardStateTracker {
    private final Map<Integer, GrapheneDomKeyData> activeKeyDataByKeyCode = new HashMap<>();
    private final Set<Integer> pressedKeys = new HashSet<>();

    boolean markPressed(int keyCode) {
        return !pressedKeys.add(keyCode);
    }

    void markReleased(int keyCode) {
        pressedKeys.remove(keyCode);
    }

    void rememberActiveKeyData(int keyCode, GrapheneDomKeyData keyData) {
        activeKeyDataByKeyCode.put(keyCode, keyData);
    }

    GrapheneDomKeyData clearActiveKeyData(int keyCode) {
        return activeKeyDataByKeyCode.remove(keyCode);
    }

    void reset() {
        pressedKeys.clear();
        activeKeyDataByKeyCode.clear();
    }
}
