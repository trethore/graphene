package tytoo.grapheneui.internal.logging;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class GrapheneDebugLogSelector {
    private static final String PROPERTY_NAME = "graphene.debug";
    private static final GrapheneDebugLogSelector ACTIVE_SELECTOR = fromRawSelector(System.getProperty(PROPERTY_NAME));

    private final boolean wildcard;
    private final List<String> packagePrefixes;

    private GrapheneDebugLogSelector(boolean wildcard, List<String> packagePrefixes) {
        this.wildcard = wildcard;
        this.packagePrefixes = packagePrefixes;
    }

    public static boolean isEnabledFor(Class<?> ownerClass) {
        Objects.requireNonNull(ownerClass, "ownerClass");
        return ACTIVE_SELECTOR.matches(ownerClass.getName());
    }

    static GrapheneDebugLogSelector fromRawSelector(String rawSelector) {
        if (rawSelector == null || rawSelector.isBlank()) {
            return disabled();
        }

        Set<String> prefixes = new LinkedHashSet<>();
        String[] selectorParts = rawSelector.split(",");
        for (String selectorPart : selectorParts) {
            String normalizedPart = normalizeToken(selectorPart);
            if (normalizedPart == null) {
                continue;
            }

            if ("*".equals(normalizedPart)) {
                return wildcard();
            }

            prefixes.add(normalizedPart);
        }

        if (prefixes.isEmpty()) {
            return disabled();
        }

        return new GrapheneDebugLogSelector(false, List.copyOf(prefixes));
    }

    private static String normalizeToken(String selectorPart) {
        String normalizedPart = selectorPart == null ? "" : selectorPart.trim();
        if (normalizedPart.isEmpty()) {
            return null;
        }

        while (normalizedPart.endsWith(".")) {
            normalizedPart = normalizedPart.substring(0, normalizedPart.length() - 1);
        }

        if (normalizedPart.isEmpty()) {
            return null;
        }

        return normalizedPart;
    }

    private static GrapheneDebugLogSelector disabled() {
        return new GrapheneDebugLogSelector(false, List.of());
    }

    private static GrapheneDebugLogSelector wildcard() {
        return new GrapheneDebugLogSelector(true, List.of());
    }

    boolean matches(String classOrPackageName) {
        Objects.requireNonNull(classOrPackageName, "classOrPackageName");
        if (wildcard) {
            return true;
        }

        for (String packagePrefix : packagePrefixes) {
            if (classOrPackageName.equals(packagePrefix) || classOrPackageName.startsWith(packagePrefix + ".")) {
                return true;
            }
        }

        return false;
    }
}
