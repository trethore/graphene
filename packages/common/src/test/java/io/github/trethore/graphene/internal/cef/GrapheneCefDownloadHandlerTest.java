package io.github.trethore.graphene.internal.cef;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GrapheneCefDownloadHandlerTest {
  @TempDir Path temporaryDirectory;

  @Test
  void createsSafeUniqueDownloadPaths() {
    Path first =
        GrapheneCefDownloadHandler.uniquePath(temporaryDirectory, "bad/name.txt", Set.of());
    Path second =
        GrapheneCefDownloadHandler.uniquePath(temporaryDirectory, "bad/name.txt", Set.of(first));

    assertEquals("bad_name.txt", first.getFileName().toString());
    assertEquals("bad_name (1).txt", second.getFileName().toString());
  }
}
