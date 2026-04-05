package fumi.day.literalagenda.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import fumi.day.literalagenda.R

val ScopeOne = FontFamily(
    Font(R.font.scopeone, FontWeight.Normal)
)

fun getTypography(fontChoice: String, fontSize: Float = 16f): Typography {
    val fontFamily = when (fontChoice) {
        "serif" -> FontFamily.Serif
        "mono" -> FontFamily.Monospace
        "scopeone" -> ScopeOne
        else -> FontFamily.Default
    }

    return Typography(
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * 1.5f).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (fontSize - 2f).sp,
            lineHeight = (fontSize * 1.4f).sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (fontSize + 4f).sp,
            lineHeight = (fontSize * 1.6f).sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (fontSize + 2f).sp,
            lineHeight = (fontSize * 1.5f).sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (fontSize - 4f).sp,
            lineHeight = (fontSize * 1.3f).sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (fontSize - 5f).sp,
            lineHeight = (fontSize * 1.2f).sp
        )
    )
}

val Typography = getTypography("system", 16f)
