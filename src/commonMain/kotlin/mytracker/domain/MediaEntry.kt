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

/** A short note about a viewing. The stored date uses ISO-8601 text (yyyy-MM-dd). */
data class ViewingNote(
    val date: String,
    val text: String,
)

data class SeasonProgress(
    val seasonNumber: Int,
    val name: String? = null,
    val episodeCount: Int? = null,
    val firstAirDate: String? = null,
    val rating: Int? = null,
    val notes: String = "",
)

data class MediaEntry(
    val id: String,
    val title: String,
    val categoryId: String,
    val externalReference: ExternalMediaReference? = null,
    val posterUrl: String? = null,
    val releaseYear: Int? = null,
    val productionCountries: List<String> = emptyList(),
    val status: WatchStatus = WatchStatus.WANT_TO_WATCH,
    /** A score from 1 to 5 check marks. Null means no score was assigned. */
    val rating: Int? = null,
    /** Kept when the status changes, even when the normal UI hides it. */
    val notes: String = "",
    val viewingNotes: List<ViewingNote> = emptyList(),
    val seasons: List<SeasonProgress> = emptyList(),
)
