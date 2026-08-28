package io.github.trethore.graphene.internal.browser;

import io.github.trethore.graphene.api.browser.input.BrowserModifier;
import io.github.trethore.graphene.api.browser.input.BrowserPointerAction;
import io.github.trethore.graphene.api.browser.input.BrowserPointerButton;
import io.github.trethore.graphene.api.browser.input.BrowserTextInput;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class GrapheneBrowserInputState {
    private static final int FILTERED_CHARACTER = -1;
    private static final long SYNTHETIC_DUPLICATE_WINDOW_MILLIS = 250;

    private Set<BrowserModifier> currentModifiers = Set.of();
    private String pendingSyntheticText;
    private long pendingSyntheticTimestamp;
    private boolean rightAltPressed;
    private BrowserPointerButton pressedButton = BrowserPointerButton.NONE;

    public void reset() {
        currentModifiers = Set.of();
        pendingSyntheticText = null;
        pendingSyntheticTimestamp = 0;
        rightAltPressed = false;
        pressedButton = BrowserPointerButton.NONE;
    }

    public void updateModifiers(Set<BrowserModifier> modifiers) {
        currentModifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
    }

    public Set<BrowserModifier> currentModifiers() {
        return currentModifiers;
    }

    public void setRightAltPressed(boolean pressed) {
        rightAltPressed = pressed;
    }

    public BrowserPointerAction pointerMovementAction() {
        return pressedButton == BrowserPointerButton.NONE ? BrowserPointerAction.MOVE : BrowserPointerAction.DRAG;
    }

    public BrowserPointerButton pressedButton() {
        return pressedButton;
    }

    public void updatePointerButton(BrowserPointerButton button, boolean pressed) {
        BrowserPointerButton validatedButton = Objects.requireNonNull(button, "button");
        if (pressed) {
            pressedButton = validatedButton;
        } else if (pressedButton == validatedButton) {
            pressedButton = BrowserPointerButton.NONE;
        }
    }

    public BrowserTextInput syntheticText(String text, Set<BrowserModifier> modifiers, long timestamp) {
        String normalizedText = Objects.requireNonNull(text, "text");
        pendingSyntheticText = normalizedText;
        pendingSyntheticTimestamp = timestamp;
        return textInput(normalizedText, modifiers);
    }

    public BrowserTextInput committedText(String text, Set<BrowserModifier> modifiers, long timestamp) {
        String normalizedText = normalizeText(text);
        if (normalizedText == null || isSyntheticDuplicate(normalizedText, timestamp)) {
            return null;
        }
        return textInput(normalizedText, modifiers);
    }

    public static String normalizeText(String text) {
        Objects.requireNonNull(text, "text");
        if (text.isEmpty()) {
            return null;
        }
        StringBuilder normalized = null;
        int unchangedStart = 0;
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            int replacement = normalizeTextCharacter(character);
            if (replacement == character) {
                continue;
            }
            if (normalized == null) {
                normalized = new StringBuilder(text.length());
            }
            normalized.append(text, unchangedStart, index);
            if (replacement != FILTERED_CHARACTER) {
                normalized.append((char) replacement);
            }
            unchangedStart = index + 1;
        }
        if (normalized == null) {
            return text;
        }
        normalized.append(text, unchangedStart, text.length());
        return normalized.isEmpty() ? null : normalized.toString();
    }

    private BrowserTextInput textInput(String text, Set<BrowserModifier> modifiers) {
        Set<BrowserModifier> browserModifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers"));
        if (rightAltPressed
                && browserModifiers.contains(BrowserModifier.ALT)
                && browserModifiers.contains(BrowserModifier.CONTROL)) {
            EnumSet<BrowserModifier> sanitized = EnumSet.copyOf(browserModifiers);
            sanitized.remove(BrowserModifier.ALT);
            sanitized.remove(BrowserModifier.CONTROL);
            browserModifiers = Set.copyOf(sanitized);
        }
        return new BrowserTextInput(text, browserModifiers);
    }

    private boolean isSyntheticDuplicate(String text, long timestamp) {
        if (pendingSyntheticText == null) {
            return false;
        }
        boolean duplicate = pendingSyntheticText.equals(text)
                && timestamp - pendingSyntheticTimestamp <= SYNTHETIC_DUPLICATE_WINDOW_MILLIS;
        pendingSyntheticText = null;
        pendingSyntheticTimestamp = 0;
        return duplicate;
    }

    private static int normalizeTextCharacter(char character) {
        if (character == 0x7F) {
            return '\b';
        }
        if (character == '\n') {
            return '\r';
        }
        if ((character >= '\uF700' && character <= '\uF8FF')
                || (Character.isISOControl(character) && character != '\b' && character != '\t' && character != '\r')) {
            return FILTERED_CHARACTER;
        }
        return character;
    }
}
