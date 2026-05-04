package tytoo.grapheneui.internal.browser.input;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertSame;

final class GrapheneCursorMapperTest {
    @Test
    void mapsCommonCefCursorsToMinecraftCursors() {
        assertSame(CursorTypes.CROSSHAIR, GrapheneCursorMapper.toMinecraftCursor(Cursor.CROSSHAIR_CURSOR));
        assertSame(CursorTypes.IBEAM, GrapheneCursorMapper.toMinecraftCursor(Cursor.TEXT_CURSOR));
        assertSame(CursorTypes.POINTING_HAND, GrapheneCursorMapper.toMinecraftCursor(Cursor.HAND_CURSOR));
    }

    @Test
    void mapsResizeCursorsByAxis() {
        assertSame(CursorTypes.RESIZE_NS, GrapheneCursorMapper.toMinecraftCursor(Cursor.N_RESIZE_CURSOR));
        assertSame(CursorTypes.RESIZE_NS, GrapheneCursorMapper.toMinecraftCursor(Cursor.S_RESIZE_CURSOR));
        assertSame(CursorTypes.RESIZE_EW, GrapheneCursorMapper.toMinecraftCursor(Cursor.E_RESIZE_CURSOR));
        assertSame(CursorTypes.RESIZE_EW, GrapheneCursorMapper.toMinecraftCursor(Cursor.W_RESIZE_CURSOR));
    }

    @Test
    void mapsDiagonalAndMoveCursorsToResizeAll() {
        assertSame(CursorTypes.RESIZE_ALL, GrapheneCursorMapper.toMinecraftCursor(Cursor.NE_RESIZE_CURSOR));
        assertSame(CursorTypes.RESIZE_ALL, GrapheneCursorMapper.toMinecraftCursor(Cursor.NW_RESIZE_CURSOR));
        assertSame(CursorTypes.RESIZE_ALL, GrapheneCursorMapper.toMinecraftCursor(Cursor.SE_RESIZE_CURSOR));
        assertSame(CursorTypes.RESIZE_ALL, GrapheneCursorMapper.toMinecraftCursor(Cursor.SW_RESIZE_CURSOR));
        assertSame(CursorTypes.RESIZE_ALL, GrapheneCursorMapper.toMinecraftCursor(Cursor.MOVE_CURSOR));
    }

    @Test
    void mapsUnknownCursorToArrow() {
        assertSame(CursorTypes.ARROW, GrapheneCursorMapper.toMinecraftCursor(Cursor.WAIT_CURSOR));
    }
}
