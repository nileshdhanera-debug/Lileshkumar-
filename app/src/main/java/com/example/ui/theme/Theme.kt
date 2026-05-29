package com.example.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SharedPreferences Helpers for Theme Persistence
fun loadPersistedTheme(context: Context) {
    val prefs = context.getSharedPreferences("app_settings_theme", Context.MODE_PRIVATE)
    val color = prefs.getString("theme_color_code", "clay") ?: "clay"
    val mode = prefs.getString("theme_mode_setting", "light") ?: "light"
    
    AppThemeManager.currentThemeCode = color
    AppThemeManager.isDarkTheme = when (mode) {
        "dark" -> true
        "light" -> false
        else -> {
            val uiMode = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}

fun persistThemeSettings(context: Context, colorCode: String, mode: String) {
    val prefs = context.getSharedPreferences("app_settings_theme", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putString("theme_color_code", colorCode)
        putString("theme_mode_setting", mode)
        putBoolean("theme_is_dark", AppThemeManager.isDarkTheme)
        apply()
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // Dynamically build Color Schemes inside composition to read current state values
    val colorScheme = if (AppThemeManager.isDarkTheme) {
        darkColorScheme(
            primary = GeoPrimaryAction,
            secondary = GeoBadgeBg,
            tertiary = GeoAccentLight,
            background = GeoBackground, 
            surface = GeoCardBg,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = GeoTextPrimary,
            onSurface = GeoTextPrimary
        )
    } else {
        lightColorScheme(
            primary = GeoPrimaryAction,
            secondary = GeoBadgeBg,
            tertiary = GeoAccentLight,
            background = GeoBackground,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color(0xFF221B11),
            onBackground = GeoTextPrimary,
            onSurface = GeoTextDark
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
