package io.github.trethore.graphene.fabric.internal.platform;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.glfw.GLFWNativeX11;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GlfwNativeWindowHandle {
  private static final Logger LOGGER = LoggerFactory.getLogger(GlfwNativeWindowHandle.class);

  private GlfwNativeWindowHandle() {}

  static long resolve(long glfwWindowHandle) {
    if (glfwWindowHandle == MemoryUtil.NULL) {
      return MemoryUtil.NULL;
    }
    long nativeWindowHandle =
        switch (GLFW.glfwGetPlatform()) {
          case GLFW.GLFW_PLATFORM_WIN32 -> GLFWNativeWin32.glfwGetWin32Window(glfwWindowHandle);
          case GLFW.GLFW_PLATFORM_COCOA -> resolveCocoaView(glfwWindowHandle);
          case GLFW.GLFW_PLATFORM_X11 -> GLFWNativeX11.glfwGetX11Window(glfwWindowHandle);
          default -> MemoryUtil.NULL; // Unsupported or unknown GLFW platform.
        };
    if (nativeWindowHandle == MemoryUtil.NULL) {
      LOGGER.warn("No supported native parent window is available for CEF browser parenting");
    }
    return nativeWindowHandle;
  }

  private static long resolveCocoaView(long glfwWindowHandle) {
    long cocoaWindow = GLFWNativeCocoa.glfwGetCocoaWindow(glfwWindowHandle);
    if (cocoaWindow == MemoryUtil.NULL) {
      return MemoryUtil.NULL;
    }
    return JNI.invokePPP(
        cocoaWindow,
        ObjCRuntime.sel_getUid("contentView"),
        ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend"));
  }
}
