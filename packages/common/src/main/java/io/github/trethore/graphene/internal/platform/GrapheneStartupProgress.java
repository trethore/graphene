package io.github.trethore.graphene.internal.platform;

import java.util.Locale;

public final class GrapheneStartupProgress {
    private volatile String stage = "INITIALIZING";
    private volatile double progress = -1.0;

    public void update(String stage, double progress) {
        this.stage = stage == null || stage.isBlank() ? "INITIALIZING" : stage;
        this.progress = progress;
    }

    public String displayStage() {
        return stage.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public int fillWidth(int availableWidth, long timestampMillis) {
        if (availableWidth <= 0) {
            throw new IllegalArgumentException("availableWidth must be positive");
        }
        if (progress < 0.0) {
            return (int) ((timestampMillis / 8L) % availableWidth);
        }
        return (int) Math.round(availableWidth * Math.clamp(progress, 0.0, 1.0));
    }
}
