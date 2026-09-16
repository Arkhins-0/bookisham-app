package com.bookisham.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * A reading room: warm paper, dark ink, one ember of colour for the actions.
 * The reader itself runs on night, a near-black that keeps a white page from
 * glaring in a dark room. The same palette as tailwind.config.ts on the web.
 */
val Paper = Color(0xFFF6F1E7)
val PaperDeep = Color(0xFFEDE5D6)
val Ink = Color(0xFF1B1A17)
val InkSoft = Color(0xFF4A4741)
val InkFaint = Color(0xFF8A857B)
val Ember = Color(0xFFC2410C)
val EmberDark = Color(0xFF9A3412)
val Night = Color(0xFF0E0E0D)
val NightPanel = Color(0xFF1A1A18)
val NightLine = Color(0xFF2B2B28)
val WhatsAppGreen = Color(0xFF25D366)

/** Display is a serif, as Fraunces is on the web; body is the system sans, as Inter is. */
val Display: FontFamily = FontFamily.Serif
val Body: FontFamily = FontFamily.SansSerif

private val scheme = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = PaperDeep,
    onPrimaryContainer = Ink,
    secondary = Ember,
    onSecondary = Color.White,
    tertiary = EmberDark,
    onTertiary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = PaperDeep,
    onSurfaceVariant = InkSoft,
    outline = Ink.copy(alpha = 0.15f),
    outlineVariant = Ink.copy(alpha = 0.10f),
    error = EmberDark,
    onError = Color.White,
)

val BookishamTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontFamily = Display, fontSize = 32.sp, lineHeight = 36.sp),
    headlineLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp),
    headlineMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 24.sp),
    titleMedium = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Display, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 15.sp, lineHeight = 22.sp),
    bodySmall = TextStyle(fontFamily = Body, fontSize = 13.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 1.sp),
    labelSmall = TextStyle(fontFamily = Body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 2.5.sp),
)

@Composable
fun BookishamTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = BookishamTypography, content = content)
}
