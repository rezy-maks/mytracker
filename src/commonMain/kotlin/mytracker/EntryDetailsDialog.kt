package mytracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mytracker.domain.MediaEntry
import mytracker.domain.MediaCategories
import mytracker.domain.MediaEntryValidator
import mytracker.domain.SeasonProgress
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus

@Composable
internal fun EntryDetailsDialog(
    entry: MediaEntry,
    currentDate: () -> String,
    onDismiss: () -> Unit,
    onEntryChange: (MediaEntry) -> Boolean,
) {
    var currentEntry by remember(entry.id) { mutableStateOf(entry) }
    var noteText by remember { mutableStateOf("") }
    var isNotePopupVisible by remember { mutableStateOf(false) }
    var reviewText by remember(entry.id) { mutableStateOf(entry.notes) }
    var isReviewPopupVisible by remember { mutableStateOf(false) }
    var reviewTargetSeason by remember { mutableStateOf<Int?>(null) }
    var seasonReviewText by remember { mutableStateOf("") }
    var isSeasonReviewPopupVisible by remember { mutableStateOf(false) }
    var isStatusMenuVisible by remember { mutableStateOf(false) }
    val validationErrors = MediaEntryValidator.validate(currentEntry)

    fun updateEntry(updatedEntry: MediaEntry) {
        currentEntry = updatedEntry
    }

    fun showNoteForm() {
        noteText = ""
        isNotePopupVisible = true
    }

    fun saveNote() {
        val note = ViewingNote(date = currentDate(), text = noteText.trim())
        updateEntry(currentEntry.copy(viewingNotes = currentEntry.viewingNotes + note))
        isNotePopupVisible = false
    }

    fun updateSeason(seasonNumber: Int, update: (SeasonProgress) -> SeasonProgress) {
        updateEntry(
            currentEntry.copy(
                seasons = currentEntry.seasons.map { season ->
                    if (season.seasonNumber == seasonNumber) update(season) else season
                },
            ),
        )
    }

    fun saveSeasonReview() {
        reviewTargetSeason?.let { seasonNumber ->
            updateSeason(seasonNumber) { it.copy(notes = seasonReviewText.trim()) }
        }
        isSeasonReviewPopupVisible = false
    }

    val hasSeasons = when (currentEntry.externalReference?.mediaType) {
        "movie" -> false
        "tv" -> true
        else -> currentEntry.categoryId !in setOf(MediaCategories.MOVIE, MediaCategories.ANIME_MOVIE)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(currentEntry.title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    MediaPoster(
                        url = currentEntry.posterUrl,
                        title = currentEntry.title,
                        modifier = Modifier.size(width = 82.dp, height = 123.dp),
                    )
                    Text(currentEntry.categoryId, color = Muted)
                }
                Box {
                    TextButton(onClick = { isStatusMenuVisible = true }) {
                        Text("Статус: ${currentEntry.status.label()}")
                    }
                    DropdownMenu(
                        expanded = isStatusMenuVisible,
                        onDismissRequest = { isStatusMenuVisible = false },
                    ) {
                        WatchStatus.entries.forEach { status ->
                            DropdownMenuItem(
                                modifier = Modifier.testTag("status-${status.name}"),
                                text = { Text(status.label()) },
                                onClick = {
                                    updateEntry(currentEntry.copy(status = status))
                                    isStatusMenuVisible = false
                                },
                            )
                        }
                    }
                }
                validationErrors.forEach { error ->
                    Text(error.message, color = Color(0xFFE58A96), fontSize = 12.sp)
                }
                if (currentEntry.status == WatchStatus.WATCHED) {
                    RatingPicker(
                        value = currentEntry.rating,
                        onValueChange = { rating ->
                            updateEntry(currentEntry.copy(rating = rating))
                        },
                    )
                }
                if (currentEntry.status == WatchStatus.WATCHED && currentEntry.rating != null) {
                    Text("Рецензия", fontWeight = FontWeight.SemiBold)
                    if (currentEntry.notes.isNotBlank()) Text(currentEntry.notes)
                    Box {
                        TextButton(
                            onClick = {
                                reviewText = currentEntry.notes
                                isReviewPopupVisible = true
                            },
                        ) {
                            Text(if (currentEntry.notes.isBlank()) "Написать рецензию" else "Изменить рецензию")
                        }
                        DropdownMenu(
                            expanded = isReviewPopupVisible,
                            onDismissRequest = { isReviewPopupVisible = false },
                        ) {
                            Column(
                                modifier = Modifier.width(320.dp).padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text("Рецензия на произведение", fontWeight = FontWeight.Medium)
                                OutlinedTextField(
                                    value = reviewText,
                                    onValueChange = { reviewText = it },
                                    label = { Text("Текст рецензии") },
                                    minLines = 3,
                                    maxLines = 6,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(
                                        onClick = {
                                            updateEntry(
                                                currentEntry.copy(
                                                    notes = reviewText.trim(),
                                                ),
                                            )
                                            isReviewPopupVisible = false
                                        },
                                    ) {
                                        Text("Сохранить рецензию")
                                    }
                                    TextButton(onClick = { isReviewPopupVisible = false }) {
                                        Text("Отмена")
                                    }
                                }
                            }
                        }
                    }
                }

                Text("Личные заметки", fontWeight = FontWeight.SemiBold)
                if (currentEntry.viewingNotes.isEmpty()) {
                    Text("Пока нет заметок", color = Muted, fontSize = 13.sp)
                } else {
                    currentEntry.viewingNotes.forEachIndexed { index, note ->
                        ViewingNoteRow(note) {
                            updateEntry(
                                currentEntry.copy(
                                    viewingNotes = currentEntry.viewingNotes.filterIndexed { noteIndex, _ ->
                                        noteIndex != index
                                    },
                                ),
                            )
                        }
                    }
                }
                Box {
                    TextButton(onClick = ::showNoteForm) {
                        Text("Добавить заметку")
                    }
                    DropdownMenu(
                        expanded = isNotePopupVisible,
                        onDismissRequest = { isNotePopupVisible = false },
                    ) {
                        NotePopupContent(
                            text = noteText,
                            onTextChange = { noteText = it },
                            onSave = ::saveNote,
                            onCancel = { isNotePopupVisible = false },
                        )
                    }
                }

                if (hasSeasons) {
                    Text("Сезоны", fontWeight = FontWeight.SemiBold)
                    if (currentEntry.seasons.isEmpty()) {
                        Text("Сезоны появятся после загрузки данных", color = Muted, fontSize = 13.sp)
                    } else {
                        currentEntry.seasons.sortedBy { it.seasonNumber }.forEach { season ->
                            Surface(color = Panel, shape = RoundedCornerShape(12.dp)) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Text(
                                        buildString {
                                            append("Сезон ${season.seasonNumber}")
                                            season.name?.takeIf(String::isNotBlank)?.let { append(" — $it") }
                                        },
                                        fontWeight = FontWeight.Medium,
                                    )
                                    season.episodeCount?.let { count ->
                                        Text("$count эпизодов", color = Muted, fontSize = 12.sp)
                                    }
                                    season.firstAirDate?.let { date ->
                                        Text("Премьера: ${date.toDisplayDate()}", color = Muted, fontSize = 12.sp)
                                    }
                                    if (currentEntry.status == WatchStatus.WATCHED) {
                                        RatingPicker(
                                            value = season.rating,
                                            onValueChange = { rating ->
                                                updateSeason(season.seasonNumber) { it.copy(rating = rating) }
                                            },
                                            tagPrefix = "season-${season.seasonNumber}-rating",
                                            label = "Оценка сезона (необязательно)",
                                        )
                                        if (season.rating != null) {
                                            TextButton(
                                                onClick = {
                                                    updateSeason(season.seasonNumber) { it.copy(rating = null) }
                                                },
                                            ) {
                                                Text("Убрать оценку сезона")
                                            }
                                        }
                                        if (season.notes.isNotBlank()) Text(season.notes)
                                        Box {
                                            TextButton(
                                                modifier = Modifier.testTag(
                                                    "season-${season.seasonNumber}-review",
                                                ),
                                                onClick = {
                                                    reviewTargetSeason = season.seasonNumber
                                                    seasonReviewText = season.notes
                                                    isSeasonReviewPopupVisible = true
                                                },
                                            ) {
                                                Text(
                                                    if (season.notes.isBlank()) {
                                                        "Написать рецензию сезона ${season.seasonNumber}"
                                                    } else {
                                                        "Изменить рецензию сезона ${season.seasonNumber}"
                                                    },
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = isSeasonReviewPopupVisible &&
                                                    reviewTargetSeason == season.seasonNumber,
                                                onDismissRequest = { isSeasonReviewPopupVisible = false },
                                            ) {
                                                Column(
                                                    modifier = Modifier.width(320.dp).padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Text(
                                                        "Рецензия на сезон ${season.seasonNumber}",
                                                        fontWeight = FontWeight.Medium,
                                                    )
                                                    OutlinedTextField(
                                                        value = seasonReviewText,
                                                        onValueChange = { seasonReviewText = it },
                                                        label = { Text("Текст рецензии") },
                                                        minLines = 3,
                                                        maxLines = 6,
                                                    )
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        TextButton(onClick = ::saveSeasonReview) {
                                                            Text("Сохранить рецензию")
                                                        }
                                                        TextButton(
                                                            onClick = { isSeasonReviewPopupVisible = false },
                                                        ) {
                                                            Text("Отмена")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text(
                                            "Чтобы оценить сезон или написать рецензию, отметьте сериал как просмотренный.",
                                            color = Muted,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (onEntryChange(currentEntry)) onDismiss()
                },
                enabled = validationErrors.isEmpty(),
            ) {
                Text("Готово")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Назад") } },
    )

}
