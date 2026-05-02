package tytoo.grapheneui.internal.browser.input;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

import java.awt.*;

public final class GrapheneCursorMapper {
    private GrapheneCursorMapper() {
    }

    public static CursorType toMinecraftCursor(int cursorType) {
        return switch (cursorType) {
            case Cursor.CROSSHAIR_CURSOR -> CursorTypes.CROSSHAIR;
            case Cursor.TEXT_CURSOR -> CursorTypes.IBEAM;
            case Cursor.HAND_CURSOR -> CursorTypes.POINTING_HAND;
            case Cursor.N_RESIZE_CURSOR,
                 Cursor.S_RESIZE_CURSOR -> CursorTypes.RESIZE_NS;
            case Cursor.E_RESIZE_CURSOR,
                 Cursor.W_RESIZE_CURSOR -> CursorTypes.RESIZE_EW;
            case Cursor.NE_RESIZE_CURSOR,
                 Cursor.NW_RESIZE_CURSOR,
                 Cursor.SE_RESIZE_CURSOR,
                 Cursor.SW_RESIZE_CURSOR,
                 Cursor.MOVE_CURSOR -> CursorTypes.RESIZE_ALL;
            default -> CursorTypes.ARROW;
        };
    }
}
