package mytracker.domain

import kotlin.uuid.Uuid

/** Creates a random UUID that is not already used by the current catalog. */
class MediaEntryIdGenerator(
    private val generateCandidate: () -> String = { Uuid.random().toString() },
) {
    fun generate(existingIds: Set<String>): String {
        var candidate: String
        do {
            candidate = generateCandidate()
        } while (candidate in existingIds)
        return candidate
    }
}
