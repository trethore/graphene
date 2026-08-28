package io.github.trethore.graphene.internal.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GrapheneStartupProgressTest {
    @Test
    void normalizesAndFormatsStages() {
        GrapheneStartupProgress progress = new GrapheneStartupProgress();

        progress.update("LOADING_RUNTIME", 0.5);

        assertEquals("loading runtime", progress.displayStage());
        assertEquals(50, progress.fillWidth(100, 0));
    }

    @Test
    void animatesIndeterminateProgress() {
        GrapheneStartupProgress progress = new GrapheneStartupProgress();

        progress.update(null, -1.0);

        assertEquals("initializing", progress.displayStage());
        assertEquals(5, progress.fillWidth(100, 40));
    }
}
