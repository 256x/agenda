package fumi.day.literalagenda.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    background = Color.White,
    surface = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black.copy(alpha = 0.6f)
)

fun parseColor(hex: String): Color? {
    return try {
        if (hex.isBlank()) return null
        val cleanHex = hex.removePrefix("#")
        Color(android.graphics.Color.parseColor("#$cleanHex"))
    } catch (e: Exception) {
        null
    }
}

@Composable
fun LiteralAgendaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    bgColor: String = "",
    textColor: String = "",
    accentColor: String = "",
    fontChoice: String = "system",
    fontSize: Float = 16f,
    content: @Composable () -> Unit
) {
    val baseScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val colorScheme = baseScheme.copy(
        background = parseColor(bgColor) ?: baseScheme.background,
        surface = parseColor(bgColor) ?: baseScheme.surface,
        onBackground = parseColor(textColor) ?: baseScheme.onBackground,
        onSurface = parseColor(textColor) ?: baseScheme.onSurface,
        primary = parseColor(accentColor) ?: baseScheme.primary
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = getTypography(fontChoice, fontSize),
        content = content
    )
}
