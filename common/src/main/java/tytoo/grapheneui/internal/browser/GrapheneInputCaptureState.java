package tytoo.grapheneui.internal.browser;

public final class GrapheneInputCaptureState {
    private boolean cursorCaptured;
    private EscapeMode escapeMode = EscapeMode.MINECRAFT;

    public boolean isActive() {
        return cursorCaptured || escapeMode != EscapeMode.MINECRAFT;
    }

    public boolean isCursorCaptured() {
        return cursorCaptured;
    }

    public EscapeMode escapeMode() {
        return escapeMode;
    }

    public void capture(boolean cursorCaptured, EscapeMode escapeMode) {
        this.cursorCaptured = cursorCaptured;
        this.escapeMode = escapeMode;
    }

    public void release() {
        cursorCaptured = false;
        escapeMode = EscapeMode.MINECRAFT;
    }

    public enum EscapeMode {
        MINECRAFT,
        RELEASE,
        PASSTHROUGH
    }
}
