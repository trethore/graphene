package tytoo.grapheneui.browser;

import java.util.*;

public final class GrapheneBrowserSurfaceManager {
    private static final String OWNER_NAME = "owner";
    private static final String SURFACE_NAME = "surface";
    private static final String BUILDER_NAME = "builder";

    private final Object lock = new Object();
    private final Map<Object, Set<BrowserSurface>> surfacesByOwner = new IdentityHashMap<>();
    private final Map<BrowserSurface, Object> ownersBySurface = new IdentityHashMap<>();

    public GrapheneBrowserSurfaceManager() {
        // Intentionally empty: all state is initialized via field initializers.
    }

    private static void closeSurfaces(List<BrowserSurface> surfacesToClose) {
        for (BrowserSurface surface : surfacesToClose) {
            surface.close();
        }
    }

    public BrowserSurface register(Object owner, BrowserSurface surface) {
        Objects.requireNonNull(owner, OWNER_NAME);
        Objects.requireNonNull(surface, SURFACE_NAME);

        synchronized (lock) {
            Object previousOwner = ownersBySurface.put(surface, owner);
            if (previousOwner != null && previousOwner != owner) {
                removeSurfaceFromOwner(previousOwner, surface);
            }

            surfacesByOwner.computeIfAbsent(owner, ignoredOwner -> new LinkedHashSet<>()).add(surface);
        }

        return surface;
    }

    public BrowserSurface createAndRegister(Object owner, BrowserSurface.Builder builder) {
        Objects.requireNonNull(owner, OWNER_NAME);
        Objects.requireNonNull(builder, BUILDER_NAME);
        return register(owner, builder.build());
    }

    public void unregister(Object owner, BrowserSurface surface) {
        Objects.requireNonNull(owner, OWNER_NAME);
        Objects.requireNonNull(surface, SURFACE_NAME);

        synchronized (lock) {
            Object trackedOwner = ownersBySurface.get(surface);
            if (trackedOwner != owner) {
                return;
            }

            ownersBySurface.remove(surface);
            removeSurfaceFromOwner(owner, surface);
        }
    }

    public void closeOwner(Object owner) {
        Objects.requireNonNull(owner, OWNER_NAME);
        closeSurfaces(extractSurfacesForOwner(owner));
    }

    public void closeAll() {
        List<BrowserSurface> surfacesToClose = new ArrayList<>();
        synchronized (lock) {
            for (Set<BrowserSurface> surfaces : surfacesByOwner.values()) {
                surfacesToClose.addAll(surfaces);
            }
            surfacesByOwner.clear();
            ownersBySurface.clear();
        }

        closeSurfaces(surfacesToClose);
    }

    public int trackedSurfaceCount(Object owner) {
        Objects.requireNonNull(owner, OWNER_NAME);

        synchronized (lock) {
            Set<BrowserSurface> surfaces = surfacesByOwner.get(owner);
            return surfaces == null ? 0 : surfaces.size();
        }
    }

    private List<BrowserSurface> extractSurfacesForOwner(Object owner) {
        synchronized (lock) {
            Set<BrowserSurface> surfaces = surfacesByOwner.remove(owner);
            if (surfaces == null || surfaces.isEmpty()) {
                return List.of();
            }

            for (BrowserSurface surface : surfaces) {
                ownersBySurface.remove(surface);
            }

            return new ArrayList<>(surfaces);
        }
    }

    private void removeSurfaceFromOwner(Object owner, BrowserSurface surface) {
        Set<BrowserSurface> ownerSurfaces = surfacesByOwner.get(owner);
        if (ownerSurfaces == null) {
            return;
        }

        ownerSurfaces.remove(surface);
        if (ownerSurfaces.isEmpty()) {
            surfacesByOwner.remove(owner);
        }
    }
}
