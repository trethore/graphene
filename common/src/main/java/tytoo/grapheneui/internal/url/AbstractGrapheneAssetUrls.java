package tytoo.grapheneui.internal.url;

import tytoo.grapheneui.api.url.GrapheneAssetUrls;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.util.Objects;

public abstract class AbstractGrapheneAssetUrls implements GrapheneAssetUrls {
    protected static final String ASSET_HOST = "assets";
    protected static final String PATH_DELIMITER = "/";
    private final String defaultNamespace;

    protected AbstractGrapheneAssetUrls(String defaultNamespace) {
        this.defaultNamespace = normalizeNamespace(defaultNamespace);
    }

    protected abstract String rootPrefix();

    @Override
    public final String asset(String path) {
        return asset(defaultNamespace, path);
    }

    @Override
    public final String asset(String namespace, String path) {
        String normalizedNamespace = normalizeNamespace(namespace);
        String normalizedPath = normalizePath(path);
        String resolvedRootPrefix = Objects.requireNonNull(rootPrefix(), "rootPrefix");
        return resolvedRootPrefix + normalizedNamespace + PATH_DELIMITER + normalizedPath;
    }

    @Override
    public final String asset(Object assetId) {
        Object identifier = Objects.requireNonNull(assetId, "assetId");
        return asset(readIdentifierPart(identifier, "getNamespace"), readIdentifierPart(identifier, "getPath"));
    }

    private String readIdentifierPart(Object identifier, String methodName) {
        try {
            Method method = identifier.getClass().getMethod(methodName);
            method.setAccessible(true);
            Object value = method.invoke(identifier);
            if (value instanceof String stringValue) {
                return stringValue;
            }
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            throw new IllegalArgumentException(
                    "assetId must expose public getNamespace() and getPath() String methods",
                    exception
            );
        }

        throw new IllegalArgumentException("assetId " + methodName + "() must return String");
    }

    protected final String normalizePath(String path) {
        String normalizedPath = Objects.requireNonNull(path, "path").trim();
        while (normalizedPath.startsWith(PATH_DELIMITER)) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }

    protected final String normalizeNamespace(String namespace) {
        String normalizedNamespace = Objects.requireNonNull(namespace, "namespace").trim();
        while (normalizedNamespace.startsWith(PATH_DELIMITER)) {
            normalizedNamespace = normalizedNamespace.substring(1);
        }

        while (normalizedNamespace.endsWith(PATH_DELIMITER)) {
            normalizedNamespace = normalizedNamespace.substring(0, normalizedNamespace.length() - 1);
        }

        return normalizedNamespace;
    }
}
