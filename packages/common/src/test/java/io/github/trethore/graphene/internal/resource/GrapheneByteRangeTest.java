package io.github.trethore.graphene.internal.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GrapheneByteRangeTest {
    @Test
    void resolvesBoundedRange() {
        GrapheneByteRange.Resolution resolution = GrapheneByteRange.resolve("bytes=2-5", 10);

        assertEquals(GrapheneByteRange.Status.PARTIAL, resolution.status());
        assertEquals(2, resolution.startInclusive());
        assertEquals(6, resolution.endExclusive());
        assertEquals(4, resolution.responseLength());
        assertEquals("bytes 2-5/10", resolution.contentRange());
    }

    @Test
    void resolvesOpenEndedAndSuffixRanges() {
        GrapheneByteRange.Resolution openEnded = GrapheneByteRange.resolve("bytes=6-", 10);
        GrapheneByteRange.Resolution suffix = GrapheneByteRange.resolve("bytes=-3", 10);

        assertEquals(GrapheneByteRange.Status.PARTIAL, openEnded.status());
        assertEquals(6, openEnded.startInclusive());
        assertEquals(10, openEnded.endExclusive());
        assertEquals(GrapheneByteRange.Status.PARTIAL, suffix.status());
        assertEquals(7, suffix.startInclusive());
        assertEquals(10, suffix.endExclusive());
    }

    @Test
    void clampsRangesToTheResourceLength() {
        GrapheneByteRange.Resolution bounded = GrapheneByteRange.resolve("bytes=8-100", 10);
        GrapheneByteRange.Resolution suffix = GrapheneByteRange.resolve("bytes=-100", 10);

        assertEquals(8, bounded.startInclusive());
        assertEquals(10, bounded.endExclusive());
        assertEquals(0, suffix.startInclusive());
        assertEquals(10, suffix.endExclusive());
    }

    @Test
    void reportsUnsatisfiableRanges() {
        GrapheneByteRange.Resolution beyondEnd = GrapheneByteRange.resolve("bytes=10-20", 10);
        GrapheneByteRange.Resolution emptySuffix = GrapheneByteRange.resolve("bytes=-0", 10);

        assertEquals(GrapheneByteRange.Status.UNSATISFIABLE, beyondEnd.status());
        assertEquals("bytes */10", beyondEnd.contentRange());
        assertEquals(GrapheneByteRange.Status.UNSATISFIABLE, emptySuffix.status());
    }

    @Test
    void ignoresMalformedAndMultipleRanges() {
        assertEquals(
                GrapheneByteRange.Status.FULL,
                GrapheneByteRange.resolve("items=1-2", 10).status());
        assertEquals(
                GrapheneByteRange.Status.FULL,
                GrapheneByteRange.resolve("bytes=5-2", 10).status());
        assertEquals(
                GrapheneByteRange.Status.FULL,
                GrapheneByteRange.resolve("bytes=0-1,4-5", 10).status());
        assertEquals(
                GrapheneByteRange.Status.FULL,
                GrapheneByteRange.resolve("bytes=invalid", 10).status());
    }
}
