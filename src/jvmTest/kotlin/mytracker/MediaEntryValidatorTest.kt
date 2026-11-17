package mytracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mytracker.domain.MediaEntry
import mytracker.domain.MediaEntryValidationError
import mytracker.domain.MediaEntryValidator
import mytracker.domain.SeasonProgress
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus

class MediaEntryValidatorTest {
    @Test
    fun watchedEntryRequiresRatingAndRatingMustBeWithinOneToFive() {
        val withoutRating = MediaEntry(
            id = "entry",
            title = "Title",
            categoryId = "Фильм",
            status = WatchStatus.WATCHED,
        )
        val invalidRating = withoutRating.copy(rating = 6)

        assertEquals(
            listOf(MediaEntryValidationError.WATCHED_WITHOUT_RATING),
            MediaEntryValidator.validate(withoutRating),
        )
        assertEquals(
            listOf(MediaEntryValidationError.INVALID_RATING),
            MediaEntryValidator.validate(invalidRating),
        )
        assertTrue(MediaEntryValidator.validate(withoutRating.copy(rating = 1)).isEmpty())
        assertTrue(MediaEntryValidator.validate(withoutRating.copy(rating = 5)).isEmpty())
        assertEquals(
            listOf(MediaEntryValidationError.INVALID_RATING),
            MediaEntryValidator.validate(withoutRating.copy(rating = 0)),
        )
    }

    @Test
    fun ratingIsOptionalForNonWatchedEntriesAndReviewTextIsNotConstrained() {
        val entry = MediaEntry(
            id = "entry",
            title = "Title",
            categoryId = "Фильм",
            status = WatchStatus.WANT_TO_WATCH,
            rating = 5,
            notes = "A review can be kept when the status changes.\n".repeat(1_000),
        )

        assertTrue(MediaEntryValidator.validate(entry).isEmpty())
    }

    @Test
    fun titleAndCategoryMustNotBeBlank() {
        val entry = MediaEntry(id = "entry", title = "  ", categoryId = "")

        assertEquals(
            listOf(
                MediaEntryValidationError.EMPTY_TITLE,
                MediaEntryValidationError.EMPTY_CATEGORY,
            ),
            MediaEntryValidator.validate(entry),
        )
    }

    @Test
    fun viewingNotesRequireNonBlankTextAndValidIsoDate() {
        val entry = MediaEntry(
            id = "entry",
            title = "Title",
            categoryId = "Фильм",
            viewingNotes = listOf(
                ViewingNote("2024-02-29", "Valid leap day"),
                ViewingNote("2023-02-29", "Invalid leap day"),
                ViewingNote("2024-03-01", "  "),
            ),
        )

        assertEquals(
            listOf(
                MediaEntryValidationError.INVALID_VIEWING_NOTE_DATE,
                MediaEntryValidationError.EMPTY_VIEWING_NOTE,
            ),
            MediaEntryValidator.validate(entry),
        )
    }

    @Test
    fun calendarDatesRespectLeapYearsAndMonthBoundaries() {
        val entry = MediaEntry(
            id = "entry",
            title = "Title",
            categoryId = "Фильм",
            viewingNotes = listOf(
                ViewingNote("2000-02-29", "Leap year"),
                ViewingNote("1900-02-29", "Not a leap year"),
                ViewingNote("2024-04-30", "Last day of April"),
                ViewingNote("2024-04-31", "Invalid April date"),
                ViewingNote("2024-13-01", "Invalid month"),
                ViewingNote("24-01-01", "Invalid format"),
            ),
        )

        assertEquals(
            listOf(MediaEntryValidationError.INVALID_VIEWING_NOTE_DATE),
            MediaEntryValidator.validate(entry),
        )
    }

    @Test
    fun seasonsMustHaveNonNegativeUniqueNumbersAndRatingsWithinRange() {
        val entry = MediaEntry(
            id = "entry",
            title = "Title",
            categoryId = "Сериал",
            seasons = listOf(
                SeasonProgress(0),
                SeasonProgress(seasonNumber = 1, rating = 5),
                SeasonProgress(1),
                SeasonProgress(-1),
                SeasonProgress(seasonNumber = 2, rating = 6),
                SeasonProgress(seasonNumber = 3, rating = 0),
            ),
        )

        assertEquals(
            listOf(
                MediaEntryValidationError.INVALID_RATING,
                MediaEntryValidationError.INVALID_SEASON_NUMBER,
                MediaEntryValidationError.DUPLICATE_SEASON_NUMBER,
            ),
            MediaEntryValidator.validate(entry),
        )
    }

    @Test
    fun emptyOptionalReviewAndSeasonZeroAreValid() {
        val entry = MediaEntry(
            id = "entry",
            title = "Title",
            categoryId = "Сериал",
            notes = "",
            seasons = listOf(SeasonProgress(0)),
        )

        assertTrue(MediaEntryValidator.validate(entry).isEmpty())
    }

    @Test
    fun seasonRatingAndReviewAreOptionalWithoutWatchedStatus() {
        val entry = MediaEntry(
            id = "entry",
            title = "Series",
            categoryId = "Сериал",
            seasons = listOf(
                SeasonProgress(seasonNumber = 1),
                SeasonProgress(seasonNumber = 2, rating = 4, notes = "Season review"),
            ),
        )

        assertTrue(MediaEntryValidator.validate(entry).isEmpty())
    }
}
