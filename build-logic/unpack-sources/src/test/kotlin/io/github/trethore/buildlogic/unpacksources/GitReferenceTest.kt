package io.github.trethore.buildlogic.unpacksources

import kotlin.test.Test
import kotlin.test.assertEquals

class GitReferenceTest {
    @Test
    fun `serialized references round trip reserved characters`() {
        val reference = GitReference(
            name = "example%name\u001Fvalue",
            url = "git@example.com:owner/repository.git",
            branch = "feature/references",
            commit = null,
            sparsePaths = listOf(
                "sources/%value\u001Eone",
                "sources/value\u001Ftwo",
            ),
        )

        assertEquals(reference, GitReference.deserialize(reference.serialize()))
    }

    @Test
    fun `legacy serialized references deserialize without sparse paths`() {
        val reference = GitReference.deserialize(
            "%00\u001Fhttps://example.com/repository.git\u001Fmain\u001F%00"
        )

        assertEquals(emptyList(), reference.sparsePaths)
    }
}
