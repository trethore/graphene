package tytoo.grapheneui.internal.browser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class GrapheneFocusUtil {
    private final Consumer<Boolean> nativeFocusUpdater;
    private final List<Consumer<Boolean>> focusListeners = new ArrayList<>();
    private volatile boolean focused;
    private boolean focusUpdateInProgress;
    private boolean focusUpdateTarget;

    public GrapheneFocusUtil(Consumer<Boolean> nativeFocusUpdater) {
        this.nativeFocusUpdater = Objects.requireNonNull(nativeFocusUpdater, "nativeFocusUpdater");
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        List<Consumer<Boolean>> listeners = updateFocusedState(focused);

        for (Consumer<Boolean> listener : listeners) {
            listener.accept(focused);
        }
    }

    public synchronized void addFocusListener(Consumer<Boolean> focusListener) {
        focusListeners.add(Objects.requireNonNull(focusListener, "focusListener"));
    }

    public synchronized void syncNativeFocus() {
        applyNativeFocus(true);
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
