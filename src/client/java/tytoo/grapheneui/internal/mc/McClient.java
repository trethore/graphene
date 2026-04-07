package tytoo.grapheneui.internal.mc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.macosx.ObjCRuntime;
import tytoo.grapheneui.internal.logging.GrapheneDebugLogger;
import tytoo.grapheneui.internal.platform.GraphenePlatform;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.lwjgl.system.JNI.invokePPP;
import static org.lwjgl.system.JNI.invokePPPZ;

@SuppressWarnings({"resource", "java:S2095", "unused"})
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

    public static boolean isOnMainThread() {
        return mc().isSameThread();
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
                return nativeMacViewHandle(glfwWindowHandle);
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

    private static long nativeMacViewHandle(long glfwWindowHandle) {
        long cocoaWindowHandle = GLFWNativeCocoa.glfwGetCocoaWindow(glfwWindowHandle);
        if (cocoaWindowHandle == NO_WINDOW_HANDLE) {
            return NO_WINDOW_HANDLE;
        }

        if (!respondsToContentViewSelector(cocoaWindowHandle)) {
            DEBUG_LOGGER.debugIfEnabled(logger -> logger.debug(
                    "GLFW Cocoa window {} does not expose contentView; returning no handle",
                    cocoaWindowHandle
            ));
            return NO_WINDOW_HANDLE;
        }

        long contentViewHandle = invokePPP(cocoaWindowHandle, MacObjc.SELECTOR_CONTENT_VIEW, MacObjc.OBJC_MSG_SEND);
        if (contentViewHandle != NO_WINDOW_HANDLE) {
            return contentViewHandle;
        }

        DEBUG_LOGGER.debugIfEnabled(logger -> logger.debug(
                "Failed to resolve NSView contentView from Cocoa window {}; returning no handle",
                cocoaWindowHandle
        ));
        return NO_WINDOW_HANDLE;
    }

    private static boolean respondsToContentViewSelector(long objectHandle) {
        return objectHandle != NO_WINDOW_HANDLE
                && invokePPPZ(
                        objectHandle,
                        MacObjc.SELECTOR_RESPONDS_TO_SELECTOR,
                        MacObjc.SELECTOR_CONTENT_VIEW,
                        MacObjc.OBJC_MSG_SEND
                );
    }

    public static void registerTexture(Identifier textureId, DynamicTexture dynamicTexture) {
        mc().getTextureManager().register(textureId, dynamicTexture);
    }

    public static void releaseTexture(Identifier textureId) {
        mc().getTextureManager().release(textureId);
    }

    private static final class MacObjc {
        private static final long OBJC_MSG_SEND = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");
        private static final long SELECTOR_CONTENT_VIEW = ObjCRuntime.sel_getUid("contentView");
        private static final long SELECTOR_RESPONDS_TO_SELECTOR = ObjCRuntime.sel_getUid("respondsToSelector:");

        private MacObjc() {
        }
    }
}
