package tytoo.grapheneui.internal.mc;

import net.minecraft.resources.Identifier;
import tytoo.grapheneui.api.GrapheneCore;

public final class McIdentifiers {
    private McIdentifiers() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GrapheneCore.ID, path);
    }
}
