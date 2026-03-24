package tytoo.grapheneui.internal.cef.startup;

import net.minecraft.util.Mth;

import java.util.Objects;

public final class GrapheneNativeDownloadState {
    private final String platformIdentifier;
    private volatile boolean active;
    private volatile float progress;

    public GrapheneNativeDownloadState(String platformIdentifier) {
        this.platformIdentifier = Objects.requireNonNull(platformIdentifier, "platformIdentifier").trim();
    }

    public String platformIdentifier() {
        return platformIdentifier;
    }

    public boolean isActive() {
        return active;
    }

    public float progress() {
        return progress;
    }

    public void beginDownload(float percent) {
        active = true;
        updateProgress(percent);
    }

    public void updateProgress(float percent) {
        if (percent < 0.0F) {
            return;
        }

        progress = Mth.clamp(percent / 100.0F, 0.0F, 1.0F);
    }

    public void markPostDownloadWork() {
        active = true;
        progress = 1.0F;
    }

    public void reset() {
        active = false;
        progress = 0.0F;
    }
}
