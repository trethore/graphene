package io.github.trethore.buildlogic.architecture

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportScannerTest {
  @Test
  fun `reports regular and static forbidden imports`() {
    val sourceFile = Files.createTempFile("architecture-check", ".java")
    sourceFile.writeText(
        """
        import org.cef.CefApp;
        import static io.github.trethore.jcefgithub.Platform.LINUX;
        import java.util.List;
        """
            .trimIndent()
    )

    val violations =
        ImportScanner.findViolations(
            sourceFile.toFile(),
            listOf("org.cef.", "io.github.trethore.jcefgithub."),
        )

    assertEquals(2, violations.size)
    assertEquals(1, violations[0].lineNumber)
    assertEquals(2, violations[1].lineNumber)
  }

  @Test
  fun `ignores comments strings and allowed imports`() {
    val sourceFile = Files.createTempFile("architecture-check", ".java")
    sourceFile.writeText(
        """
        // import org.cef.CefApp;
        String value = "import org.cef.CefApp;";
        import java.util.List;
        """
            .trimIndent()
    )

    val violations = ImportScanner.findViolations(sourceFile.toFile(), listOf("org.cef."))

    assertTrue(violations.isEmpty())
  }

  @Test
  fun `allows explicitly permitted types under forbidden prefixes`() {
    val sourceFile = Files.createTempFile("architecture-check", ".java")
    sourceFile.writeText(
        """
        import io.github.example.internal.AllowedType;
        import io.github.example.internal.ForbiddenType;
        """
            .trimIndent()
    )

    val violations =
        ImportScanner.findViolations(
            sourceFile.toFile(),
            listOf("io.github.example.internal."),
            setOf("io.github.example.internal.AllowedType"),
        )

    assertEquals(1, violations.size)
    assertEquals(2, violations[0].lineNumber)
  }
}
