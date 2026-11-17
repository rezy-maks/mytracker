package mytracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import mytracker.domain.MediaCategories
import mytracker.domain.MediaClassificationConfidence
import mytracker.domain.MediaClassifier

class MediaClassifierTest {
    @Test
    fun explicitAnimeKeywordIsHighConfidenceForMoviesAndSeries() {
        val movie = MediaClassifier.suggest(
            mediaType = "movie",
            genreIds = emptySet(),
            originalLanguage = null,
            countryCodes = emptySet(),
            keywords = setOf("Japanese anime"),
        )
        val series = MediaClassifier.suggest(
            mediaType = "tv",
            genreIds = emptySet(),
            originalLanguage = null,
            countryCodes = emptySet(),
            keywords = setOf("anime"),
        )

        assertEquals(MediaCategories.ANIME_MOVIE, movie?.categoryId)
        assertEquals(MediaCategories.ANIME_SERIES, series?.categoryId)
        assertEquals(MediaClassificationConfidence.HIGH, movie?.confidence)
        assertEquals(MediaClassificationConfidence.HIGH, series?.confidence)
    }

    @Test
    fun japaneseAnimationIsMediumConfidence() {
        val suggestion = MediaClassifier.suggest(
            mediaType = "tv",
            genreIds = setOf(16),
            originalLanguage = "ja",
            countryCodes = emptySet(),
            keywords = emptySet(),
        )

        assertEquals(MediaCategories.ANIME_SERIES, suggestion?.categoryId)
        assertEquals(MediaClassificationConfidence.MEDIUM, suggestion?.confidence)
    }

    @Test
    fun animationOrJapaneseOriginAloneDoesNotClassifyAsAnime() {
        val animationOnly = MediaClassifier.suggest(
            mediaType = "movie",
            genreIds = setOf(16),
            originalLanguage = "en",
            countryCodes = emptySet(),
            keywords = emptySet(),
        )
        val japaneseLiveAction = MediaClassifier.suggest(
            mediaType = "movie",
            genreIds = setOf(18),
            originalLanguage = "ja",
            countryCodes = setOf("JP"),
            keywords = emptySet(),
        )

        assertNull(animationOnly)
        assertNull(japaneseLiveAction)
    }
}
