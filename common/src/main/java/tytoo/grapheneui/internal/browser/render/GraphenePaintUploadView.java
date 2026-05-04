package tytoo.grapheneui.internal.browser.render;

import java.awt.*;
import java.nio.ByteBuffer;

public interface GraphenePaintUploadView {
    ByteBuffer buffer();

    int width();

    int height();

    Rectangle[] dirtyRects();

    boolean fullReRender();

    long frameVersion();
}
