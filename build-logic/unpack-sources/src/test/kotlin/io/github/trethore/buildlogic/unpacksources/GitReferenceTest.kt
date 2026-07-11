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
        )

        assertEquals(reference, GitReference.deserialize(reference.serialize()))
    }
}
