package io.github.trethore.graphene.internal.browser;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.trethore.graphene.api.browser.BrowserDirtyRegion;
import io.github.trethore.graphene.api.browser.BrowserFrame;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class GrapheneFrameUploadPlannerTest {
    @Test
    void requiresFullUploadWhenFrameSequenceIsNotConsecutive() {
        BrowserFrame frame = new BrowserFrame(
                10, 10, 3, List.of(new BrowserDirtyRegion(0, 0, 1, 1)), ByteBuffer.allocateDirect(400));

        assertTrue(GrapheneFrameUploadPlanner.shouldUploadFullFrame(frame, false));
        assertFalse(GrapheneFrameUploadPlanner.shouldUploadFullFrame(frame, true));
    }
}
