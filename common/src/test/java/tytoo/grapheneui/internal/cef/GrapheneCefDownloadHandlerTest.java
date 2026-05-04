package tytoo.grapheneui.internal.cef;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class GrapheneCefDownloadHandlerTest {
    @TempDir
    Path tempDir;

    @Test
    void uniqueDownloadPathSkipsExistingAndReservedPaths() throws IOException {
        Path existingPath = tempDir.resolve("download.txt");
        Files.writeString(existingPath, "existing");

        Path reservedPath = tempDir.resolve("download (1).txt");

        Path uniquePath = GrapheneCefDownloadHandler.uniqueDownloadPath(
                tempDir,
                "download.txt",
                Set.of(reservedPath)
        );

        assertEquals(tempDir.resolve("download (2).txt"), uniquePath);
    }
}
