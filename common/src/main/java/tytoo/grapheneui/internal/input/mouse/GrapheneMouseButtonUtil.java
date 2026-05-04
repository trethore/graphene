package tytoo.grapheneui.internal.input.mouse;

import org.lwjgl.glfw.GLFW;

public final class GrapheneMouseButtonUtil {
    public static final String DEVTOOLS_BUTTON_NONE = "none";
    public static final int DEVTOOLS_BUTTONS_NONE = 0;

    private static final String DEVTOOLS_BUTTON_BACK = "back";
    private static final String DEVTOOLS_BUTTON_FORWARD = "forward";
    private static final int DEVTOOLS_BUTTONS_LEFT = 1;
    private static final int DEVTOOLS_BUTTONS_RIGHT = 2;
    private static final int DEVTOOLS_BUTTONS_MIDDLE = 4;
    private static final int DEVTOOLS_BUTTONS_BACK = 8;
    private static final int DEVTOOLS_BUTTONS_FORWARD = 16;

    private GrapheneMouseButtonUtil() {
    }

    public static boolean isBrowserNavigationButton(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_4 || button == GLFW.GLFW_MOUSE_BUTTON_5;
    }

    public static boolean isExtraMouseButton(int button) {
        return button >= GLFW.GLFW_MOUSE_BUTTON_6 && button <= GLFW.GLFW_MOUSE_BUTTON_8;
    }

    public static String toDevToolsButton(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_4 -> DEVTOOLS_BUTTON_BACK;
            case GLFW.GLFW_MOUSE_BUTTON_5 -> DEVTOOLS_BUTTON_FORWARD;
            default -> DEVTOOLS_BUTTON_NONE;
        };
    }

    public static int toDevToolsButtonsBit(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_1 -> DEVTOOLS_BUTTONS_LEFT;
            case GLFW.GLFW_MOUSE_BUTTON_2 -> DEVTOOLS_BUTTONS_RIGHT;
            case GLFW.GLFW_MOUSE_BUTTON_3 -> DEVTOOLS_BUTTONS_MIDDLE;
            case GLFW.GLFW_MOUSE_BUTTON_4 -> DEVTOOLS_BUTTONS_BACK;
            case GLFW.GLFW_MOUSE_BUTTON_5 -> DEVTOOLS_BUTTONS_FORWARD;
            default -> DEVTOOLS_BUTTONS_NONE;
        };
    }
}
