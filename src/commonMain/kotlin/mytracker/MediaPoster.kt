package mytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage

@Composable
internal fun MediaPoster(
    url: String?,
    title: String,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember(url) { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Panel)
            .clickable(enabled = !url.isNullOrBlank()) { isExpanded = true },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Нет\nпостера",
            color = Muted,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = "Постер: $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (isExpanded && !url.isNullOrBlank()) {
        Dialog(onDismissRequest = { isExpanded = false }) {
            Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AsyncImage(
                        model = url.replace("/w342/", "/original/"),
                        contentDescription = "Увеличенный постер: $title",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(width = 360.dp, height = 540.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(title, color = Color(0xFFF1F0F6), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
