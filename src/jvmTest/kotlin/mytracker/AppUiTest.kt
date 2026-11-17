package mytracker

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import mytracker.domain.ExternalMediaReference
import mytracker.domain.MediaCategories
import mytracker.domain.MediaCategorySuggestion
import mytracker.domain.MediaClassificationConfidence
import mytracker.domain.MediaEntry
import mytracker.domain.MediaMetadata
import mytracker.domain.MediaSearchRateLimitException
import mytracker.domain.MediaSearchResult
import mytracker.domain.MediaSeasonMetadata
import mytracker.domain.SeasonProgress
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class AppUiTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun navigationSeparatesAnimeMoviesAndSeries() {
        val animeMovie = entry("anime-movie", "Anime movie", MediaCategories.ANIME_MOVIE, "movie")
        val animeSeries = entry("anime-series", "Anime series", MediaCategories.ANIME_SERIES, "tv")
        rule.setContent { App(initialEntries = listOf(animeMovie, animeSeries)) }

        rule.onNodeWithText("Аниме  ▸").performClick()
        rule.onNodeWithText("Аниме-сериалы", substring = false).performClick()
        rule.onNodeWithText("Anime series", substring = false).assertExists()
        rule.onNodeWithText("Anime movie", substring = false).assertDoesNotExist()

        rule.onNodeWithText("Аниме-фильмы", substring = false).performClick()
        rule.onNodeWithText("Anime movie", substring = false).assertExists()
        rule.onNodeWithText("Anime series", substring = false).assertDoesNotExist()
    }

    @Test
    fun catalogGroupsEntriesByStatusAndCreatesAnEntry() {
        var savedEntries = emptyList<MediaEntry>()
        val watched = entry(
            id = "watched",
            title = "Already watched",
            categoryId = MediaCategories.MOVIE,
            status = WatchStatus.WATCHED,
            rating = 4,
        )
        val wanted = entry(
            id = "wanted",
            title = "Want to watch",
            categoryId = MediaCategories.MOVIE,
        )
        rule.setContent {
            App(
                initialEntries = listOf(watched, wanted),
                currentDate = { "2026-11-17" },
                onEntriesChanged = {
                    savedEntries = it
                    Result.success(Unit)
                },
            )
        }

        rule.onNodeWithText("Фильмы", substring = false).performClick()
        rule.onNodeWithText("Хочу посмотреть", substring = false).assertExists()
        rule.onNodeWithText("Просмотрено", substring = false).assertExists()
        rule.onNodeWithText("Already watched", substring = false).assertExists()
        rule.onNodeWithText("Want to watch", substring = false).assertExists()

        rule.onNodeWithTag("catalog-add").performClick()
        rule.onNodeWithText("Название", substring = false).performTextInput("New movie")
        rule.onNodeWithTag("dialog-add").performClick()

        rule.onNodeWithText("New movie", substring = false).assertExists()
        assertEquals("New movie", savedEntries.last().title)
        assertEquals(MediaCategories.MOVIE, savedEntries.last().categoryId)
    }

    @Test
    fun selectingSeriesResultFromMovieCategoryAutomaticallyUsesSeriesCategory() {
        var savedEntries = emptyList<MediaEntry>()
        val seriesResult = MediaSearchResult(
            reference = ExternalMediaReference("tmdb", "series-1", "tv"),
            title = "Series result",
            releaseYear = 2024,
            overview = null,
        )
        rule.setContent {
            App(
                currentDate = { "2026-11-17" },
                searchMedia = { _, _ -> listOf(seriesResult) },
                loadMediaMetadata = {
                    MediaMetadata(
                        productionCountries = emptyList(),
                        categorySuggestion = MediaCategorySuggestion(
                            categoryId = MediaCategories.ANIME_SERIES,
                            confidence = MediaClassificationConfidence.MEDIUM,
                            reason = "Anime metadata match",
                        ),
                        seasons = listOf(MediaSeasonMetadata(1, "Первый сезон", 12, "2024-01-15")),
                    )
                },
                onEntriesChanged = {
                    savedEntries = it
                    Result.success(Unit)
                },
            )
        }

        rule.onNodeWithText("Фильмы", substring = false).performClick()
        rule.onNodeWithTag("catalog-add").performClick()
        rule.onNodeWithText("Название", substring = false).performTextInput("Series")
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("Series result", substring = false)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        rule.onNodeWithText("Series result", substring = false).performClick()
        rule.mainClock.advanceTimeBy(600)
        rule.waitForIdle()
        rule.onNodeWithTag("dialog-add").performClick()

        assertEquals("Series result", savedEntries.single().title)
        assertEquals(MediaCategories.ANIME_SERIES, savedEntries.single().categoryId)
        assertEquals(
            listOf(
                SeasonProgress(
                    seasonNumber = 1,
                    name = "Первый сезон",
                    episodeCount = 12,
                    firstAirDate = "2024-01-15",
                ),
            ),
            savedEntries.single().seasons,
        )
    }

    @Test
    fun seriesDetailsShowSeasonInformationFromMetadata() {
        val series = entry("series-metadata", "Series with seasons", MediaCategories.SERIES, "tv").copy(
            viewingNotes = listOf(ViewingNote("2026-11-17", "Personal note")),
            seasons = listOf(
                SeasonProgress(
                    seasonNumber = 1,
                    name = "Первый сезон",
                    episodeCount = 12,
                    firstAirDate = "2024-01-15",
                ),
            ),
        )
        rule.setContent { App(initialEntries = listOf(series)) }

        rule.onNodeWithText("Сериалы", substring = false).performClick()
        rule.onNodeWithText("Series with seasons", substring = false).performClick()

        rule.onNodeWithText("Сезон 1 — Первый сезон", substring = false).assertExists()
        rule.onNodeWithText("12 эпизодов", substring = false).assertExists()
        rule.onNodeWithText("Премьера: 15.01.2024", substring = false).assertExists()
        rule.onNodeWithText("17.11.2026 · Personal note", substring = false).assertExists()
    }

    @Test
    fun seasonRatingAndReviewAreAvailableOnlyWhenSeriesIsWatched() {
        var savedEntries = emptyList<MediaEntry>()
        val series = entry("series-progress", "Season progress", MediaCategories.SERIES, "tv")
            .copy(seasons = listOf(SeasonProgress(seasonNumber = 1, name = "Первый сезон")))
        rule.setContent {
            App(
                initialEntries = listOf(series),
                onEntriesChanged = {
                    savedEntries = it
                    Result.success(Unit)
                },
            )
        }

        rule.onNodeWithText("Сериалы", substring = false).performClick()
        rule.onNodeWithText("Season progress", substring = false).performClick()
        rule.onNodeWithText("Написать рецензию сезона 1", substring = false).assertDoesNotExist()
        rule.onNodeWithTag("season-1-rating-4").assertDoesNotExist()

        rule.onNodeWithText("Статус: Хочу посмотреть", substring = false).performClick()
        rule.onNodeWithTag("status-WATCHED").performClick()
        rule.onNodeWithText("Статус: Просмотрено", substring = false).assertExists()
        rule.onNodeWithTag("rating-5").performClick()
        rule.onNodeWithTag("season-1-review").performScrollTo().performClick()
        rule.onNodeWithText("Текст рецензии", substring = false).performTextInput("Season-specific review")
        rule.onNodeWithText("Сохранить рецензию", substring = false).performClick()
        rule.onNodeWithTag("season-1-rating-4").performScrollTo().performClick()
        rule.onNodeWithTag("season-1-review").assertExists()
        rule.onNodeWithText("Готово", substring = false).performClick()

        val savedSeason = savedEntries.single().seasons.single()
        assertEquals(WatchStatus.WATCHED, savedEntries.single().status)
        assertEquals(4, savedSeason.rating)
        assertEquals("Season-specific review", savedSeason.notes)
    }

    @Test
    fun creatingWatchedEntryAllowsSavingAReview() {
        var savedEntries = emptyList<MediaEntry>()
        rule.setContent {
            App(
                currentDate = { "2026-11-17" },
                onEntriesChanged = {
                    savedEntries = it
                    Result.success(Unit)
                },
            )
        }

        rule.onNodeWithText("Фильмы", substring = false).performClick()
        rule.onNodeWithTag("catalog-add").performClick()
        rule.onNodeWithText("Ищем фильмы и сериалы независимо от категории.", substring = true)
            .assertDoesNotExist()
        rule.onNodeWithText("Название", substring = false).performTextInput("Reviewed movie")
        rule.onNodeWithTag("new-status-WATCHED").performClick()
        rule.onNodeWithTag("rating-4").performClick()
        rule.onNodeWithText("Рецензия (необязательно)", substring = false)
            .performTextInput("A thoughtful review")
        rule.onNodeWithTag("dialog-add").performClick()

        val saved = savedEntries.single()
        assertEquals(WatchStatus.WATCHED, saved.status)
        assertEquals(4, saved.rating)
        assertEquals("A thoughtful review", saved.notes)
    }

    @Test
    fun acknowledgingStorageErrorDismissesTheDialog() {
        rule.setContent {
            App(initialStorageError = "Не удалось прочитать каталог.")
        }

        rule.onNodeWithText("Ошибка работы с каталогом", substring = false).assertExists()
        rule.onNodeWithText("Понятно", substring = false).performClick()
        rule.onNodeWithText("Ошибка работы с каталогом", substring = false).assertDoesNotExist()
    }

    @Test
    fun rateLimitErrorIsShownAndIncreasesSearchDebounce() {
        rule.setContent {
            App(
                searchMedia = { _, _ ->
                    throw MediaSearchRateLimitException("TMDB временно ограничил число запросов. Попробуйте позже.")
                },
            )
        }

        rule.onNodeWithText("Фильмы", substring = false).performClick()
        rule.onNodeWithTag("catalog-add").performClick()
        rule.onNodeWithText("Название", substring = false).performTextInput("Rate limited")
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("увеличена до 800 мс.", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        rule.onNodeWithText("TMDB временно ограничил число запросов", substring = true).assertExists()
        rule.onNodeWithText("увеличена до 800 мс.", substring = true).assertExists()
    }

    @Test
    fun editingCanMarkAnEntryWatchedAfterChoosingRating() {
        var savedEntries = emptyList<MediaEntry>()
        val initial = entry("edit", "Edit me", MediaCategories.MOVIE)
        rule.setContent {
            App(
                initialEntries = listOf(initial),
                onEntriesChanged = {
                    savedEntries = it
                    Result.success(Unit)
                },
            )
        }

        rule.onNodeWithText("Фильмы", substring = false).performClick()
        rule.onNodeWithText("Edit me", substring = false).performClick()
        rule.onNodeWithText("Статус: Хочу посмотреть", substring = false).performClick()
        rule.onNodeWithTag("status-WATCHED").performClick()
        rule.onNodeWithTag("rating-5").performClick()
        rule.onNodeWithText("Готово", substring = false).performClick()

        assertEquals(WatchStatus.WATCHED, savedEntries.single().status)
        assertEquals(5, savedEntries.single().rating)
    }

    @Test
    fun returningBackCancelsUncommittedChanges() {
        var saveCount = 0
        val initial = entry("cancel", "Cancel changes", MediaCategories.MOVIE)
        rule.setContent {
            App(
                initialEntries = listOf(initial),
                onEntriesChanged = {
                    saveCount++
                    Result.success(Unit)
                },
            )
        }

        rule.onNodeWithText("Фильмы", substring = false).performClick()
        rule.onNodeWithText("Cancel changes", substring = false).performClick()
        rule.onNodeWithText("Статус: Хочу посмотреть", substring = false).performClick()
        rule.onNodeWithTag("status-WATCHED").performClick()
        rule.onNodeWithTag("rating-5").performClick()
        rule.onNodeWithText("Назад", substring = false).performClick()

        assertEquals(0, saveCount)
        rule.onNodeWithText("Cancel changes", substring = false).performClick()
        rule.onNodeWithText("Статус: Хочу посмотреть", substring = false).assertExists()
    }

    @Test
    fun deletingRequiresConfirmation() {
        var savedEntries = listOf(entry("delete", "Delete me", MediaCategories.MOVIE))
        rule.setContent {
            App(
                initialEntries = savedEntries,
                onEntriesChanged = {
                    savedEntries = it
                    Result.success(Unit)
                },
            )
        }

        rule.onNodeWithText("Фильмы", substring = false).performClick()
        rule.onNodeWithContentDescription("Удалить Delete me").performClick()
        rule.onNodeWithText("Удалить произведение?", substring = false).assertExists()
        rule.onNodeWithText("Отмена", substring = false).performClick()
        rule.onNodeWithText("Delete me", substring = false).assertExists()
        assertEquals(1, savedEntries.size)

        rule.onNodeWithContentDescription("Удалить Delete me").performClick()
        rule.onNodeWithText("Удалить", substring = false).performClick()
        rule.onNodeWithText("Delete me", substring = false).assertDoesNotExist()
        assertEquals(emptyList(), savedEntries)
    }

    private fun entry(
        id: String,
        title: String,
        categoryId: String,
        mediaType: String? = null,
        status: WatchStatus = WatchStatus.WANT_TO_WATCH,
        rating: Int? = null,
    ) = MediaEntry(
        id = id,
        title = title,
        categoryId = categoryId,
        externalReference = mediaType?.let { ExternalMediaReference("tmdb", id, it) },
        status = status,
        rating = rating,
    )
}
