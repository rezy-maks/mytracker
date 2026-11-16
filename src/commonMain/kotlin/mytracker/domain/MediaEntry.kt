package mytracker.domain

/** A node in the user-visible category tree, for example "Anime" -> "Film". */
data class MediaCategory(
    val id: String,
    val name: String,
    val parentId: String? = null,
)

enum class WatchStatus {
    WANT_TO_WATCH,
    ABANDONED,
    WATCHED,
}

/**
 * A personal review written after a viewing. The date is stored as ISO-8601
 * text (yyyy-MM-dd) so the shared model does not depend on a platform date API.
 */
data class ViewingNote(
    val date: String,
    val text: String,
    val rating: Int? = null,
)

data class SeasonProgress(
    val seasonNumber: Int,
    val rating: Int? = null,
    val viewingNotes: List<ViewingNote> = emptyList(),
)

data class MediaEntry(
    val id: String,
    val title: String,
    val categoryId: String,
    val status: WatchStatus = WatchStatus.WANT_TO_WATCH,
    /** A score from 1 to 5 check marks. Null means no score was assigned. */
    val rating: Int? = null,
    /** Kept when the status changes, even when the normal UI hides it. */
    val notes: String = "",
    val viewingNotes: List<ViewingNote> = emptyList(),
    val seasons: List<SeasonProgress> = emptyList(),
)

