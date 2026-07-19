package io.github.trethore.graphene.api.browser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.trethore.graphene.api.runtime.GrapheneRuntimeState;
import org.junit.jupiter.api.Test;

class BrowserRuntimeUnavailableExceptionTest {
  @Test
  void exposesRuntimeStateAndInitializationFailure() {
    IllegalStateException initializationFailure = new IllegalStateException("failed");

    BrowserRuntimeUnavailableException exception =
        new BrowserRuntimeUnavailableException(GrapheneRuntimeState.FAILED, initializationFailure);

    assertEquals(GrapheneRuntimeState.FAILED, exception.runtimeState());
    assertSame(initializationFailure, exception.getCause());
  }
}
