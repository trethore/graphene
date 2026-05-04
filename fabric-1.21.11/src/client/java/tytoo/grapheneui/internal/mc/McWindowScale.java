package tytoo.grapheneui.internal.mc;

public final class McWindowScale {
    private McWindowScale() {
    }

    public static double getScaleX() {
        return McClient.windowWidth() / (double) McClient.guiScaledWidth();
    }

    public static double getScaleY() {
        return McClient.windowHeight() / (double) McClient.guiScaledHeight();
    }
}
