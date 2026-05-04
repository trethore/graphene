package tytoo.grapheneui.internal.mc;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@SuppressWarnings({"resource", "java:S2095"})
public final class McClient {
    private static final long NO_WINDOW_HANDLE = 0L;
    private static final GrapheneDebugLogger DEBUG_LOGGER = GrapheneDebugLogger.of(McClient.class);

    private McClient() {
    }

    public static Minecraft mc() {
        return Objects.requireNonNull(Minecraft.getInstance(), "Minecraft client is not available");
    }

    public static void execute(Runnable runnable) {
        mc().execute(Objects.requireNonNull(runnable, "runnable"));
    }

    public static void runOnMainThread(Runnable runnable) {
        Runnable task = Objects.requireNonNull(runnable, "runnable");
        Minecraft minecraft = mc();
        if (minecraft.isSameThread()) {
            task.run();
            return;
        }

        minecraft.execute(task);
    }

    public static <T> CompletableFuture<T> supplyOnMainThread(Supplier<T> supplier) {
        Supplier<T> task = Objects.requireNonNull(supplier, "supplier");
        CompletableFuture<T> future = new CompletableFuture<>();
        runOnMainThread(() -> {
            try {
                future.complete(task.get());
            } catch (RuntimeException exception) {
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    public static Screen currentScreen() {
        return mc().screen;
    }

    public static void setScreen(Screen screen) {
        mc().setScreen(screen);
    }

    public static Overlay currentOverlay() {
        return mc().getOverlay();
    }

    public static void setOverlay(Overlay overlay) {
        mc().setOverlay(overlay);
    }

    public static int windowWidth() {
        return mc().getWindow().getWidth();
    }

    public static int windowHeight() {
        return mc().getWindow().getHeight();
    }

    public static int guiScaledWidth() {
        return mc().getWindow().getGuiScaledWidth();
    }

    public static int guiScaledHeight() {
        return mc().getWindow().getGuiScaledHeight();
    }

    public static long nativeWindowHandle() {
        long glfwWindowHandle = mc().getWindow().handle();

        try {
            if (GraphenePlatform.isWindows()) {
                return GLFWNativeWin32.glfwGetWin32Window(glfwWindowHandle);
            }

            if (GraphenePlatform.isMac()) {
                return GLFWNativeCocoa.glfwGetCocoaWindow(glfwWindowHandle);
            }

            if (GraphenePlatform.isLinux()) {
                return GLFWNativeX11.glfwGetX11Window(glfwWindowHandle);
            }
        } catch (RuntimeException | UnsatisfiedLinkError exception) {
            DEBUG_LOGGER.debugIfEnabled(logger -> logger.debug(
                    "Failed to resolve native platform window handle from GLFW window {}",
                    glfwWindowHandle,
                    exception
            ));
        }

        return NO_WINDOW_HANDLE;
    }

    public static void setCursorDisabled(boolean disabled) {
        Window window = mc().getWindow();
        double centerX = window.getScreenWidth() / 2.0D;
        double centerY = window.getScreenHeight() / 2.0D;
        int cursorMode = disabled ? InputConstants.CURSOR_DISABLED : InputConstants.CURSOR_NORMAL;
        InputConstants.grabOrReleaseMouse(window, cursorMode, centerX, centerY);
    }
}
