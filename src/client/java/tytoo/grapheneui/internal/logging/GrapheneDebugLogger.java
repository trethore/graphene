package tytoo.grapheneui.internal.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

public final class GrapheneDebugLogger {
    private final Logger logger;
    private final boolean enabled;

    private GrapheneDebugLogger(Class<?> ownerClass) {
        Class<?> validatedOwnerClass = Objects.requireNonNull(ownerClass, "ownerClass");
        this.logger = LoggerFactory.getLogger(validatedOwnerClass);
        this.enabled = GrapheneDebugLogSelector.isEnabledFor(validatedOwnerClass);
    }

    public static GrapheneDebugLogger of(Class<?> ownerClass) {
        return new GrapheneDebugLogger(ownerClass);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void debug(String message) {
        if (enabled) {
            logger.debug(message);
        }
    }

    public void debug(String message, Object argument) {
        if (enabled) {
            logger.debug(message, argument);
        }
    }

    public void debug(String message, Object argumentOne, Object argumentTwo) {
        if (enabled) {
            logger.debug(message, argumentOne, argumentTwo);
        }
    }

    public void debug(String message, Object... arguments) {
        if (enabled) {
            logger.debug(message, arguments);
        }
    }

    public void debug(String message, Throwable throwable) {
        if (enabled) {
            logger.debug(message, throwable);
        }
    }

    public void debugIfEnabled(Consumer<Logger> debugAction) {
        Objects.requireNonNull(debugAction, "debugAction");
        if (enabled) {
            debugAction.accept(logger);
        }
    }
}
