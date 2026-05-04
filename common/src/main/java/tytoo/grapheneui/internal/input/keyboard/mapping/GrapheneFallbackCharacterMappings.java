package tytoo.grapheneui.internal.input.keyboard.mapping;

import org.lwjgl.glfw.GLFW;

import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

final class GrapheneFallbackCharacterMappings {
    private static final int[][] GLFW_KEY_TO_CHARACTER = {
            {GLFW.GLFW_KEY_A, 'a', 'A'},
            {GLFW.GLFW_KEY_B, 'b', 'B'},
            {GLFW.GLFW_KEY_C, 'c', 'C'},
            {GLFW.GLFW_KEY_D, 'd', 'D'},
            {GLFW.GLFW_KEY_E, 'e', 'E'},
            {GLFW.GLFW_KEY_F, 'f', 'F'},
            {GLFW.GLFW_KEY_G, 'g', 'G'},
            {GLFW.GLFW_KEY_H, 'h', 'H'},
            {GLFW.GLFW_KEY_I, 'i', 'I'},
            {GLFW.GLFW_KEY_J, 'j', 'J'},
            {GLFW.GLFW_KEY_K, 'k', 'K'},
            {GLFW.GLFW_KEY_L, 'l', 'L'},
            {GLFW.GLFW_KEY_M, 'm', 'M'},
            {GLFW.GLFW_KEY_N, 'n', 'N'},
            {GLFW.GLFW_KEY_O, 'o', 'O'},
            {GLFW.GLFW_KEY_P, 'p', 'P'},
            {GLFW.GLFW_KEY_Q, 'q', 'Q'},
            {GLFW.GLFW_KEY_R, 'r', 'R'},
            {GLFW.GLFW_KEY_S, 's', 'S'},
            {GLFW.GLFW_KEY_T, 't', 'T'},
            {GLFW.GLFW_KEY_U, 'u', 'U'},
            {GLFW.GLFW_KEY_V, 'v', 'V'},
            {GLFW.GLFW_KEY_W, 'w', 'W'},
            {GLFW.GLFW_KEY_X, 'x', 'X'},
            {GLFW.GLFW_KEY_Y, 'y', 'Y'},
            {GLFW.GLFW_KEY_Z, 'z', 'Z'},
            {GLFW.GLFW_KEY_0, '0', ')'},
            {GLFW.GLFW_KEY_1, '1', '!'},
            {GLFW.GLFW_KEY_2, '2', '@'},
            {GLFW.GLFW_KEY_3, '3', '#'},
            {GLFW.GLFW_KEY_4, '4', '$'},
            {GLFW.GLFW_KEY_5, '5', '%'},
            {GLFW.GLFW_KEY_6, '6', '^'},
            {GLFW.GLFW_KEY_7, '7', '&'},
            {GLFW.GLFW_KEY_8, '8', '*'},
            {GLFW.GLFW_KEY_9, '9', '('},
            {GLFW.GLFW_KEY_KP_0, '0', '0'},
            {GLFW.GLFW_KEY_KP_1, '1', '1'},
            {GLFW.GLFW_KEY_KP_2, '2', '2'},
            {GLFW.GLFW_KEY_KP_3, '3', '3'},
            {GLFW.GLFW_KEY_KP_4, '4', '4'},
            {GLFW.GLFW_KEY_KP_5, '5', '5'},
            {GLFW.GLFW_KEY_KP_6, '6', '6'},
            {GLFW.GLFW_KEY_KP_7, '7', '7'},
            {GLFW.GLFW_KEY_KP_8, '8', '8'},
            {GLFW.GLFW_KEY_KP_9, '9', '9'},
            {GLFW.GLFW_KEY_MINUS, '-', '_'},
            {GLFW.GLFW_KEY_EQUAL, '=', '+'},
            {GLFW.GLFW_KEY_KP_DECIMAL, '.', '.'},
            {GLFW.GLFW_KEY_KP_DIVIDE, '/', '/'},
            {GLFW.GLFW_KEY_KP_MULTIPLY, '*', '*'},
            {GLFW.GLFW_KEY_KP_SUBTRACT, '-', '-'},
            {GLFW.GLFW_KEY_KP_ADD, '+', '+'},
            {GLFW.GLFW_KEY_KP_EQUAL, '=', '='},
            {GLFW.GLFW_KEY_BACKSLASH, '\\', '|'},
            {GLFW.GLFW_KEY_SLASH, '/', '?'},
            {GLFW.GLFW_KEY_SEMICOLON, ';', ':'},
            {GLFW.GLFW_KEY_COMMA, ',', '<'},
            {GLFW.GLFW_KEY_PERIOD, '.', '>'},
            {GLFW.GLFW_KEY_APOSTROPHE, '\'', '"'},
            {GLFW.GLFW_KEY_LEFT_BRACKET, '[', '{'},
            {GLFW.GLFW_KEY_RIGHT_BRACKET, ']', '}'},
            {GLFW.GLFW_KEY_TAB, '\t', '\t'},
            {GLFW.GLFW_KEY_ENTER, '\r', '\r'},
            {GLFW.GLFW_KEY_KP_ENTER, '\r', '\r'},
            {GLFW.GLFW_KEY_BACKSPACE, '\b', '\b'},
            {GLFW.GLFW_KEY_SPACE, ' ', ' '}
    };
    private static final Map<Integer, Integer> UNSHIFTED_CHARACTER_BY_GLFW_KEY = createCharacterByFirstColumn(1);
    private static final Map<Integer, Integer> SHIFTED_CHARACTER_BY_GLFW_KEY = createCharacterByFirstColumn(2);

    private GrapheneFallbackCharacterMappings() {
    }

    static char fromKeyCode(int keyCode, boolean shift) {
        Map<Integer, Integer> charMap = shift ? SHIFTED_CHARACTER_BY_GLFW_KEY : UNSHIFTED_CHARACTER_BY_GLFW_KEY;
        Integer mappedCharacter = charMap.get(keyCode);
        if (mappedCharacter == null) {
            return KeyEvent.CHAR_UNDEFINED;
        }

        return (char) mappedCharacter.intValue();
    }

    private static Map<Integer, Integer> createCharacterByFirstColumn(int valueColumnIndex) {
        Map<Integer, Integer> mappings = new HashMap<>();
        for (int[] mappingRow : GLFW_KEY_TO_CHARACTER) {
            mappings.put(mappingRow[0], mappingRow[valueColumnIndex]);
        }

        return Map.copyOf(mappings);
    }
}
