package com.novelreader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlueDark,
    secondary = SecondaryTealDark,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryBlue,
    secondary = SecondaryTeal,
    tertiary = Pink40
)

@Composable
fun NovelReaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useGlass: Boolean = true,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val glassColorScheme = if (useGlass) {
        colorScheme.copy(
            surface = if (darkTheme) GlassSurfaceDark else GlassSurfaceLight,
            surfaceVariant = if (darkTheme) GlassSurfaceDark else GlassSurfaceLight,
            onSurface = if (darkTheme) GlassContentDark else GlassContentLight,
            onSurfaceVariant = if (darkTheme) GlassContentDark else GlassContentLight
        )
    } else {
        colorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = glassColorScheme,
        typography = NovelTypography,
        content = content
    )
}
