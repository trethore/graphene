package io.github.trethore.graphene.fabric.internal.input;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class GrapheneClickCounterTest {
  @Test
  void countsConsecutiveClicksBeyondDoubleClick() {
    GrapheneClickCounter counter = new GrapheneClickCounter();

    assertEquals(1, counter.registerClick(0, false, 1_000));
    assertEquals(2, counter.registerClick(0, true, 1_100));
    assertEquals(3, counter.registerClick(0, true, 1_200));
    assertEquals(3, counter.current(0));
  }

  @Test
  void resetsForAStaleOrDifferentButtonSequence() {
    GrapheneClickCounter counter = new GrapheneClickCounter();

    assertEquals(1, counter.registerClick(0, false, 1_000));
    assertEquals(1, counter.registerClick(0, true, 1_250));
    assertEquals(1, counter.registerClick(1, true, 1_300));
    assertEquals(1, counter.current(0));
  }
}
