package tytoo.grapheneui.internal.input.keyboard;

public record GrapheneDomKeyData(
        String code,
        String key,
        int windowsVirtualKeyCode,
        int nativeVirtualKeyCode,
        int location,
        boolean keypad,
        boolean systemKey,
        int modifiers
) {
}
