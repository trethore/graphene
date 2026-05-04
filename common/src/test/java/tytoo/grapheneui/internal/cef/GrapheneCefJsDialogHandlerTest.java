package tytoo.grapheneui.internal.cef;

import org.cef.handler.CefJSDialogHandler;
import org.cef.misc.BoolRef;
import org.junit.jupiter.api.Test;
import tytoo.grapheneui.internal.cef.alert.GrapheneJsDialogDispatcher;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GrapheneCefJsDialogHandlerTest {
    private static org.cef.browser.CefBrowser testBrowser() {
        return (org.cef.browser.CefBrowser) Proxy.newProxyInstance(
                GrapheneCefJsDialogHandlerTest.class.getClassLoader(),
                new Class<?>[]{org.cef.browser.CefBrowser.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("toString")) {
                        return "TestCefBrowser";
                    }

                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static GrapheneJsDialogDispatcher noOpDispatcher() {
        return new GrapheneJsDialogDispatcher() {
            @Override
            public void enqueueDialog(
                    org.cef.browser.CefBrowser browser,
                    String originUrl,
                    CefJSDialogHandler.JSDialogType dialogType,
                    String messageText,
                    String defaultPromptText,
                    org.cef.callback.CefJSDialogCallback callback
            ) {
            }

            @Override
            public void enqueueBeforeUnloadDialog(
                    org.cef.browser.CefBrowser browser,
                    String messageText,
                    boolean isReload,
                    org.cef.callback.CefJSDialogCallback callback
            ) {
            }

            @Override
            public void resetDialogState(org.cef.browser.CefBrowser browser) {
            }

            @Override
            public void onDialogClosed(org.cef.browser.CefBrowser browser) {
            }
        };
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }

        if (returnType == boolean.class) {
            return false;
        }

        if (returnType == byte.class) {
            return (byte) 0;
        }

        if (returnType == short.class) {
            return (short) 0;
        }

        if (returnType == int.class) {
            return 0;
        }

        if (returnType == long.class) {
            return 0L;
        }

        if (returnType == float.class) {
            return 0.0F;
        }

        if (returnType == double.class) {
            return 0.0D;
        }

        if (returnType == char.class) {
            return '\0';
        }

        return null;
    }

    @Test
    void suppressesJsDialogWhenCallbackIsMissing() {
        GrapheneCefJsDialogHandler handler = new GrapheneCefJsDialogHandler(noOpDispatcher());
        BoolRef suppressMessage = new BoolRef(false);

        boolean handled = handler.onJSDialog(
                null,
                "https://example.invalid",
                CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT,
                "Hello",
                "",
                null,
                suppressMessage
        );

        assertFalse(handled);
        assertTrue(suppressMessage.get());
    }

    @Test
    void suppressesBeforeUnloadDialogWhenCallbackIsMissing() {
        GrapheneCefJsDialogHandler handler = new GrapheneCefJsDialogHandler(noOpDispatcher());

        boolean handled = handler.onBeforeUnloadDialog(testBrowser(), "Leave page?", false, null);

        assertTrue(handled);
    }

    @Test
    void doesNotHandleBeforeUnloadDialogWhenBrowserIsMissing() {
        GrapheneCefJsDialogHandler handler = new GrapheneCefJsDialogHandler(noOpDispatcher());

        boolean handled = handler.onBeforeUnloadDialog(null, "Leave page?", false, null);

        assertFalse(handled);
    }
}
