package mytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import mytracker.resources.Res
import mytracker.resources.tmdb_logo
import org.jetbrains.compose.resources.painterResource
import mytracker.domain.MediaEntry
import mytracker.domain.MediaCategories
import mytracker.domain.MediaMetadata
import mytracker.domain.MediaEntryIdGenerator
import mytracker.domain.MediaEntryValidator
import mytracker.domain.MediaSearchResult
import mytracker.domain.SeasonProgress
import mytracker.domain.ViewingNote

private val mediaEntryIdGenerator = MediaEntryIdGenerator()

@Composable
fun App(
    initialEntries: List<MediaEntry> = emptyList(),
    onEntriesChanged: (List<MediaEntry>) -> Result<Unit> = { Result.success(Unit) },
    initialStorageError: String? = null,
    currentDate: () -> String = { "" },
    searchMedia: suspend (query: String, category: String) -> List<MediaSearchResult> = { _, _ -> emptyList() },
    searchFallbackMedia: suspend (query: String, category: String) -> List<MediaSearchResult> = { _, _ -> emptyList() },
    loadMediaMetadata: suspend (MediaSearchResult) -> MediaMetadata = { MediaMetadata(emptyList(), null) },
) {
    var selectedItem by remember { mutableStateOf("Главная") }
    val entries = remember { mutableStateListOf<MediaEntry>().apply { addAll(initialEntries) } }
    var storageError by remember { mutableStateOf(initialStorageError) }
    var isAddDialogVisible by remember { mutableStateOf(false) }
    var isAboutDialogVisible by remember { mutableStateOf(false) }
    var selectedEntryId by remember { mutableStateOf<String?>(null) }
    var entryPendingDeletion by remember { mutableStateOf<MediaEntry?>(null) }

    fun commitEntries(updatedEntries: List<MediaEntry>, changedEntry: MediaEntry? = null): Boolean {
        if (changedEntry != null && MediaEntryValidator.validate(changedEntry).isNotEmpty()) return false
        if (initialStorageError != null) {
            storageError = initialStorageError
            return false
        }
        storageError = null
        onEntriesChanged(updatedEntries)
            .onSuccess {
                entries.clear()
                entries.addAll(updatedEntries)
            }
            .onFailure { error ->
                storageError = error.message ?: error::class.simpleName ?: "Неизвестная ошибка"
            }
        return storageError == null
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Background,
            surface = Panel,
            primary = Accent,
            onBackground = Color(0xFFF1F0F6),
            onSurface = Color(0xFFF1F0F6),
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            Row(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                Sidebar(
                    selectedItem = selectedItem,
                    onItemSelected = { selectedItem = it },
                    onAboutClick = { isAboutDialogVisible = true },
                )
                Spacer(Modifier.width(24.dp))
                if (selectedItem == "Главная") {
                    HomeContent(entries = entries, modifier = Modifier.weight(1f))
                } else {
                    CatalogContent(
                        entries = entries,
                        selectedItem = selectedItem,
                        onAddClick = { isAddDialogVisible = true },
                        onEntryClick = { selectedEntryId = it.id },
                        onDeleteClick = { entryPendingDeletion = it },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            val selectedEntry = entries.firstOrNull { it.id == selectedEntryId }
            if (isAboutDialogVisible) {
                val uriHandler = LocalUriHandler.current
                AlertDialog(
                    onDismissRequest = { isAboutDialogVisible = false },
                    title = { Text("О приложении") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Image(
                                painter = painterResource(Res.drawable.tmdb_logo),
                                contentDescription = "The Movie Database",
                                modifier = Modifier.width(160.dp).height(22.dp),
                            )
                            Text("This product uses the TMDB API but is not endorsed or certified by TMDB.")
                            TextButton(onClick = { uriHandler.openUri("https://www.themoviedb.org") }) {
                                Text("The Movie Database", color = Accent)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { isAboutDialogVisible = false }) { Text("Закрыть") }
                    },
                )
            }
            if (selectedEntry != null) {
                EntryDetailsDialog(
                    entry = selectedEntry,
                    currentDate = currentDate,
                    onDismiss = { selectedEntryId = null },
                    onEntryChange = { updatedEntry ->
                        val index = entries.indexOfFirst { it.id == updatedEntry.id }
                        if (index >= 0) {
                            commitEntries(
                                entries.toList().toMutableList().also { it[index] = updatedEntry },
                                changedEntry = updatedEntry,
                            )
                        } else {
                            false
                        }
                    },
                )
            }
            entryPendingDeletion?.let { entry ->
                AlertDialog(
                    onDismissRequest = { entryPendingDeletion = null },
                    title = { Text("Удалить произведение?") },
                    text = { Text("Запись «${entry.title}» будет удалена из каталога.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                commitEntries(entries.filterNot { it.id == entry.id })
                                entryPendingDeletion = null
                            },
                        ) {
                            Text("Удалить", color = Color(0xFFE58A96))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { entryPendingDeletion = null }) { Text("Отмена") }
                    },
                )
            }
            if (isAddDialogVisible) {
                AddEntryDialog(
                    onDismiss = { isAddDialogVisible = false },
                    searchMedia = searchMedia,
                    searchFallbackMedia = searchFallbackMedia,
                    loadMediaMetadata = loadMediaMetadata,
                    currentDate = currentDate,
                    initialCategory = when (selectedItem) {
                        "Аниме-сериалы" -> MediaCategories.ANIME_SERIES
                        "Аниме-фильмы" -> MediaCategories.ANIME_MOVIE
                        "Аниме" -> MediaCategories.ANIME
                        "Сериалы" -> MediaCategories.SERIES
                        else -> MediaCategories.MOVIE
                    },
                    onAdd = { title, category, status, noteText, rating, result, countries, review, seasons ->
                        val updatedEntries = entries.toList() + MediaEntry(
                            id = mediaEntryIdGenerator.generate(entries.mapTo(mutableSetOf()) { it.id }),
                            title = title,
                            categoryId = category,
                            externalReference = result?.reference,
                            posterUrl = result?.posterUrl,
                            releaseYear = result?.releaseYear,
                            productionCountries = countries,
                            status = status,
                            rating = rating,
                            notes = review.trim(),
                            seasons = seasons.map { season ->
                                SeasonProgress(
                                    seasonNumber = season.seasonNumber,
                                    name = season.name,
                                    episodeCount = season.episodeCount,
                                    firstAirDate = season.firstAirDate,
                                )
                            },
                            viewingNotes = noteText.trim().takeIf { it.isNotEmpty() }?.let { text ->
                                listOf(ViewingNote(date = currentDate(), text = text))
                            }.orEmpty(),
                        )
                        if (commitEntries(updatedEntries, changedEntry = updatedEntries.last())) {
                            isAddDialogVisible = false
                        }
                    },
                )
            }
            storageError?.let { error ->
                AlertDialog(
                    onDismissRequest = { storageError = null },
                    title = { Text("Ошибка работы с каталогом") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { storageError = null }) { Text("Понятно") }
                    },
                )
            }
        }
    }
}
