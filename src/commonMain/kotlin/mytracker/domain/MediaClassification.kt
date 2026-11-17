package mytracker.domain

enum class MediaClassificationConfidence {
    HIGH,
    MEDIUM,
}

data class MediaCategorySuggestion(
    val categoryId: String,
    val confidence: MediaClassificationConfidence,
    val reason: String,
)

data class MediaMetadata(
    val productionCountries: List<String>,
    val categorySuggestion: MediaCategorySuggestion?,
    val seasons: List<MediaSeasonMetadata> = emptyList(),
)

data class MediaSeasonMetadata(
    val seasonNumber: Int,
    val name: String?,
    val episodeCount: Int?,
    val firstAirDate: String?,
)

object MediaCategories {
    const val MOVIE = "Фильм"
    const val SERIES = "Сериал"
    const val ANIME_MOVIE = "Аниме-фильм"
    const val ANIME_SERIES = "Аниме-сериал"
    const val ANIME = "Аниме"

    fun forMediaType(mediaType: String, anime: Boolean = false): String = when {
        mediaType == "movie" && anime -> ANIME_MOVIE
        mediaType == "tv" && anime -> ANIME_SERIES
        mediaType == "movie" -> MOVIE
        mediaType == "tv" -> SERIES
        else -> ANIME
    }

    fun isAnime(categoryId: String): Boolean =
        categoryId == ANIME || categoryId == ANIME_MOVIE || categoryId == ANIME_SERIES
}

object MediaClassifier {
    private const val ANIMATION_GENRE_ID = 16

    fun suggest(
        mediaType: String,
        genreIds: Set<Int>,
        originalLanguage: String?,
        countryCodes: Set<String>,
        keywords: Set<String>,
    ): MediaCategorySuggestion? {
        if (mediaType != "movie" && mediaType != "tv") return null

        val hasAnimeKeyword = keywords.any { keyword ->
            keyword.contains("anime", ignoreCase = true) ||
                keyword.contains("japanese animation", ignoreCase = true)
        }
        val category = MediaCategories.forMediaType(mediaType, anime = true)
        if (hasAnimeKeyword) {
            return MediaCategorySuggestion(
                categoryId = category,
                confidence = MediaClassificationConfidence.HIGH,
                reason = "В ключевых словах TMDB указано аниме",
            )
        }

        val isJapanese = originalLanguage.equals("ja", ignoreCase = true) ||
            countryCodes.any { it.equals("JP", ignoreCase = true) }
        if (ANIMATION_GENRE_ID in genreIds && isJapanese) {
            return MediaCategorySuggestion(
                categoryId = category,
                confidence = MediaClassificationConfidence.MEDIUM,
                reason = "Анимация и японское происхождение",
            )
        }

        return null
    }
}
