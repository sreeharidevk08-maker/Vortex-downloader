package com.vortex.downloader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vortex.downloader.data.model.ThemeMode

val MidnightBlack = Color(0xFF070A12)
val DarkCharcoal = Color(0xFF111625)
val ElectricBlue = Color(0xFF2E7DFF)
val SoftViolet = Color(0xFF8B5CF6)
val SubtleCyan = Color(0xFF22D3EE)
val FrostedWhite = Color(0xFFFFFFFF)
val LightSurface = Color(0xFFF3F6FC)

@Immutable
data class VortexGlassPalette(
    val isDark: Boolean,
    val backgroundBase: Color,
    val backgroundSecondary: Color,
    val cardFillTop: Color,
    val cardFillBottom: Color,
    val cardBorder: Color,
    val navBarFill: Color,
    val navBarBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accentBlue: Color,
    val accentViolet: Color,
    val accentCyan: Color,
    val errorColor: Color,
    val successColor: Color
)

private val DarkGlassPalette = VortexGlassPalette(
    isDark = true,
    backgroundBase = MidnightBlack,
    backgroundSecondary = DarkCharcoal,
    cardFillTop = Color(0x26FFFFFF),
    cardFillBottom = Color(0x10FFFFFF),
    cardBorder = Color(0x33FFFFFF),
    navBarFill = Color(0xCC111625),
    navBarBorder = Color(0x33FFFFFF),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFA3B1C6),
    accentBlue = ElectricBlue,
    accentViolet = SoftViolet,
    accentCyan = SubtleCyan,
    errorColor = Color(0xFFFF5C7A),
    successColor = Color(0xFF34D399)
)

private val LightGlassPalette = VortexGlassPalette(
    isDark = false,
    backgroundBase = LightSurface,
    backgroundSecondary = Color(0xFFE7EEF9),
    cardFillTop = Color(0xCCFFFFFF),
    cardFillBottom = Color(0x99FFFFFF),
    cardBorder = Color(0x80FFFFFF),
    navBarFill = Color(0xDDFFFFFF),
    navBarBorder = Color(0x330F172A),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    accentBlue = ElectricBlue,
    accentViolet = SoftViolet,
    accentCyan = Color(0xFF0891B2),
    errorColor = Color(0xFFE11D48),
    successColor = Color(0xFF059669)
)

val LocalVortexGlass = staticCompositionLocalOf { DarkGlassPalette }

private val VortexTypography = Typography(
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun VortexTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val glass = if (useDark) DarkGlassPalette else LightGlassPalette
    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = ElectricBlue,
            secondary = SoftViolet,
            tertiary = SubtleCyan,
            background = MidnightBlack,
            surface = DarkCharcoal,
            onBackground = glass.textPrimary,
            onSurface = glass.textPrimary
        )
    } else {
        lightColorScheme(
            primary = ElectricBlue,
            secondary = SoftViolet,
            tertiary = glass.accentCyan,
            background = LightSurface,
            surface = FrostedWhite,
            onBackground = glass.textPrimary,
            onSurface = glass.textPrimary
        )
    }

    CompositionLocalProvider(LocalVortexGlass provides glass) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VortexTypography,
            content = content
        )
    }
}
