package tytoo.grapheneui.internal.browser.render;

public final class GraphenePaintSnapshot {
    private final GraphenePaintFrameView mainFrame;
    private final GraphenePopupPaintFrameView popupFrame;

    GraphenePaintSnapshot(GraphenePaintFrameView mainFrame, GraphenePopupPaintFrameView popupFrame) {
        this.mainFrame = mainFrame;
        this.popupFrame = popupFrame;
    }

    public GraphenePaintFrameView mainFrame() {
        return mainFrame;
    }

    public GraphenePopupPaintFrameView popupFrame() {
        return popupFrame;
    }
}
