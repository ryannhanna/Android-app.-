package com.smartfilemanager.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme { SYSTEM, LIGHT, DARK }

private val LightColorScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = White,
    background = White,
    onBackground = OnSurfaceLight,
    surface = LightSurface,
    onSurface = OnSurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue200,
    onPrimary = DarkBackground,
    background = DarkBackground,
    onBackground = OnSurfaceDark,
    surface = DarkSurface,
    onSurface = OnSurfaceDark
)

@Composable
fun SmartFileManagerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDark) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        useDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as Activity
        SideEffect {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowCompat.getInsetsController(activity.window, view)
                .isAppearanceLightStatusBars = !useDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
