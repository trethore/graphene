package tytoo.grapheneui.internal.input.keyboard.platform;

import org.cef.input.CefKeyEvent;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class GrapheneWindowsKeyEventPlatformResolver implements GrapheneKeyEventPlatformResolver {
    private static final int[][] GLFW_SCAN_CODE_PREFERRED_KEYS = {
            {GLFW.GLFW_KEY_BACKSPACE},
            {GLFW.GLFW_KEY_KP_4},
            {GLFW.GLFW_KEY_KP_8},
            {GLFW.GLFW_KEY_KP_6},
            {GLFW.GLFW_KEY_KP_2},
            {GLFW.GLFW_KEY_PRINT_SCREEN},
            {GLFW.GLFW_KEY_SCROLL_LOCK},
            {GLFW.GLFW_KEY_CAPS_LOCK},
            {GLFW.GLFW_KEY_NUM_LOCK},
            {GLFW.GLFW_KEY_PAUSE},
            {GLFW.GLFW_KEY_INSERT}
    };

    private static final int[][] WINDOWS_EXTENDED_KEYS = {
            {GLFW.GLFW_KEY_RIGHT_ALT},
            {GLFW.GLFW_KEY_RIGHT_CONTROL},
            {GLFW.GLFW_KEY_INSERT},
            {GLFW.GLFW_KEY_DELETE},
            {GLFW.GLFW_KEY_HOME},
            {GLFW.GLFW_KEY_END},
            {GLFW.GLFW_KEY_PAGE_UP},
            {GLFW.GLFW_KEY_PAGE_DOWN},
            {GLFW.GLFW_KEY_UP},
            {GLFW.GLFW_KEY_DOWN},
            {GLFW.GLFW_KEY_LEFT},
            {GLFW.GLFW_KEY_RIGHT},
            {GLFW.GLFW_KEY_KP_ENTER},
            {GLFW.GLFW_KEY_KP_DIVIDE},
            {GLFW.GLFW_KEY_NUM_LOCK},
            {GLFW.GLFW_KEY_PRINT_SCREEN}
    };

    private static final Set<Integer> GLFW_SCAN_CODE_PREFERRED_KEY_SET = createSetByFirstColumn(GLFW_SCAN_CODE_PREFERRED_KEYS);
    private static final Map<Integer, Integer> REMAPPED_WINDOWS_SCAN_CODE_BY_KEY = Map.ofEntries(
            Map.entry(GLFW.GLFW_KEY_LEFT_CONTROL, 29),
            Map.entry(GLFW.GLFW_KEY_RIGHT_CONTROL, 29),
            Map.entry(GLFW.GLFW_KEY_DELETE, 83),
            Map.entry(GLFW.GLFW_KEY_LEFT, 75),
            Map.entry(GLFW.GLFW_KEY_DOWN, 80),
            Map.entry(GLFW.GLFW_KEY_UP, 72),
            Map.entry(GLFW.GLFW_KEY_RIGHT, 77),
            Map.entry(GLFW.GLFW_KEY_PAGE_DOWN, 81),
            Map.entry(GLFW.GLFW_KEY_PAGE_UP, 73),
            Map.entry(GLFW.GLFW_KEY_END, 79),
            Map.entry(GLFW.GLFW_KEY_HOME, 71),
            Map.entry(GLFW.GLFW_KEY_ENTER, 28),
            Map.entry(GLFW.GLFW_KEY_KP_ENTER, 28)
    );
    private static final Set<Integer> WINDOWS_EXTENDED_KEY_SET = createSetByFirstColumn(WINDOWS_EXTENDED_KEYS);

    private static boolean isGlfwScanCodePreferredKey(int keyCode) {
        return GLFW_SCAN_CODE_PREFERRED_KEY_SET.contains(keyCode);
    }

    private static int remapWindowsScanCode(int keyCode) {
        return REMAPPED_WINDOWS_SCAN_CODE_BY_KEY.getOrDefault(keyCode, 0);
    }

    private static boolean isWindowsExtendedKey(int keyCode) {
        return WINDOWS_EXTENDED_KEY_SET.contains(keyCode);
    }

    private static Set<Integer> createSetByFirstColumn(int[][] rows) {
        Set<Integer> values = new HashSet<>();
        for (int[] row : rows) {
            values.add(row[0]);
        }

        return Set.copyOf(values);
    }

    @Override
    public int resolveScanCode(int keyCode, int scanCode) {
        if (isGlfwScanCodePreferredKey(keyCode)) {
            int glfwScanCode = GLFW.glfwGetKeyScancode(keyCode);
            if (glfwScanCode > 0) {
                return glfwScanCode;
            }
        }

        int remappedScanCode = remapWindowsScanCode(keyCode);
        if (remappedScanCode != 0) {
            return remappedScanCode;
        }

        return scanCode;
    }

    @Override
    public int getNativeVirtualKeyCode(int keyCode, int scanCode, char character, boolean pressed) {
        if (scanCode <= 0) {
            return 0;
        }

        return CefKeyEvent.buildWindowsNativeKeyCode(scanCode, isWindowsExtendedKey(keyCode), !pressed);
    }
}
