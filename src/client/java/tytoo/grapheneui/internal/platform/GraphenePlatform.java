package tytoo.grapheneui.internal.platform;

import java.util.Locale;

@SuppressWarnings("unused") // This class is used for platform detection.
public final class GraphenePlatform {
    private static final String OS_NAME = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
    private static final String XDG_SESSION_TYPE = System.getenv("XDG_SESSION_TYPE");
    private static final String WAYLAND_DISPLAY = System.getenv("WAYLAND_DISPLAY");

    private GraphenePlatform() {
    }

    public static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    public static boolean isMac() {
        return OS_NAME.contains("mac");
    }

    public static boolean isLinux() {
        return OS_NAME.contains("linux");
    }

    public static boolean isWaylandSession() {
        if (!isLinux()) {
            return false;
        }

        if (WAYLAND_DISPLAY != null && !WAYLAND_DISPLAY.isBlank()) {
            return true;
        }

        return XDG_SESSION_TYPE != null && XDG_SESSION_TYPE.equalsIgnoreCase("wayland");
    }
}
