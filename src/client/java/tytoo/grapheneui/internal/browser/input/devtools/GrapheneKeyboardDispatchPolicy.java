package tytoo.grapheneui.internal.browser.input.devtools;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.keyboard.GrapheneDomKeyData;

public final class GrapheneKeyboardDispatchPolicy {
    boolean shouldWaitForTextInput(GrapheneDomKeyData keyData, int modifiers) {
        if (keyData.keypad()) {
            return false;
        }

        boolean mayProduceText = keyData.key().length() == 1 || "Unidentified".equals(keyData.key());
        return mayProduceText && (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_SUPER)) == 0;
    }

    boolean shouldLetKeyEventHandleText(String text) {
        if (text.codePointCount(0, text.length()) != 1) {
            return false;
        }

        int codePoint = text.codePointAt(0);
        return Character.isISOControl(codePoint);
    }
}
