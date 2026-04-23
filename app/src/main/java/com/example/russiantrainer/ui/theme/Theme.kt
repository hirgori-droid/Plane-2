package com.example.russiantrainer.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BlossomColors = lightColorScheme(
    primary = Color(0xFFD9A6F7),
    onPrimary = Color(0xFF34203F),
    primaryContainer = Color(0xFFF1E7FF),
    onPrimaryContainer = Color(0xFF2F2340),
    secondary = Color(0xFFE7DEFF),
    onSecondary = Color(0xFF2F2940),
    secondaryContainer = Color(0xFFF4EEFF),
    onSecondaryContainer = Color(0xFF2F2940),
    tertiary = Color(0xFFC9E1FF),
    onTertiary = Color(0xFF20324A),
    background = Color(0xFFF7F4FF),
    onBackground = Color(0xFF24212C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF24212C),
    surfaceVariant = Color(0xFFF1ECF8),
    onSurfaceVariant = Color(0xFF5F5870),
    outline = Color(0xFFDAD2E6),
    outlineVariant = Color(0xFFEAE3F3),
    error = Color(0xFFCC5B78),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE4EA),
    onErrorContainer = Color(0xFF5E2030)
)

private val GroveColors = lightColorScheme(
    primary = Color(0xFF82B88F),
    onPrimary = Color(0xFF14321F),
    primaryContainer = Color(0xFFE4F5E8),
    onPrimaryContainer = Color(0xFF203928),
    secondary = Color(0xFFD8E8C6),
    onSecondary = Color(0xFF26331F),
    secondaryContainer = Color(0xFFEAF3DE),
    onSecondaryContainer = Color(0xFF2E3A26),
    tertiary = Color(0xFFFFD8A8),
    onTertiary = Color(0xFF463018),
    background = Color(0xFFF6F7F1),
    onBackground = Color(0xFF20251F),
    surface = Color(0xFFFFFFFC),
    onSurface = Color(0xFF20251F),
    surfaceVariant = Color(0xFFEDF0E7),
    onSurfaceVariant = Color(0xFF5E655C),
    outline = Color(0xFFD3D9CD),
    outlineVariant = Color(0xFFE5EADF),
    error = Color(0xFFB94D5D),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE2E5),
    onErrorContainer = Color(0xFF541922)
)

private val MidnightColors = darkColorScheme(
    primary = Color(0xFF2B6C90),
    onPrimary = Color(0xFFF4FBFF),
    primaryContainer = Color(0xFF163E54),
    onPrimaryContainer = Color(0xFFE4F5FF),
    secondary = Color(0xFF8A611E),
    onSecondary = Color(0xFFFFF9F0),
    secondaryContainer = Color(0xFF624512),
    onSecondaryContainer = Color(0xFFFFEBC7),
    tertiary = Color(0xFF2E6E58),
    onTertiary = Color(0xFFF2FFF9),
    tertiaryContainer = Color(0xFF1F5341),
    onTertiaryContainer = Color(0xFFD5F9EA),
    background = Color(0xFF0C1320),
    onBackground = Color(0xFFE6EDF7),
    surface = Color(0xFF101A2B),
    onSurface = Color(0xFFE6EDF7),
    surfaceVariant = Color(0xFF1A2638),
    onSurfaceVariant = Color(0xFFB9C5D7),
    outline = Color(0xFF66758D),
    outlineVariant = Color(0xFF243246),
    error = Color(0xFFFFB4B9),
    onError = Color(0xFF680015),
    errorContainer = Color(0xFF8C1129),
    onErrorContainer = Color(0xFFFFDADD)
)

@Composable
fun RussianTrainerTheme(
    appTheme: AppTheme = AppTheme.BLOSSOM,
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        AppTheme.BLOSSOM -> BlossomColors
        AppTheme.GROVE -> GroveColors
        AppTheme.MIDNIGHT -> MidnightColors
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val useDarkSystemIcons = appTheme != AppTheme.MIDNIGHT
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = useDarkSystemIcons
                isAppearanceLightNavigationBars = useDarkSystemIcons
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
