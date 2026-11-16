package mytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF111318)
private val Panel = Color(0xFF1A1D24)
private val Muted = Color(0xFF9AA1AE)
private val Accent = Color(0xFFB5A0FF)

@Composable
fun App() {
    var selectedItem by remember { mutableStateOf("Все произведения") }

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
                CatalogContent(modifier = Modifier.weight(1f))
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
            NavigationItem("Все произведения", selected = selectedItem == "Все произведения") {
                onItemSelected("Все произведения")
            }
            NavigationItem("Хочу посмотреть", selected = selectedItem == "Хочу посмотреть") {
                onItemSelected("Хочу посмотреть")
            }
            NavigationItem("Просмотрено", selected = selectedItem == "Просмотрено") {
                onItemSelected("Просмотрено")
            }
            NavigationItem("Брошено", selected = selectedItem == "Брошено") {
                onItemSelected("Брошено")
            }
            Spacer(Modifier.height(28.dp))
            Text("КАТЕГОРИИ", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            NavigationItem("Фильм", selected = selectedItem == "Фильм") { onItemSelected("Фильм") }
            NavigationItem("Сериал", selected = selectedItem == "Сериал") { onItemSelected("Сериал") }
            NavigationItem("Аниме", selected = selectedItem == "Аниме") { onItemSelected("Аниме") }
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
private fun CatalogContent(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().padding(top = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text("Все произведения", fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Ваш личный каталог фильмов, сериалов и аниме", color = Muted, fontSize = 14.sp)
            }
            Button(
                onClick = {},
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
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Коллекция пока пуста", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("Добавьте первое произведение, которое хотите посмотреть", color = Muted, fontSize = 14.sp)
            }
        }
    }
}

