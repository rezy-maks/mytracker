package mytracker.domain

enum class MediaEntryValidationError(val message: String) {
    EMPTY_TITLE("Название не может быть пустым."),
    EMPTY_CATEGORY("Выберите категорию произведения."),
    WATCHED_WITHOUT_RATING("Для просмотренного произведения нужна оценка от 1 до 5."),
    INVALID_RATING("Оценка должна быть от 1 до 5."),
    EMPTY_VIEWING_NOTE("Текст заметки не может быть пустым."),
    INVALID_VIEWING_NOTE_DATE("Дата заметки должна быть корректной датой в формате ГГГГ-ММ-ДД."),
    INVALID_SEASON_NUMBER("Номер сезона не может быть отрицательным."),
    DUPLICATE_SEASON_NUMBER("Номера сезонов должны быть уникальными."),
}

object MediaEntryValidator {
    fun validate(entry: MediaEntry): List<MediaEntryValidationError> = buildList {
        if (entry.title.isBlank()) add(MediaEntryValidationError.EMPTY_TITLE)
        if (entry.categoryId.isBlank()) add(MediaEntryValidationError.EMPTY_CATEGORY)
        if (entry.rating != null && entry.rating !in 1..5) {
            add(MediaEntryValidationError.INVALID_RATING)
        }
        if (entry.status == WatchStatus.WATCHED && entry.rating == null) {
            add(MediaEntryValidationError.WATCHED_WITHOUT_RATING)
        }

        entry.viewingNotes.forEach { note ->
            if (note.text.isBlank()) add(MediaEntryValidationError.EMPTY_VIEWING_NOTE)
            if (!note.date.isValidIsoDate()) add(MediaEntryValidationError.INVALID_VIEWING_NOTE_DATE)
        }

        if (entry.seasons.any { it.rating != null && it.rating !in 1..5 }) {
            add(MediaEntryValidationError.INVALID_RATING)
        }
        if (entry.seasons.any { it.seasonNumber < 0 }) {
            add(MediaEntryValidationError.INVALID_SEASON_NUMBER)
        }
        if (entry.seasons.map { it.seasonNumber }.distinct().size != entry.seasons.size) {
            add(MediaEntryValidationError.DUPLICATE_SEASON_NUMBER)
        }
    }.distinct()

    private fun String.isValidIsoDate(): Boolean {
        if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(this)) return false
        val year = substring(0, 4).toInt()
        val month = substring(5, 7).toInt()
        val day = substring(8, 10).toInt()
        if (month !in 1..12) return false

        val daysInMonth = when (month) {
            2 -> if (year.isLeapYear()) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return day in 1..daysInMonth
    }

    private fun Int.isLeapYear(): Boolean = this % 4 == 0 && (this % 100 != 0 || this % 400 == 0)
}
