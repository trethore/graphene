package tytoo.grapheneui.internal.browser.cef;

import java.util.function.Consumer;

public final class GrapheneBrowserFocusGuard {
    private final Object lock = new Object();

    private boolean updateInProgress;
    private boolean updateTarget;

    public void apply(boolean focused, Consumer<Boolean> nativeFocusUpdater) {
        boolean previousInProgress;
        boolean previousTarget;
        synchronized (lock) {
            if (updateInProgress && updateTarget == focused) {
                return;
            }

            previousInProgress = updateInProgress;
            previousTarget = updateTarget;
            updateInProgress = true;
            updateTarget = focused;
        }

        try {
            nativeFocusUpdater.accept(focused);
        } finally {
            synchronized (lock) {
                updateInProgress = previousInProgress;
                updateTarget = previousTarget;
            }
        }
    }
}
