package mytracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Frame

private val TitleBarColor = Color(0xFF1A1D24)
private val TitleBarText = Color(0xFFB5A0FF)

fun main() = application {
    val windowState = rememberWindowState()
    var isMaximized by remember { mutableStateOf(false) }

    Window(
        onCloseRequest = ::exitApplication,
        title = "mytracker",
        state = windowState,
        undecorated = true,
    ) {
        Column(Modifier.fillMaxSize().background(Color(0xFF111318))) {
            WindowDraggableArea(Modifier.fillMaxWidth().height(32.dp)) {
                Row(
                    modifier = Modifier.fillMaxSize().background(TitleBarColor),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "mytracker",
                        color = TitleBarText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                    )
                    WindowControl(WindowControlIcon.Minimize, onClick = { window.extendedState = Frame.ICONIFIED })
                    WindowControl(
                        icon = if (isMaximized) WindowControlIcon.Restore else WindowControlIcon.Maximize,
                        onClick = {
                            isMaximized = !isMaximized
                            window.extendedState = if (isMaximized) Frame.MAXIMIZED_BOTH else Frame.NORMAL
                        },
                    )
                    WindowControl(WindowControlIcon.Close, closeButton = true, onClick = ::exitApplication)
                }
            }
            App()
        }
    }
}

@androidx.compose.runtime.Composable
private fun WindowControl(icon: WindowControlIcon, closeButton: Boolean = false, onClick: () -> Unit) {
    val background = if (closeButton) Color(0xFF7B303B) else TitleBarColor
    Box(
        modifier = Modifier.width(46.dp).height(32.dp)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.width(12.dp).height(12.dp)) {
            val iconColor = Color(0xFFE9E7EF)
            val strokeWidth = 1.2.dp.toPx()
            val inset = 2.dp.toPx()
            when (icon) {
                WindowControlIcon.Minimize -> drawLine(
                    color = iconColor,
                    start = Offset(inset, size.height / 2),
                    end = Offset(size.width - inset, size.height / 2),
                    strokeWidth = strokeWidth,
                )
                WindowControlIcon.Maximize -> drawRect(
                    color = iconColor,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(strokeWidth),
                )
                WindowControlIcon.Restore -> {
                    drawRect(
                        color = iconColor,
                        topLeft = Offset(inset + 1.dp.toPx(), inset),
                        size = Size(size.width - inset * 2, size.height - inset * 2 - 1.dp.toPx()),
                        style = Stroke(strokeWidth),
                    )
                    drawLine(
                        color = TitleBarColor,
                        start = Offset(inset + 1.dp.toPx(), inset + 2.dp.toPx()),
                        end = Offset(size.width - inset - 1.dp.toPx(), inset + 2.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawRect(
                        color = iconColor,
                        topLeft = Offset(inset, inset + 2.dp.toPx()),
                        size = Size(size.width - inset * 2 - 1.dp.toPx(), size.height - inset * 2 - 1.dp.toPx()),
                        style = Stroke(strokeWidth),
                    )
                }
                WindowControlIcon.Close -> {
                    drawLine(
                        color = iconColor,
                        start = Offset(inset, inset),
                        end = Offset(size.width - inset, size.height - inset),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = iconColor,
                        start = Offset(size.width - inset, inset),
                        end = Offset(inset, size.height - inset),
                        strokeWidth = strokeWidth,
                    )
                }
            }
        }
    }
}

private enum class WindowControlIcon {
    Minimize,
    Maximize,
    Restore,
    Close,
}

