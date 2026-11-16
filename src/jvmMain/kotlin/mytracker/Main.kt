package mytracker

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.sun.jna.Native
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import com.sun.jna.win32.W32APIOptions

private const val DWMWA_USE_IMMERSIVE_DARK_MODE = 20

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "mytracker",
        undecorated = false,
    ) {
        DisposableEffect(window) {
            if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
                val hwnd = HWND(Native.getComponentPointer(window))
                val enabled = IntByReference(1)
                DWM_API.DwmSetWindowAttribute(
                    hwnd,
                    DWMWA_USE_IMMERSIVE_DARK_MODE,
                    enabled.pointer,
                    Int.SIZE_BYTES,
                )
            }
            onDispose { }
        }
        App()
    }
}

private interface DwmApi : StdCallLibrary {
    fun DwmSetWindowAttribute(hwnd: HWND, attribute: Int, value: com.sun.jna.Pointer, valueSize: Int): Int

    companion object {
        val INSTANCE: DwmApi = Native.load("dwmapi", DwmApi::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }
}

private val DWM_API = DwmApi.INSTANCE
