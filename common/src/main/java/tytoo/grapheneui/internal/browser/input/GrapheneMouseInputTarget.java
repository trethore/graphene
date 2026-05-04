package tytoo.grapheneui.internal.browser.input;

public interface GrapheneMouseInputTarget {
    void mouseMoved(int x, int y, int modifiers);

    void mouseDragged(double x, double y, int button);

    void mouseInteracted(int x, int y, int modifiers, int button, boolean pressed, int clickCount);

    void mouseScrolled(int x, int y, int modifiers, int amount, int rotation);

    void navigationButtonInteracted(
            int x,
            int y,
            int modifiers,
            int button,
            boolean pressed,
            int clickCount,
            int buttons
    );

    void dragUpdated(int x, int y, int cefModifiers);

    void dragCompleted(int x, int y, int cefModifiers);

    void cancelActiveDrag();

    double getZoomLevel();

    void setZoomLevel(double zoomLevel);
}
