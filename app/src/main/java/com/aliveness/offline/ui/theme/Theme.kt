package com.aliveness.offline.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aliveness.offline.data.AppSettings
import com.aliveness.offline.data.ThemeMode

// Same palette and accent as the BitChord reference (Apple-Music-style monochrome + one red).
val AccentRed = Color(0xFFFA2D48)

private val DarkColors = darkColorScheme(
    primary = Color.White, onPrimary = Color.Black,
    background = Color.Black, onBackground = Color.White,
    surface = Color(0xFF0D0D0F), onSurface = Color.White,
    surfaceVariant = Color(0xFF1C1C1E), onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF2C2C2E),
)

private val LightColors = lightColorScheme(
    primary = Color.Black, onPrimary = Color.White,
    background = Color.White, onBackground = Color.Black,
    surface = Color(0xFFF7F7F9), onSurface = Color.Black,
    surfaceVariant = Color(0xFFF2F2F7), onSurfaceVariant = Color(0xFF6E6E73),
    outline = Color(0xFFE5E5EA),
)

private val Family = FontFamily.SansSerif

// Same scale as the reference: heavy, tightly tracked headings over regular body text.
private val AlivenessTypography = Typography(
    displayLarge = TextStyle(fontFamily = Family, fontWeight = FontWeight.W800, fontSize = 34.sp, letterSpacing = (-0.8).sp),
    headlineLarge = TextStyle(fontFamily = Family, fontWeight = FontWeight.W800, fontSize = 30.sp, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontFamily = Family, fontWeight = FontWeight.W700, fontSize = 22.sp, letterSpacing = (-0.4).sp),
    titleLarge = TextStyle(fontFamily = Family, fontWeight = FontWeight.W700, fontSize = 20.sp, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontFamily = Family, fontWeight = FontWeight.W600, fontSize = 16.sp, letterSpacing = (-0.2).sp),
    titleSmall = TextStyle(fontFamily = Family, fontWeight = FontWeight.W600, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = Family, fontWeight = FontWeight.W400, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Family, fontWeight = FontWeight.W400, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Family, fontWeight = FontWeight.W400, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Family, fontWeight = FontWeight.W600, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Family, fontWeight = FontWeight.W600, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Family, fontWeight = FontWeight.W600, fontSize = 11.sp),
)

/** Layout constants shared by every screen so spacing stays consistent. */
object Dimens {
    val PageGutter = 20.dp
    val ShelfGap = 14.dp
    val CardRadius = 18.dp
    val SmallRadius = 12.dp
    val ThumbRadius = 7.dp
    /** Space reserved at the bottom for the floating navigation bar. */
    val NavBarSpace = 124.dp
}

/** Decoded thumbnail edge (px), driven by the Thumbnail quality setting. */
val LocalThumbPx = staticCompositionLocalOf { 420 }

@Composable
fun AlivenessTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val scheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = scheme, typography = AlivenessTypography, content = content)
}
