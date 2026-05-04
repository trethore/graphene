package tytoo.grapheneui.internal.core;

import java.util.Objects;

public final class GrapheneNamespaceValidator {
    private GrapheneNamespaceValidator() {
    }

    public static String normalizeNamespace(String namespace, String name) {
        String normalizedNamespace = Objects.requireNonNull(namespace, name).trim();
        if (normalizedNamespace.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        if (!isValidNamespace(normalizedNamespace)) {
            throw new IllegalArgumentException(
                    name + " must be a valid namespace using lowercase letters, digits, '.', '_' or '-'"
            );
        }

        return normalizedNamespace;
    }

    private static boolean isValidNamespace(String namespace) {
        for (int index = 0; index < namespace.length(); index++) {
            char character = namespace.charAt(index);
            if (!isValidNamespaceCharacter(character)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isValidNamespaceCharacter(char character) {
        return character == '_'
                || character == '-'
                || character == '.'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9';
    }
}
