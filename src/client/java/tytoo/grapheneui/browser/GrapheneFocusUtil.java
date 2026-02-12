package tytoo.grapheneui.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class GrapheneFocusUtil {
    private final Consumer<Boolean> nativeFocusUpdater;
    private final List<Consumer<Boolean>> focusListeners = new ArrayList<>();
    private volatile boolean focused;
    private boolean focusUpdateInProgress;
    private boolean focusUpdateTarget;

    GrapheneFocusUtil(Consumer<Boolean> nativeFocusUpdater) {
        this.nativeFocusUpdater = Objects.requireNonNull(nativeFocusUpdater, "nativeFocusUpdater");
    }

    boolean isFocused() {
        return focused;
    }

    synchronized void addFocusListener(Consumer<Boolean> focusListener) {
        focusListeners.add(Objects.requireNonNull(focusListener, "focusListener"));
    }

    synchronized void syncNativeFocus() {
        applyNativeFocus(true);
    }

    void setFocused(boolean focused) {
        List<Consumer<Boolean>> listeners = updateFocusedState(focused);

        for (Consumer<Boolean> listener : listeners) {
            listener.accept(focused);
        }
    }

    private synchronized List<Consumer<Boolean>> updateFocusedState(boolean focused) {
        this.focused = focused;
        applyNativeFocus(focused);
        return List.copyOf(focusListeners);
    }

    private void applyNativeFocus(boolean focused) {
        if (focusUpdateInProgress && focusUpdateTarget == focused) {
            return;
        }

        boolean previousInProgress = focusUpdateInProgress;
        boolean previousTarget = focusUpdateTarget;
        focusUpdateInProgress = true;
        focusUpdateTarget = focused;
        try {
            nativeFocusUpdater.accept(focused);
        } finally {
            focusUpdateInProgress = previousInProgress;
            focusUpdateTarget = previousTarget;
        }
    }
}
