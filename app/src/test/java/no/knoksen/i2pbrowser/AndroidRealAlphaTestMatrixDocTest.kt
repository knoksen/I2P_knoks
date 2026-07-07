package no.knoksen.i2pbrowser

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AndroidRealAlphaTestMatrixDocTest {
    @Test
    fun `matrix document contains stable P0 core-flow identifiers`() {
        val matrix = findMatrixDocument().readText()
        val requiredIds = listOf(
            "CORE-START-001",
            "CORE-MODE-001",
            "CORE-ENDPOINT-001",
            "CORE-DIAG-001",
            "CORE-DIAG-004",
            "CORE-DIAG-005",
            "CORE-DIAG-006",
            "CORE-DIAG-007",
            "CORE-SAM-001",
            "CORE-PROXY-001",
            "CORE-IDENTITY-001",
            "CORE-LOG-001",
            "CORE-MIGRATION-001",
            "CORE-MIGRATION-002",
            "CORE-MIGRATION-003",
            "CORE-MIGRATION-004",
            "CORE-MIGRATION-005",
            "CORE-MIGRATION-006",
            "CORE-UI-001"
        )

        requiredIds.forEach { id ->
            assertTrue("Missing matrix ID $id", matrix.contains("`$id`"))
        }
        assertTrue(matrix.contains("does not prove anonymity"))
        assertTrue(matrix.contains("timeout does not prove the router is offline"))
        assertTrue(matrix.contains("Default CI must keep using deterministic local tests"))
    }

    private fun findMatrixDocument(): File {
        val candidates = listOf(
            File("docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md"),
            File("../docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md")
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Could not locate docs/ANDROID_REAL_ALPHA_TEST_MATRIX.md from ${File(".").absolutePath}")
    }
}
