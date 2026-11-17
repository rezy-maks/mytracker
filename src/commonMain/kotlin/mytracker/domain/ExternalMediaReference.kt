package mytracker.domain

/** Stable identifier of a title in an external metadata catalog. */
data class ExternalMediaReference(
    val provider: String,
    val id: String,
    /** Provider-specific type, for example TMDB's `movie` or `tv`. */
    val mediaType: String,
)

data class MediaSearchResult(
    val reference: ExternalMediaReference,
    val title: String,
    val releaseYear: Int?,
    val overview: String?,
    val posterUrl: String? = null,
)
