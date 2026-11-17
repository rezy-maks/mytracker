package mytracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import mytracker.domain.MediaEntryIdGenerator

class MediaEntryIdGeneratorTest {
    @Test
    fun generateReturnsDistinctUuidIds() {
        val generator = MediaEntryIdGenerator()
        val existingIds = mutableSetOf<String>()

        repeat(10_000) {
            val id = generator.generate(existingIds)
            assertNotEquals("", id)
            assertEquals(36, id.length)
            assertEquals(true, existingIds.add(id))
        }
    }

    @Test
    fun generateRetriesWhenCandidateAlreadyExists() {
        val candidates = ArrayDeque(listOf("already-used", "fresh-id"))
        val generator = MediaEntryIdGenerator { candidates.removeFirst() }

        assertEquals("fresh-id", generator.generate(setOf("already-used")))
    }
}
