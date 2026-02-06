package tytoo.grapheneui.browser;

import java.awt.event.KeyEvent;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

final class GrapheneScancodeInjector {
    private static final long SCANCODE_UNSUPPORTED = -1L;

    private final Object unsafe;
    private final MethodHandle putLongHandle;
    private final long scancodeOffset;

    private GrapheneScancodeInjector(Object unsafe, MethodHandle putLongHandle, long scancodeOffset) {
        this.unsafe = unsafe;
        this.putLongHandle = putLongHandle;
        this.scancodeOffset = scancodeOffset;
    }

    // This is digesting, but it allows use to inject native scancodes into KeyEvent instances without needing to use JNI or any native code.
    static GrapheneScancodeInjector create() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            MethodHandles.Lookup unsafeLookup = MethodHandles.privateLookupIn(unsafeClass, MethodHandles.lookup());
            Object unsafe = unsafeLookup.findStaticVarHandle(unsafeClass, "theUnsafe", unsafeClass).get();

            MethodHandle objectFieldOffsetHandle = unsafeLookup.findVirtual(
                    unsafeClass,
                    "objectFieldOffset",
                    MethodType.methodType(long.class, Field.class)
            );
            MethodHandle putLongHandle = unsafeLookup.findVirtual(
                    unsafeClass,
                    "putLong",
                    MethodType.methodType(void.class, Object.class, long.class, long.class)
            );

            Field scancodeField = KeyEvent.class.getDeclaredField("scancode");
            long scancodeOffset = (long) objectFieldOffsetHandle.invoke(unsafe, scancodeField);
            return new GrapheneScancodeInjector(unsafe, putLongHandle, scancodeOffset);
        } catch (Throwable _) {
            return new GrapheneScancodeInjector(null, null, SCANCODE_UNSUPPORTED);
        }
    }

    void inject(KeyEvent event, int scanCode) {
        if (!isSupported()) {
            return;
        }

        try {
            putLongHandle.invoke(unsafe, event, scancodeOffset, scanCode & 0xFFL);
        } catch (Throwable _) {
            // Keep dispatching key events even when native scancode injection fails.
        }
    }

    private boolean isSupported() {
        return unsafe != null && putLongHandle != null && scancodeOffset >= 0L;
    }
}
