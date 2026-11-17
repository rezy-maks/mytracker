package mytracker

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mytracker.domain.MediaEntry
import mytracker.domain.MediaCategories
import mytracker.domain.WatchStatus
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState

@Composable
internal fun Sidebar(
    selectedItem: String,
    onItemSelected: (String) -> Unit,
    onAboutClick: () -> Unit,
) {
    val animeScreens = setOf("Аниме", "Аниме-сериалы", "Аниме-фильмы")
    var isAnimeExpanded by remember { mutableStateOf(selectedItem in animeScreens) }

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
            NavigationItem(
                label = "Аниме  ${if (isAnimeExpanded) "▾" else "▸"}",
                selected = selectedItem in animeScreens,
            ) {
                isAnimeExpanded = !isAnimeExpanded
                onItemSelected("Аниме")
            }
            if (isAnimeExpanded) {
                NavigationItem(
                    label = "Аниме-сериалы",
                    selected = selectedItem == "Аниме-сериалы",
                    indented = true,
                ) { onItemSelected("Аниме-сериалы") }
                NavigationItem(
                    label = "Аниме-фильмы",
                    selected = selectedItem == "Аниме-фильмы",
                    indented = true,
                ) { onItemSelected("Аниме-фильмы") }
            }
            NavigationItem("Сериалы", selected = selectedItem == "Сериалы") { onItemSelected("Сериалы") }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF343741)))
            Spacer(Modifier.height(18.dp))
            NavigationItem("Все произведения", selected = selectedItem == "Все произведения") {
                onItemSelected("Все произведения")
            }
        }
        TextButton(onClick = onAboutClick) { Text("О приложении", color = Muted) }
    }
}

@Composable
internal fun HomeContent(entries: List<MediaEntry>, modifier: Modifier = Modifier) {
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
                    MediaPoster(
                        url = entry.posterUrl,
                        title = entry.title,
                        modifier = Modifier.size(width = 48.dp, height = 72.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(entry.title, fontSize = 17.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(5.dp))
                        val metadata = listOfNotNull(
                            entry.releaseYear?.toString(),
                            entry.productionCountries.takeIf { it.isNotEmpty() }?.joinToString(", "),
                        ).joinToString(" · ")
                        if (metadata.isNotEmpty()) {
                            Text(metadata, color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(3.dp))
                        }
                        Text(entry.categoryId, color = Muted, fontSize = 13.sp)
                    }
                }
            }
            DeleteIconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
                interactionSource = deleteInteraction,
                contentDescription = "Удалить ${entry.title}",
            )
        }
    }
}

@Composable
private fun NavigationItem(
    label: String,
    selected: Boolean = false,
    indented: Boolean = false,
    onClick: () -> Unit,
) {
    val textColor = if (selected) Color(0xFFF1F0F6) else Muted
    val background = if (selected) Color(0xFF2A2638) else Color.Transparent
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(background, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(start = if (indented) 24.dp else 12.dp, end = 12.dp, top = 11.dp, bottom = 11.dp),
    ) {
        Text(label, color = textColor, fontSize = 14.sp)
    }
}

@Composable
internal fun CatalogContent(
    entries: List<MediaEntry>,
    selectedItem: String,
    onAddClick: () -> Unit,
    onEntryClick: (MediaEntry) -> Unit,
    onDeleteClick: (MediaEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleEntries = entries.filter { entry ->
        when (selectedItem) {
            "Фильмы" -> entry.categoryId == MediaCategories.MOVIE
            "Аниме" -> MediaCategories.isAnime(entry.categoryId)
            "Аниме-сериалы" -> entry.categoryId == MediaCategories.ANIME_SERIES ||
                (entry.categoryId == MediaCategories.ANIME && entry.externalReference?.mediaType == "tv")
            "Аниме-фильмы" -> entry.categoryId == MediaCategories.ANIME_MOVIE ||
                (entry.categoryId == MediaCategories.ANIME && entry.externalReference?.mediaType == "movie")
            "Сериалы" -> entry.categoryId == MediaCategories.SERIES
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
                modifier = Modifier.height(44.dp).testTag("catalog-add"),
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
