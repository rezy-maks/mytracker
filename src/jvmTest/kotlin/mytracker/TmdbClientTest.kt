package mytracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import mytracker.domain.MediaCategories
import mytracker.domain.MediaClassificationConfidence
import mytracker.domain.MediaSeasonMetadata

class TmdbClientTest {
    private val client = TmdbClient(accessToken = null)

    @Test
    fun fallbackSearchIncludesYoVariantForCommonRussianSpelling() {
        val withYo = "\u0417\u0435\u043b\u0451\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        val withoutYo = "\u0417\u0435\u043b\u0435\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        assertTrue(withYo in client.queryFallbacks(withoutYo))
        assertTrue(withoutYo in client.queryFallbacks(withYo))
    }

    @Test
    fun fallbackSearchConvertsTextTypedWithWrongKeyboardLayout() {
        val cyrillicTitle = "\u0417\u0435\u043b\u0435\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        val russianLayoutEnglishTitle = "\u042b\u0435\u0444\u043a \u0426\u0444\u043a\u044b"
        assertTrue(cyrillicTitle in client.queryFallbacks("Ptktyfz vbkz"))
        assertTrue("Star Wars" in client.queryFallbacks(russianLayoutEnglishTitle))
    }

    @Test
    fun fallbackSearchTransliteratesRussianTitlesTypedInLatin() {
        val russianTitle = "\u0437\u0435\u043b\u0451\u043d\u0430\u044f \u043c\u0438\u043b\u044f"
        assertTrue(russianTitle in client.queryFallbacks("Zelenaya milya"))
    }

    @Test
    fun fallbackSearchRemovesLatinDiacritics() {
        assertTrue("Amelie" in client.queryFallbacks("Am\u00e9lie"))
    }

    @Test
    fun searchIncludesMoviesAndSeriesRegardlessOfSelectedCategory() {
        listOf(
            MediaCategories.MOVIE,
            MediaCategories.SERIES,
            MediaCategories.ANIME,
            MediaCategories.ANIME_MOVIE,
            MediaCategories.ANIME_SERIES,
        ).forEach { category ->
            val types = client.searchTypes(category)

            assertEquals(setOf("movie", "tv"), types.toSet())
            assertEquals(2, types.size)
        }
    }

    @Test
    fun searchPrioritizesTheSelectedCategoryTypeWithoutExcludingTheOtherType() {
        assertEquals(listOf("movie", "tv"), client.searchTypes(MediaCategories.MOVIE))
        assertEquals(listOf("tv", "movie"), client.searchTypes(MediaCategories.SERIES))
        assertEquals(listOf("movie", "tv"), client.searchTypes(MediaCategories.ANIME_MOVIE))
        assertEquals(listOf("tv", "movie"), client.searchTypes(MediaCategories.ANIME_SERIES))
    }

    @Test
    fun productionCountriesUsesTmdbNamesWhenAvailable() {
        val details = """{"production_countries":[{"iso_3166_1":"RU","name":"\u0420\u043e\u0441\u0441\u0438\u044f"}]}"""

        assertEquals(listOf("\u0420\u043e\u0441\u0441\u0438\u044f"), client.parseProductionCountries(details))
    }

    @Test
    fun productionCountriesTranslatesTvOriginCountryCodes() {
        val details = """{"origin_country":["JP"]}"""

        val country = client.parseProductionCountries(details).single()

        assertNotEquals("JP", country)
        assertTrue(country.isNotBlank())
    }

    @Test
    fun metadataRecognizesExplicitAnimeKeywordForMovie() {
        val details = """{"genres":[{"id":16}],"keywords":{"keywords":[{"name":"anime"}]}}"""

        val metadata = client.parseMediaMetadata(details, "movie")

        assertEquals(MediaCategories.ANIME_MOVIE, metadata.categorySuggestion?.categoryId)
        assertEquals(MediaClassificationConfidence.HIGH, metadata.categorySuggestion?.confidence)
    }

    @Test
    fun metadataRecognizesJapaneseAnimationAsMediumConfidenceForSeries() {
        val details =
            """{"genres":[{"id":16}],"original_language":"ja","origin_country":["JP"],"keywords":{"results":[]}}"""

        val metadata = client.parseMediaMetadata(details, "tv")

        assertEquals(MediaCategories.ANIME_SERIES, metadata.categorySuggestion?.categoryId)
        assertEquals(MediaClassificationConfidence.MEDIUM, metadata.categorySuggestion?.confidence)
    }

    @Test
    fun tvMetadataParsesSeasonDetailsAndOrdersBySeasonNumber() {
        val details = """
            {
              "seasons": [
                {"season_number": 2, "name": "Second Season", "episode_count": 8, "air_date": "2021-04-03"},
                {"season_number": 0, "name": "Specials", "episode_count": 2, "air_date": null},
                {"name": "Invalid season", "episode_count": 1}
              ]
            }
        """.trimIndent()

        val metadata = client.parseMediaMetadata(details, "tv")

        assertEquals(
            listOf(
                MediaSeasonMetadata(0, "Specials", 2, null),
                MediaSeasonMetadata(2, "Second Season", 8, "2021-04-03"),
            ),
            metadata.seasons,
        )
    }

    @Test
    fun movieMetadataDoesNotExposeSeasons() {
        val metadata = client.parseMediaMetadata(
            """{"seasons":[{"season_number":1,"name":"Ignored"}]}""",
            "movie",
        )

        assertEquals(emptyList(), metadata.seasons)
    }

    @Test
    fun searchResultBuildsTmdbPosterUrlWhenPosterPathExists() {
        val response = """{"results":[{"id":550,"title":"Fight Club","release_date":"1999-10-15","poster_path":"/poster.jpg"}]}"""

        val result = client.parseResults(response, "movie").single()

        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", result.posterUrl)
    }
}
