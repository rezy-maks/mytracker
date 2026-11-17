package mytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mytracker.domain.ViewingNote
import mytracker.domain.WatchStatus
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState

@Composable
internal fun ViewingNoteRow(note: ViewingNote, onDelete: () -> Unit) {
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
internal fun DeleteIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource? = null,
    contentDescription: String = "Удалить",
) {
    val buttonInteraction = interactionSource ?: remember { MutableInteractionSource() }
    val isHovered by buttonInteraction.collectIsHoveredAsState()

    Box(
        modifier = modifier.size(32.dp)
            .clip(CircleShape)
            .background(if (isHovered) Color(0xFF65323D) else Color(0xFF482A31), CircleShape)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
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

internal fun String.toDisplayDate(): String =
    if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(this)) {
        "${substring(8, 10)}.${substring(5, 7)}.${substring(0, 4)}"
    } else {
        this
    }

@Composable
internal fun NotePopupContent(
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
internal fun RatingPicker(
    value: Int?,
    onValueChange: (Int) -> Unit,
    tagPrefix: String = "rating",
    label: String = "Оценка произведения",
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (1..5).forEach { rating ->
                TextButton(
                    modifier = Modifier.testTag("$tagPrefix-$rating"),
                    onClick = { onValueChange(rating) },
                ) {
                    Text(rating.toString(), color = if (value == rating) Accent else Muted)
                }
            }
        }
    }
}

internal fun WatchStatus.label(): String = when (this) {
    WatchStatus.WANT_TO_WATCH -> "Хочу посмотреть"
    WatchStatus.ABANDONED -> "Брошено"
    WatchStatus.WATCHED -> "Просмотрено"
}
