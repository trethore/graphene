package io.github.trethore.graphene.fabric.internal.platform;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

final class TinyFdMessageBoxAdapter {
    private TinyFdMessageBoxAdapter() {}

    static boolean show(String title, String message, String dialogType, String iconType) {
        return TinyFileDialogs.tinyfd_messageBox(title, message, dialogType, iconType, true);
    }
}
