package mytracker

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.time.TimeSource
import mytracker.domain.MediaSearchResult
import mytracker.domain.MediaSearchRateLimitException
import mytracker.domain.MediaCategories
import mytracker.domain.MediaMetadata
import mytracker.domain.MediaSeasonMetadata
import mytracker.domain.MediaEntry
import mytracker.domain.MediaEntryValidator
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus

private const val InitialSearchDebounceMillis = 300L
private const val SearchDebounceStepMillis = 500L
private const val MaximumSearchDebounceMillis = 3000L
private const val FallbackSearchDelayMillis = 1500L

@Composable
internal fun AddEntryDialog(
    onDismiss: () -> Unit,
    initialCategory: String,
    currentDate: () -> String,
    searchMedia: suspend (String, String) -> List<MediaSearchResult>,
    searchFallbackMedia: suspend (String, String) -> List<MediaSearchResult>,
    loadMediaMetadata: suspend (MediaSearchResult) -> MediaMetadata,
    onAdd: (
        String,
        String,
        WatchStatus,
        String,
        Int?,
        MediaSearchResult?,
        List<String>,
        String,
        List<MediaSeasonMetadata>,
    ) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    var status by remember { mutableStateOf(WatchStatus.WANT_TO_WATCH) }
    var note by remember { mutableStateOf("") }
    var review by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf<Int?>(null) }
    var selectedResult by remember { mutableStateOf<MediaSearchResult?>(null) }
    var selectedCountries by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedSeasons by remember { mutableStateOf<List<MediaSeasonMetadata>>(emptyList()) }
    var isLoadingMetadata by remember { mutableStateOf(false) }
    var metadataLoadFailed by remember { mutableStateOf(false) }
    var categoryManuallySelected by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<MediaSearchResult>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(true) }
    var searchDebounceMillis by remember { mutableStateOf(InitialSearchDebounceMillis) }
    val validationErrors = MediaEntryValidator.validate(
        MediaEntry(
            id = "",
            title = title.trim(),
            categoryId = category,
            status = status,
            rating = rating,
            viewingNotes = note.trim().takeIf(String::isNotEmpty)?.let {
                listOf(ViewingNote(date = currentDate(), text = it))
            }.orEmpty(),
        ),
    )

    LaunchedEffect(title, category, isSearchExpanded, selectedResult) {
        val query = title.trim()
        if (selectedResult?.title == query) return@LaunchedEffect

        searchResults = emptyList()
        searchError = null
        hasSearched = false
        isSearching = false
        if (!isSearchExpanded || query.isBlank()) return@LaunchedEffect

        try {
            val searchStartedAt = TimeSource.Monotonic.markNow()
            delay(searchDebounceMillis)
            isSearching = true
            searchResults = searchMedia(query, category)
            if (searchResults.isEmpty()) {
                val remainingFallbackDelay =
                    (FallbackSearchDelayMillis - searchStartedAt.elapsedNow().inWholeMilliseconds).coerceAtLeast(0)
                if (remainingFallbackDelay > 0) delay(remainingFallbackDelay)
                searchResults = searchFallbackMedia(query, category)
            }
            hasSearched = true
            if (searchDebounceMillis > InitialSearchDebounceMillis) {
                searchDebounceMillis = (searchDebounceMillis - SearchDebounceStepMillis)
                    .coerceAtLeast(InitialSearchDebounceMillis)
            }
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (exception: MediaSearchRateLimitException) {
            searchDebounceMillis = (searchDebounceMillis + SearchDebounceStepMillis)
                .coerceAtMost(MaximumSearchDebounceMillis)
            searchError = buildString {
                append(
                    exception.message
                        ?: "TMDB \u0432\u0440\u0435\u043c\u0435\u043d\u043d\u043e \u043e\u0433\u0440\u0430\u043d\u0438\u0447\u0438\u043b \u0447\u0438\u0441\u043b\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432."
                )
                append(" \u041f\u0430\u0443\u0437\u0430 \u0434\u0438\u043d\u0430\u043c\u0438\u0447\u0435\u0441\u043a\u043e\u0433\u043e \u043f\u043e\u0438\u0441\u043a\u0430 \u0443\u0432\u0435\u043b\u0438\u0447\u0435\u043d\u0430 \u0434\u043e ")
                append(searchDebounceMillis)
                append(" \u043c\u0441.")
            }
            hasSearched = true
        } catch (exception: Exception) {
            searchError = exception.message ?: "TMDB search failed."
            hasSearched = true
        } finally {
            isSearching = false
        }
    }

    LaunchedEffect(selectedResult) {
        val result = selectedResult ?: run {
            selectedCountries = emptyList()
            selectedSeasons = emptyList()
            isLoadingMetadata = false
            metadataLoadFailed = false
            return@LaunchedEffect
        }
        selectedCountries = emptyList()
        selectedSeasons = emptyList()
        isLoadingMetadata = true
        metadataLoadFailed = false
        try {
            val metadata = loadMediaMetadata(result)
            selectedCountries = metadata.productionCountries
            selectedSeasons = metadata.seasons
            if (!categoryManuallySelected) {
                category = metadata.categorySuggestion?.categoryId
                    ?: MediaCategories.forMediaType(result.reference.mediaType)
            }
        } catch (exception: kotlinx.coroutines.CancellationException) {
            throw exception
        } catch (_: Exception) {
            selectedCountries = emptyList()
            selectedSeasons = emptyList()
            metadataLoadFailed = true
        } finally {
            isLoadingMetadata = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое произведение") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 590.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (isSearchExpanded || selectedResult == null) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it != selectedResult?.title) selectedResult = null
                        },
                        label = { Text("Название") },
                        singleLine = true,
                    )
                }
                if (isSearching) Text("\u0418\u0449\u0435\u043c...", color = Muted, fontSize = 12.sp)
                val selected = selectedResult
                if (!isSearchExpanded && selected != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MediaPoster(
                            url = selected.posterUrl,
                            title = selected.title,
                            modifier = Modifier.size(width = 64.dp, height = 96.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                selected.title,
                                color = Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                listOfNotNull(
                                    selected.releaseYear?.toString(),
                                    when {
                                        isLoadingMetadata -> "Загрузка метаданных…"
                                        selectedCountries.isNotEmpty() -> selectedCountries.joinToString(", ")
                                        metadataLoadFailed -> "Метаданные не удалось загрузить"
                                        else -> "Страна не указана"
                                    },
                                ).joinToString(" · "),
                                color = Muted,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TextButton(onClick = { isSearchExpanded = true }) { Text("Изменить") }
                    }
                }
                searchError?.let { Text(it, color = Color(0xFFE58A96), fontSize = 12.sp) }
                if (isSearchExpanded && searchResults.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 190.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        searchResults.forEach { result ->
                            val isSelected = result == selectedResult
                            Surface(
                                modifier = Modifier.fillMaxWidth().pointerInput(result) {
                                    detectTapGestures(
                                        onTap = {
                                            selectedResult = result
                                            title = result.title
                                            categoryManuallySelected = false
                                            category = MediaCategories.forMediaType(result.reference.mediaType)
                                        },
                                        onDoubleTap = {
                                            selectedResult = result
                                            title = result.title
                                            categoryManuallySelected = false
                                            category = MediaCategories.forMediaType(result.reference.mediaType)
                                            isSearchExpanded = false
                                        },
                                    )
                                },
                                color = if (isSelected) Color(0xFF302B43) else Panel,
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    MediaPoster(
                                        url = result.posterUrl,
                                        title = result.title,
                                        modifier = Modifier.size(width = 48.dp, height = 72.dp),
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            result.title,
                                            color = if (isSelected) Accent else Color(0xFFF1F0F6),
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            listOfNotNull(
                                                result.releaseYear?.toString(),
                                                if (result.reference.mediaType == "movie") "Фильм" else "Сериал",
                                            ).joinToString(" · "),
                                            color = Muted,
                                            fontSize = 12.sp,
                                        )
                                        result.overview?.let { overview ->
                                            Text(
                                                overview,
                                                color = Muted,
                                                fontSize = 11.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (isSearchExpanded && hasSearched && searchError == null) {
                    Text("Ничего не найдено. Можно добавить запись вручную.", color = Muted, fontSize = 12.sp)
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    minLines = 2,
                    maxLines = 4,
                )
                Text("Категория", color = Muted)
                val categoryOptions = when (selectedResult?.reference?.mediaType) {
                    "movie" -> listOf(MediaCategories.MOVIE, MediaCategories.ANIME_MOVIE)
                    "tv" -> listOf(MediaCategories.SERIES, MediaCategories.ANIME_SERIES)
                    else -> listOf(
                        MediaCategories.MOVIE,
                        MediaCategories.ANIME_MOVIE,
                        MediaCategories.SERIES,
                        MediaCategories.ANIME_SERIES,
                        MediaCategories.ANIME,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    categoryOptions.forEach { option ->
                        TextButton(
                            onClick = {
                                categoryManuallySelected = true
                                category = option
                                searchResults = emptyList()
                                hasSearched = false
                                searchError = null
                            },
                        ) {
                            Text(option, color = if (category == option) Accent else Muted)
                        }
                    }
                }
                Text("Статус", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WatchStatus.entries.forEach { option ->
                        TextButton(
                            modifier = Modifier.testTag("new-status-${option.name}"),
                            onClick = { status = option },
                        ) {
                            Text(option.label(), color = if (status == option) Accent else Muted)
                        }
                    }
                }
                if (status == WatchStatus.WATCHED) {
                    Text("Оцените просмотренное произведение", color = Muted, fontSize = 13.sp)
                    RatingPicker(value = rating, onValueChange = { rating = it })
                    OutlinedTextField(
                        value = review,
                        onValueChange = { review = it },
                        label = { Text("Рецензия (необязательно)") },
                        minLines = 2,
                        maxLines = 5,
                    )
                }
                validationErrors.forEach { error ->
                    Text(error.message, color = Color(0xFFE58A96), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("dialog-add"),
                onClick = {
                    onAdd(
                        title.trim(),
                        category,
                        status,
                        note,
                        rating,
                        selectedResult,
                        selectedCountries,
                        review,
                        selectedSeasons,
                    )
                },
                enabled = validationErrors.isEmpty(),
            ) {
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
