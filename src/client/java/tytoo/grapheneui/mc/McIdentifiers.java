package tytoo.grapheneui.mc;

import net.minecraft.resources.Identifier;
import tytoo.grapheneui.GrapheneCore;

public final class McIdentifiers {
    private McIdentifiers() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(GrapheneCore.ID, path);
    }
}
