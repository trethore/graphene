package tytoo.grapheneui.internal.input.keyboard;

import org.lwjgl.glfw.GLFW;
import tytoo.grapheneui.internal.input.GrapheneInputModifierUtil;
import tytoo.grapheneui.internal.input.keyboard.mapping.GrapheneKeyboardMappings;
import tytoo.grapheneui.internal.input.keyboard.platform.GrapheneKeyEventPlatformResolver;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.awt.event.KeyEvent;

public final class GrapheneDomKeyboardMapper {
    private final GrapheneKeyEventPlatformResolver platformResolver = GrapheneKeyEventPlatformResolver.create();

    public GrapheneDomKeyData mapKeyEvent(int keyCode, int scanCode, int modifiers, boolean pressed, boolean numLockEnabled) {
        int resolvedScanCode = platformResolver.resolveScanCode(keyCode, scanCode);
        char layoutCharacter = GrapheneKeyboardSharedUtil.resolveLayoutCharacter(keyCode, resolvedScanCode, modifiers);
        return mapResolvedKeyEvent(keyCode, resolvedScanCode, modifiers, pressed, numLockEnabled, layoutCharacter);
    }

    public GrapheneDomKeyData mapKeyEventWithCharacter(
            int keyCode,
            int scanCode,
            int modifiers,
            boolean pressed,
            boolean numLockEnabled,
            char character
    ) {
        int resolvedScanCode = platformResolver.resolveScanCode(keyCode, scanCode);
        return mapResolvedKeyEvent(keyCode, resolvedScanCode, modifiers, pressed, numLockEnabled, character);
    }

    public String normalizeTypedText(String text) {
        return GrapheneKeyboardSharedUtil.normalizeTypedText(text);
    }

    public String resolveSyntheticText(int keyCode, GrapheneDomKeyData keyData, boolean numLockEnabled) {
        if (!GrapheneKeyboardMappings.isNumpadTextKey(keyCode)) {
            return "";
        }

        if (GrapheneKeyboardMappings.requiresNumLockForText(keyCode) && !numLockEnabled) {
            return "";
        }

        String key = keyData.key();
        return key.length() == 1 ? key : "";
    }

    private GrapheneDomKeyData mapResolvedKeyEvent(
            int keyCode,
            int resolvedScanCode,
            int modifiers,
            boolean pressed,
            boolean numLockEnabled,
            char character
    ) {
        char normalizedCharacter = GrapheneKeyboardSharedUtil.normalizeTypedCharacter(character);
        return new GrapheneDomKeyData(
                GrapheneKeyboardMappings.domCodeFromGlfw(keyCode),
                resolveDomKey(keyCode, normalizedCharacter, numLockEnabled),
                GrapheneKeyboardSharedUtil.resolveWindowsVirtualKeyCode(keyCode, normalizedCharacter, numLockEnabled),
                platformResolver.getNativeVirtualKeyCode(keyCode, resolvedScanCode, normalizedCharacter, pressed),
                resolveLocation(keyCode),
                GrapheneKeyboardMappings.isNumpadKey(keyCode),
                platformResolver.isSystemKey(modifiers),
                GrapheneInputModifierUtil.toDevToolsModifiers(modifiers)
        );
    }

    private String resolveDomKey(int keyCode, char character, boolean numLockEnabled) {
        if (GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            String numpadKey = GrapheneKeyboardMappings.domKeyFromNumpadKey(keyCode, numLockEnabled);
            if (!numpadKey.isEmpty()) {
                return numpadKey;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT_ALT && (GraphenePlatform.isWindows() || GraphenePlatform.isLinux())) {
            return "AltGraph";
        }

        String mappedKey = GrapheneKeyboardMappings.domKeyFromGlfw(keyCode);
        if (!mappedKey.isEmpty()) {
            return mappedKey;
        }

        if (character != KeyEvent.CHAR_UNDEFINED) {
            return String.valueOf(character);
        }

        return "Unidentified";
    }

    private int resolveLocation(int keyCode) {
        if (GrapheneKeyboardMappings.isLeftModifierKey(keyCode)) {
            return 1;
        }

        if (GrapheneKeyboardMappings.isRightModifierKey(keyCode)) {
            return 2;
        }

        if (GrapheneKeyboardMappings.isNumpadKey(keyCode)) {
            return 3;
        }

        return 0;
    }
}
