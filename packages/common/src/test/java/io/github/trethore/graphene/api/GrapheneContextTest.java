package io.github.trethore.graphene.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class GrapheneContextTest {
  @Test
  void hasNoPublicConstructors() {
    assertEquals(0, GrapheneContext.class.getConstructors().length);
  }
}
