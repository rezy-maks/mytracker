package mytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mytracker.domain.MediaEntry
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState

private val Background = Color(0xFF111318)
private val Panel = Color(0xFF1A1D24)
private val Muted = Color(0xFF9AA1AE)
private val Accent = Color(0xFFB5A0FF)

@Composable
fun App(
    initialEntries: List<MediaEntry> = emptyList(),
    onEntriesChanged: (List<MediaEntry>) -> Unit = {},
    currentDate: () -> String = { "" },
) {
    var selectedItem by remember { mutableStateOf("Главная") }
    val entries = remember { mutableStateListOf<MediaEntry>().apply { addAll(initialEntries) } }
    var isAddDialogVisible by remember { mutableStateOf(false) }
    var selectedEntryId by remember { mutableStateOf<String?>(null) }
    var entryPendingDeletion by remember { mutableStateOf<MediaEntry?>(null) }

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
            if (selectedEntry != null) {
                EntryDetailsDialog(
                    entry = selectedEntry,
                    currentDate = currentDate,
                    onDismiss = { selectedEntryId = null },
                    onEntryChange = { updatedEntry ->
                        val index = entries.indexOfFirst { it.id == updatedEntry.id }
                        if (index >= 0) {
                            entries[index] = updatedEntry
                            onEntriesChanged(entries.toList())
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
                                entries.removeAll { it.id == entry.id }
                                onEntriesChanged(entries.toList())
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
                    initialCategory = when (selectedItem) {
                        "Аниме" -> "Аниме"
                        "Сериалы" -> "Сериал"
                        else -> "Фильм"
                    },
                    onAdd = { title, category, status, noteText, rating ->
                        entries += MediaEntry(
                            id = "${entries.size}-${title.hashCode()}",
                            title = title,
                            categoryId = category,
                            status = status,
                            rating = rating,
                            viewingNotes = noteText.trim().takeIf { it.isNotEmpty() }?.let { text ->
                                listOf(ViewingNote(date = currentDate(), text = text))
                            }.orEmpty(),
                        )
                        onEntriesChanged(entries.toList())
                        isAddDialogVisible = false
                    },
                )
            }
        }
    }
}

@Composable
private fun Sidebar(selectedItem: String, onItemSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.width(230.dp).fillMaxSize()
            .background(Panel, RoundedCornerShape(20.dp)).padding(22.dp),
    ) {
        Text("mytracker", color = Accent, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(34.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("КАТАЛОГ", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            NavigationItem("Главная", selected = selectedItem == "Главная") { onItemSelected("Главная") }
            Spacer(Modifier.height(18.dp))
            Text("КАТЕГОРИИ", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            NavigationItem("Фильмы", selected = selectedItem == "Фильмы") { onItemSelected("Фильмы") }
            NavigationItem("Аниме", selected = selectedItem == "Аниме") { onItemSelected("Аниме") }
            NavigationItem("Сериалы", selected = selectedItem == "Сериалы") { onItemSelected("Сериалы") }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF343741)))
            Spacer(Modifier.height(18.dp))
            NavigationItem("Все произведения", selected = selectedItem == "Все произведения") {
                onItemSelected("Все произведения")
            }
        }
    }
}

@Composable
private fun HomeContent(entries: List<MediaEntry>, modifier: Modifier = Modifier) {
    val watchedCount = entries.count { it.status == WatchStatus.WATCHED }
    val wantToWatchCount = entries.count { it.status == WatchStatus.WANT_TO_WATCH }
    val abandonedCount = entries.count { it.status == WatchStatus.ABANDONED }

    Column(
        modifier = modifier.fillMaxSize().padding(top = 12.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column {
            Text("Добро пожаловать в mytracker", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Ваш личный каталог фильмов, аниме и сериалов", color = Muted, fontSize = 14.sp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Моя коллекция", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HomeStatCard("Всего произведений", entries.size, Modifier.weight(1f))
                HomeStatCard("Хочу посмотреть", wantToWatchCount, Modifier.weight(1f))
                HomeStatCard("Просмотрено", watchedCount, Modifier.weight(1f))
                HomeStatCard("Брошено", abandonedCount, Modifier.weight(1f))
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
                Text("Начните с выбора категории", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text(
                    if (entries.isEmpty()) {
                        "Категории находятся слева. Добавьте первое произведение в свой каталог."
                    } else {
                        "Категории находятся слева. Внутри каждой записи сгруппированы по статусу."
                    },
                    color = Muted,
                )
            }
        }
    }
}

@Composable
private fun HomeStatCard(label: String, value: Int, modifier: Modifier = Modifier) {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(value.toString(), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = Accent)
            Spacer(Modifier.height(4.dp))
            Text(label, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun CatalogEntryCard(
    entry: MediaEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val cardInteraction = remember(entry.id) { MutableInteractionSource() }
    val deleteInteraction = remember(entry.id) { MutableInteractionSource() }
    val isCardHovered by cardInteraction.collectIsHoveredAsState()
    val isDeleteHovered by deleteInteraction.collectIsHoveredAsState()

    Surface(
        color = Panel,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth()
                    .hoverable(cardInteraction)
                    .clickable(
                        interactionSource = cardInteraction,
                        indication = null,
                        onClick = onClick,
                    ),
            ) {
                if (isCardHovered && !isDeleteHovered) {
                    Box(
                        modifier = Modifier.matchParentSize()
                            .background(Color(0xFF252A34), RoundedCornerShape(14.dp)),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 56.dp).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(entry.title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(5.dp))
                        Text(entry.categoryId, color = Muted, fontSize = 13.sp)
                    }
                }
            }
            DeleteIconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                interactionSource = deleteInteraction,
            )
        }
    }
}

@Composable
private fun NavigationItem(label: String, selected: Boolean = false, onClick: () -> Unit) {
    val textColor = if (selected) Color(0xFFF1F0F6) else Muted
    val background = if (selected) Color(0xFF2A2638) else Color.Transparent
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(background, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Text(label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
private fun CatalogContent(
    entries: List<MediaEntry>,
    selectedItem: String,
    onAddClick: () -> Unit,
    onEntryClick: (MediaEntry) -> Unit,
    onDeleteClick: (MediaEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleEntries = entries.filter { entry ->
        when (selectedItem) {
            "Фильмы" -> entry.categoryId == "Фильм"
            "Аниме" -> entry.categoryId == "Аниме"
            "Сериалы" -> entry.categoryId == "Сериал"
            else -> true
        }
    }
    val statusGroups = listOf(
        WatchStatus.WANT_TO_WATCH,
        WatchStatus.WATCHED,
        WatchStatus.ABANDONED,
    )
    Column(modifier = modifier.fillMaxSize().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(selectedItem, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Ваш личный каталог фильмов, сериалов и аниме", color = Muted, fontSize = 14.sp)
            }
            Button(
                onClick = onAddClick,
                modifier = Modifier.height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    contentColor = Color(0xFF201A32),
                ),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                baselineShift = BaselineShift(0.08f),
                            ),
                        ) {
                            append("Добавить")
                        }
                    },
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        if (visibleEntries.isEmpty()) Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    if (entries.isEmpty() && selectedItem == "Все произведения") {
                        "Коллекция пока пуста"
                    } else {
                        "В этой категории пока нет произведений"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (entries.isEmpty() && selectedItem == "Все произведения") {
                        "Добавьте первое произведение, которое хотите посмотреть"
                    } else {
                        "Добавьте произведение, чтобы оно появилось здесь"
                    },
                    color = Muted,
                    fontSize = 14.sp,
                )
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                statusGroups.forEach { status ->
                    val groupEntries = visibleEntries.filter { it.status == status }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(status.label(), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                        if (groupEntries.isEmpty()) {
                            Text("Нет произведений", color = Muted, fontSize = 13.sp)
                        } else {
                            groupEntries.forEach { entry ->
                                CatalogEntryCard(
                                    entry = entry,
                                    onClick = { onEntryClick(entry) },
                                    onDelete = { onDeleteClick(entry) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryDetailsDialog(
    entry: MediaEntry,
    currentDate: () -> String,
    onDismiss: () -> Unit,
    onEntryChange: (MediaEntry) -> Unit,
) {
    var currentEntry by remember(entry.id) { mutableStateOf(entry) }
    var noteText by remember { mutableStateOf("") }
    var noteTargetSeason by remember { mutableStateOf<Int?>(null) }
    var isNotePopupVisible by remember { mutableStateOf(false) }
    var reviewText by remember(entry.id) { mutableStateOf(entry.notes) }
    var isReviewPopupVisible by remember { mutableStateOf(false) }
    var isStatusMenuVisible by remember { mutableStateOf(false) }
    var isRatingRequired by remember(entry.id) {
        mutableStateOf(entry.status == WatchStatus.WATCHED && entry.rating == null)
    }

    fun updateEntry(updatedEntry: MediaEntry) {
        currentEntry = updatedEntry
    }

    fun showNoteForm(seasonNumber: Int?) {
        noteTargetSeason = seasonNumber
        noteText = ""
        isNotePopupVisible = true
    }

    fun saveNote() {
        val note = ViewingNote(date = currentDate(), text = noteText.trim())
        val updatedEntry = if (noteTargetSeason == null) {
            currentEntry.copy(viewingNotes = currentEntry.viewingNotes + note)
        } else {
            currentEntry.copy(
                seasons = currentEntry.seasons.map { season ->
                    if (season.seasonNumber == noteTargetSeason) {
                        season.copy(viewingNotes = season.viewingNotes + note)
                    } else {
                        season
                    }
                },
            )
        }
        updateEntry(updatedEntry)
        isNotePopupVisible = false
    }

    val hasSeasons = currentEntry.categoryId != "Фильм"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(currentEntry.title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(currentEntry.categoryId, color = Muted)
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
                                text = { Text(status.label()) },
                                onClick = {
                                    if (status == WatchStatus.WATCHED && currentEntry.rating == null) {
                                        isRatingRequired = true
                                    } else {
                                        updateEntry(currentEntry.copy(status = status))
                                        isRatingRequired = false
                                    }
                                    isStatusMenuVisible = false
                                },
                            )
                        }
                    }
                }
                if (isRatingRequired) {
                    Text("Чтобы отметить произведение просмотренным, выберите оценку", color = Accent)
                }
                if (isRatingRequired || currentEntry.status == WatchStatus.WATCHED) {
                    RatingPicker(
                        value = currentEntry.rating,
                        onValueChange = { rating ->
                            updateEntry(
                                currentEntry.copy(
                                    status = if (isRatingRequired) WatchStatus.WATCHED else currentEntry.status,
                                    rating = rating,
                                ),
                            )
                            isRatingRequired = false
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
                    TextButton(onClick = { showNoteForm(null) }) {
                        Text("Добавить заметку")
                    }
                    DropdownMenu(
                        expanded = isNotePopupVisible && noteTargetSeason == null,
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
                                    Text("Сезон ${season.seasonNumber}", fontWeight = FontWeight.Medium)
                                    if (season.viewingNotes.isEmpty()) {
                                        Text("Заметок пока нет", color = Muted, fontSize = 12.sp)
                                    } else {
                                        season.viewingNotes.forEachIndexed { index, note ->
                                            ViewingNoteRow(note) {
                                                updateEntry(
                                                    currentEntry.copy(
                                                        seasons = currentEntry.seasons.map { currentSeason ->
                                                            if (currentSeason.seasonNumber == season.seasonNumber) {
                                                                currentSeason.copy(
                                                                    viewingNotes = currentSeason.viewingNotes.filterIndexed { noteIndex, _ ->
                                                                        noteIndex != index
                                                                    },
                                                                )
                                                            } else {
                                                                currentSeason
                                                            }
                                                        },
                                                    ),
                                                )
                                            }
                                        }
                                    }
                                    Box {
                                        TextButton(onClick = { showNoteForm(season.seasonNumber) }) {
                                            Text("Добавить заметку к сезону")
                                        }
                                        DropdownMenu(
                                            expanded = isNotePopupVisible && noteTargetSeason == season.seasonNumber,
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
                    onEntryChange(currentEntry)
                    onDismiss()
                },
                enabled = !isRatingRequired,
            ) {
                Text("Готово")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Назад") } },
    )

}

@Composable
private fun ViewingNoteRow(note: ViewingNote, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "${note.date.toDisplayDate()} · ${note.text}",
            modifier = Modifier.weight(1f),
            color = Muted,
            fontSize = 13.sp,
        )
        DeleteIconButton(onClick = onDelete)
    }
}

@Composable
private fun DeleteIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
) {
    val buttonInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val isHovered by buttonInteraction.collectIsHoveredAsState()

    Box(
        modifier = modifier.size(32.dp)
            .clip(CircleShape)
            .background(if (isHovered) Color(0xFF65323D) else Color(0xFF482A31), CircleShape)
            .hoverable(buttonInteraction)
            .clickable(
                interactionSource = buttonInteraction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val iconColor = if (isHovered) Color(0xFFFFB4BE) else Color(0xFFE58A96)
            val stroke = 1.5.dp.toPx()
            val unitX = size.width / 16f
            val unitY = size.height / 16f
            fun point(x: Float, y: Float) = Offset(x * unitX, y * unitY)

            drawLine(iconColor, point(2.5f, 4.5f), point(13.5f, 4.5f), stroke, StrokeCap.Round)
            drawLine(iconColor, point(6.5f, 2.5f), point(9.5f, 2.5f), stroke, StrokeCap.Round)
            drawLine(iconColor, point(8f, 2.5f), point(8f, 4.5f), stroke, StrokeCap.Round)
            drawLine(iconColor, point(4.2f, 5.5f), point(5.1f, 13f), stroke, StrokeCap.Round)
            drawLine(iconColor, point(11.8f, 5.5f), point(10.9f, 13f), stroke, StrokeCap.Round)
            drawLine(iconColor, point(5.1f, 13f), point(10.9f, 13f), stroke, StrokeCap.Round)
            drawLine(iconColor, point(7f, 6.5f), point(7f, 11.5f), stroke, StrokeCap.Round)
            drawLine(iconColor, point(9f, 6.5f), point(9f, 11.5f), stroke, StrokeCap.Round)
        }
    }
}

private fun String.toDisplayDate(): String =
    if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(this)) {
        "${substring(8, 10)}.${substring(5, 7)}.${substring(0, 4)}"
    } else {
        this
    }

@Composable
private fun NotePopupContent(
    text: String,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier.width(320.dp).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Новая заметка", fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("Текст заметки") },
            minLines = 2,
            maxLines = 4,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onSave, enabled = text.isNotBlank()) { Text("Сохранить") }
            TextButton(onClick = onCancel) { Text("Отмена") }
        }
    }
}

@Composable
private fun RatingPicker(value: Int?, onValueChange: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Оценка произведения", color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (1..5).forEach { rating ->
                TextButton(onClick = { onValueChange(rating) }) {
                    Text(rating.toString(), color = if (value == rating) Accent else Muted)
                }
            }
        }
    }
}

@Composable
private fun AddEntryDialog(
    onDismiss: () -> Unit,
    initialCategory: String,
    onAdd: (String, String, WatchStatus, String, Int?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    var status by remember { mutableStateOf(WatchStatus.WANT_TO_WATCH) }
    var note by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое произведение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка (необязательно)") },
                    minLines = 2,
                    maxLines = 4,
                )
                Text("Категория", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Фильм", "Сериал", "Аниме").forEach { option ->
                        TextButton(onClick = { category = option }) {
                            Text(option, color = if (category == option) Accent else Muted)
                        }
                    }
                }
                Text("Статус", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WatchStatus.entries.forEach { option ->
                        TextButton(onClick = { status = option }) {
                            Text(option.label(), color = if (status == option) Accent else Muted)
                        }
                    }
                }
                if (status == WatchStatus.WATCHED) {
                    Text("Оцените просмотренное произведение", color = Muted, fontSize = 13.sp)
                    RatingPicker(value = rating, onValueChange = { rating = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(title.trim(), category, status, note, rating) },
                enabled = title.isNotBlank() && (status != WatchStatus.WATCHED || rating != null),
            ) {
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

private fun WatchStatus.label(): String = when (this) {
    WatchStatus.WANT_TO_WATCH -> "Хочу посмотреть"
    WatchStatus.ABANDONED -> "Брошено"
    WatchStatus.WATCHED -> "Просмотрено"
}

