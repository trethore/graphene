package tytoo.grapheneui.internal.input.keyboard.mapping;

import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class GrapheneNumpadMappings {
    private static final int[][] NUMPAD_KEYS = {
            {GLFW.GLFW_KEY_KP_0},
            {GLFW.GLFW_KEY_KP_1},
            {GLFW.GLFW_KEY_KP_2},
            {GLFW.GLFW_KEY_KP_3},
            {GLFW.GLFW_KEY_KP_4},
            {GLFW.GLFW_KEY_KP_5},
            {GLFW.GLFW_KEY_KP_6},
            {GLFW.GLFW_KEY_KP_7},
            {GLFW.GLFW_KEY_KP_8},
            {GLFW.GLFW_KEY_KP_9},
            {GLFW.GLFW_KEY_KP_DECIMAL},
            {GLFW.GLFW_KEY_KP_DIVIDE},
            {GLFW.GLFW_KEY_KP_MULTIPLY},
            {GLFW.GLFW_KEY_KP_SUBTRACT},
            {GLFW.GLFW_KEY_KP_ADD},
            {GLFW.GLFW_KEY_KP_ENTER},
            {GLFW.GLFW_KEY_KP_EQUAL}
    };
    private static final int[][] NUMPAD_TEXT_KEYS = {
            {GLFW.GLFW_KEY_KP_0},
            {GLFW.GLFW_KEY_KP_1},
            {GLFW.GLFW_KEY_KP_2},
            {GLFW.GLFW_KEY_KP_3},
            {GLFW.GLFW_KEY_KP_4},
            {GLFW.GLFW_KEY_KP_5},
            {GLFW.GLFW_KEY_KP_6},
            {GLFW.GLFW_KEY_KP_7},
            {GLFW.GLFW_KEY_KP_8},
            {GLFW.GLFW_KEY_KP_9},
            {GLFW.GLFW_KEY_KP_DECIMAL},
            {GLFW.GLFW_KEY_KP_DIVIDE},
            {GLFW.GLFW_KEY_KP_MULTIPLY},
            {GLFW.GLFW_KEY_KP_SUBTRACT},
            {GLFW.GLFW_KEY_KP_ADD},
            {GLFW.GLFW_KEY_KP_EQUAL}
    };
    private static final int[][] NUMPAD_TEXT_REQUIRING_NUM_LOCK = {
            {GLFW.GLFW_KEY_KP_0},
            {GLFW.GLFW_KEY_KP_1},
            {GLFW.GLFW_KEY_KP_2},
            {GLFW.GLFW_KEY_KP_3},
            {GLFW.GLFW_KEY_KP_4},
            {GLFW.GLFW_KEY_KP_5},
            {GLFW.GLFW_KEY_KP_6},
            {GLFW.GLFW_KEY_KP_7},
            {GLFW.GLFW_KEY_KP_8},
            {GLFW.GLFW_KEY_KP_9},
            {GLFW.GLFW_KEY_KP_DECIMAL}
    };
    private static final Object[][] NUMPAD_DOM_KEY_WITH_NUM_LOCK = {
            {GLFW.GLFW_KEY_KP_0, "0"},
            {GLFW.GLFW_KEY_KP_1, "1"},
            {GLFW.GLFW_KEY_KP_2, "2"},
            {GLFW.GLFW_KEY_KP_3, "3"},
            {GLFW.GLFW_KEY_KP_4, "4"},
            {GLFW.GLFW_KEY_KP_5, "5"},
            {GLFW.GLFW_KEY_KP_6, "6"},
            {GLFW.GLFW_KEY_KP_7, "7"},
            {GLFW.GLFW_KEY_KP_8, "8"},
            {GLFW.GLFW_KEY_KP_9, "9"},
            {GLFW.GLFW_KEY_KP_DECIMAL, "."},
            {GLFW.GLFW_KEY_KP_DIVIDE, "/"},
            {GLFW.GLFW_KEY_KP_MULTIPLY, "*"},
            {GLFW.GLFW_KEY_KP_SUBTRACT, "-"},
            {GLFW.GLFW_KEY_KP_ADD, "+"},
            {GLFW.GLFW_KEY_KP_ENTER, "Enter"},
            {GLFW.GLFW_KEY_KP_EQUAL, "="}
    };
    private static final Object[][] NUMPAD_DOM_KEY_WITHOUT_NUM_LOCK = {
            {GLFW.GLFW_KEY_KP_0, "Insert"},
            {GLFW.GLFW_KEY_KP_1, "End"},
            {GLFW.GLFW_KEY_KP_2, "ArrowDown"},
            {GLFW.GLFW_KEY_KP_3, "PageDown"},
            {GLFW.GLFW_KEY_KP_4, "ArrowLeft"},
            {GLFW.GLFW_KEY_KP_5, "Clear"},
            {GLFW.GLFW_KEY_KP_6, "ArrowRight"},
            {GLFW.GLFW_KEY_KP_7, "Home"},
            {GLFW.GLFW_KEY_KP_8, "ArrowUp"},
            {GLFW.GLFW_KEY_KP_9, "PageUp"},
            {GLFW.GLFW_KEY_KP_DECIMAL, "Delete"},
            {GLFW.GLFW_KEY_KP_DIVIDE, "/"},
            {GLFW.GLFW_KEY_KP_MULTIPLY, "*"},
            {GLFW.GLFW_KEY_KP_SUBTRACT, "-"},
            {GLFW.GLFW_KEY_KP_ADD, "+"},
            {GLFW.GLFW_KEY_KP_ENTER, "Enter"},
            {GLFW.GLFW_KEY_KP_EQUAL, "="}
    };
    private static final Set<Integer> NUMPAD_KEYS_SET = createSetByFirstColumn(NUMPAD_KEYS);
    private static final Set<Integer> NUMPAD_TEXT_KEYS_SET = createSetByFirstColumn(NUMPAD_TEXT_KEYS);
    private static final Set<Integer> NUMPAD_TEXT_REQUIRING_NUM_LOCK_SET = createSetByFirstColumn(NUMPAD_TEXT_REQUIRING_NUM_LOCK);
    private static final Map<Integer, String> NUMPAD_DOM_KEY_WITH_NUM_LOCK_BY_GLFW = createStringByFirstColumn(NUMPAD_DOM_KEY_WITH_NUM_LOCK);
    private static final Map<Integer, String> NUMPAD_DOM_KEY_WITHOUT_NUM_LOCK_BY_GLFW = createStringByFirstColumn(NUMPAD_DOM_KEY_WITHOUT_NUM_LOCK);

    private GrapheneNumpadMappings() {
    }

    static boolean isNumpadKey(int keyCode) {
        return NUMPAD_KEYS_SET.contains(keyCode);
    }

    static boolean isTextKey(int keyCode) {
        return NUMPAD_TEXT_KEYS_SET.contains(keyCode);
    }

    static boolean requiresNumLockForText(int keyCode) {
        return NUMPAD_TEXT_REQUIRING_NUM_LOCK_SET.contains(keyCode);
    }

    static String domKeyFromGlfw(int keyCode, boolean numLockEnabled) {
        Map<Integer, String> keyMap = numLockEnabled
                ? NUMPAD_DOM_KEY_WITH_NUM_LOCK_BY_GLFW
                : NUMPAD_DOM_KEY_WITHOUT_NUM_LOCK_BY_GLFW;
        return keyMap.getOrDefault(keyCode, "");
    }

    private static Set<Integer> createSetByFirstColumn(int[][] rows) {
        Set<Integer> values = new HashSet<>();
        for (int[] row : rows) {
            values.add(row[0]);
        }

        return Set.copyOf(values);
    }

    private static Map<Integer, String> createStringByFirstColumn(Object[][] rows) {
        Map<Integer, String> mappings = new HashMap<>();
        for (Object[] row : rows) {
            mappings.put((Integer) row[0], (String) row[1]);
        }

        return Map.copyOf(mappings);
    }
}
