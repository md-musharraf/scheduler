package com.example.routineforge.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Brand palette
val Navy = Color(0xFF1E2A5A)
val NavyDeep = Color(0xFF141C3F)
val Indigo = Color(0xFF5B5FEF)
val Lavender = Color(0xFFE9EAFF)
val Mint = Color(0xFF22B59A)
val WarmOrange = Color(0xFFF2994A)
val Coral = Color(0xFFE5484D)

/** Colors Material doesn't name: the navy hero card and status colors. */
@Immutable
data class FocusColors(
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val track: Color,
    val cardBorder: Color
)

private val LightFocus = FocusColors(
    hero = Navy, onHero = Color.White, onHeroMuted = Color(0xFFB9C1E4),
    success = Mint, warning = WarmOrange, danger = Coral,
    track = Color(0xFFE6E8F3), cardBorder = Color(0xFFE8EAF3)
)
private val DarkFocus = FocusColors(
    hero = Color(0xFF232C5C), onHero = Color.White, onHeroMuted = Color(0xFFB0B8DC),
    success = Color(0xFF3CCFB0), warning = Color(0xFFF5A962), danger = Color(0xFFFF6B6F),
    track = Color(0xFF2B3150), cardBorder = Color(0xFF262B45)
)

private val LightScheme = lightColorScheme(
    primary = Indigo, onPrimary = Color.White,
    primaryContainer = Lavender, onPrimaryContainer = Color(0xFF23286B),
    secondary = Navy, onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE3F5), onSecondaryContainer = Navy,
    tertiary = Mint, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD5F5EE), onTertiaryContainer = Color(0xFF0B4F43),
    error = Coral, onError = Color.White,
    background = Color(0xFFF6F7FC), onBackground = Color(0xFF151A30),
    surface = Color.White, onSurface = Color(0xFF151A30),
    surfaceVariant = Color(0xFFEEF0F8), onSurfaceVariant = Color(0xFF5D6480),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF9FAFE),
    surfaceContainer = Color(0xFFF1F2FA),
    surfaceContainerHigh = Color(0xFFEBEDF7),
    surfaceContainerHighest = Color(0xFFE4E7F3),
    outline = Color(0xFFC9CDDF), outlineVariant = Color(0xFFE3E5F0)
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF9396FF), onPrimary = Color(0xFF16195A),
    primaryContainer = Color(0xFF2E3172), onPrimaryContainer = Color(0xFFE0E1FF),
    secondary = Color(0xFFC3CCF2), onSecondary = NavyDeep,
    secondaryContainer = Color(0xFF2A3259), onSecondaryContainer = Color(0xFFDDE3F5),
    tertiary = Color(0xFF3CCFB0), onTertiary = Color(0xFF00382E),
    tertiaryContainer = Color(0xFF0F4A3F), onTertiaryContainer = Color(0xFFBDF3E6),
    error = Color(0xFFFF6B6F), onError = Color(0xFF3B0A0C),
    background = Color(0xFF0E1120), onBackground = Color(0xFFE6E8F4),
    surface = Color(0xFF161A2D), onSurface = Color(0xFFE6E8F4),
    surfaceVariant = Color(0xFF20253B), onSurfaceVariant = Color(0xFFA6ACC6),
    surfaceContainerLowest = Color(0xFF0B0E1A),
    surfaceContainerLow = Color(0xFF141829),
    surfaceContainer = Color(0xFF191E32),
    surfaceContainerHigh = Color(0xFF20253B),
    surfaceContainerHighest = Color(0xFF282E47),
    outline = Color(0xFF454B69), outlineVariant = Color(0xFF2C3250)
)

private val AppTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        titleSmall = titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        bodyLarge = bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    )
}

/** Big, fixed-width digits so the timer doesn't jitter as numbers change. */
val TimerDigits = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum", letterSpacing = (-1).sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LocalFocusColors = staticCompositionLocalOf { LightFocus }

object FF {
    val colors: FocusColors
        @Composable @ReadOnlyComposable get() = LocalFocusColors.current
}

@Composable
fun FocusFlowTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalFocusColors provides if (dark) DarkFocus else LightFocus) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}

/** Colors users can pick for subjects. */
val SubjectPalette = listOf(
    0xFF5B5FEF, 0xFF8B5CF6, 0xFF4C8DF6, 0xFF22B59A, 0xFFF2994A, 0xFFE5484D, 0xFFE0529C, 0xFF14A3B8, 0xFFE2B93B, 0xFF64748B
)
