package tytoo.grapheneui.internal.input.keyboard;

final class GrapheneMacKeyEventPlatformResolver extends GrapheneBaseKeyEventPlatformResolver {
    @Override
    public char resolveRawKeyCharacter(int keyCode, int scanCode, int modifiers) {
        return normalizeTypedCharacter(super.resolveRawKeyCharacter(keyCode, scanCode, modifiers));
    }

    @Override
    public int resolveNativeKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        int mappedKeyCode = GrapheneMacKeyCodeMapping.resolveFromGlfwKey(keyCode);
        if (mappedKeyCode != 0) {
            return mappedKeyCode;
        }

        int charMappedKeyCode = GrapheneMacKeyCodeMapping.resolveFromCharacter(normalizeTypedCharacter(character));
        if (charMappedKeyCode != 0) {
            return charMappedKeyCode;
        }

        return Math.max(scanCode, 0);
    }

    @Override
    public int resolveCharNativeKeyCode(char character) {
        return GrapheneMacKeyCodeMapping.resolveFromCharacter(normalizeTypedCharacter(character));
    }

    @Override
    public char normalizeTypedCharacter(char character) {
        if (character == 0x7F) {
            return '\b';
        }

        if (character == '\n' || character == 0x03) {
            return '\r';
        }

        return character;
    }
}
