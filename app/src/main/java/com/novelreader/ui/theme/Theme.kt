package com.novelreader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val MIUILightColorScheme = lightColorScheme(
    primary = MIUI_Blue,
    onPrimary = Color.White,
    primaryContainer = MIUI_BlueLight,
    onPrimaryContainer = MIUI_Blue,
    secondary = MIUI_Green,
    onSecondary = Color.White,
    secondaryContainer = MIUI_GreenLight,
    onSecondaryContainer = Color(0xFF1B7A34),
    tertiary = MIUI_Orange,
    onTertiary = Color.White,
    tertiaryContainer = MIUI_OrangeLight,
    onTertiaryContainer = Color(0xFF995E00),
    error = MIUI_Red,
    onError = Color.White,
    errorContainer = MIUI_RedLight,
    onErrorContainer = Color(0xFF93000A),
    background = MIUI_Background,
    onBackground = MIUI_TextPrimary,
    surface = MIUI_Surface,
    onSurface = MIUI_TextPrimary,
    surfaceVariant = MIUI_SurfaceAlt,
    onSurfaceVariant = MIUI_TextSecondary,
    outline = MIUI_Divider,
    outlineVariant = MIUI_Divider,
    surfaceContainerHigh = MIUI_Card,
    surfaceContainer = MIUI_Card,
    surfaceContainerLow = MIUI_Surface
)

private val MIUIDarkColorScheme = darkColorScheme(
    primary = MIUI_BlueDark,
    onPrimary = Color(0xFF1B1C1E),
    primaryContainer = Color(0xFF2A3A66),
    onPrimaryContainer = MIUI_BlueDark,
    secondary = Color(0xFF64FFDA),
    onSecondary = Color(0xFF003829),
    secondaryContainer = Color(0xFF005138),
    onSecondaryContainer = Color(0xFF64FFDA),
    tertiary = Color(0xFFFFB340),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF663800),
    onTertiaryContainer = Color(0xFFFFB340),
    error = Color(0xFFFF6961),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFF6961),
    background = MIUI_BackgroundDark,
    onBackground = MIUI_TextPrimaryDark,
    surface = MIUI_SurfaceDark,
    onSurface = MIUI_TextPrimaryDark,
    surfaceVariant = MIUI_SurfaceAltDark,
    onSurfaceVariant = MIUI_TextSecondaryDark,
    outline = MIUI_DividerDark,
    outlineVariant = MIUI_DividerDark,
    surfaceContainerHigh = MIUI_CardDark,
    surfaceContainer = MIUI_CardDark,
    surfaceContainerLow = MIUI_SurfaceDark
)

val MIUIShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
)

@Composable
fun NovelReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useGlass: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> MIUIDarkColorScheme
        else -> MIUILightColorScheme
    }

    // useGlass 控制表面颜色的透明度，营造毛玻璃质感
    val adjustedScheme = if (useGlass) {
        colorScheme.copy(
            surface = colorScheme.surface.copy(alpha = 0.85f),
            surfaceContainerHigh = colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            surfaceContainer = colorScheme.surfaceContainer.copy(alpha = 0.9f),
        )
    } else {
        colorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = adjustedScheme,
        typography = NovelTypography,
        shapes = MIUIShapes,
        content = content
    )
}
