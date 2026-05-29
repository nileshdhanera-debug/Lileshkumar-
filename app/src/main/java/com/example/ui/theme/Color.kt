package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// ==============================================================
// APP STATEFUL THEME MANAGER
// ==============================================================
object AppThemeManager {
    // Persistent theme settings, to be restored on start up
    var currentThemeCode by mutableStateOf("clay") // "clay", "blue", "teal", "green", "amber"
    var isDarkTheme by mutableStateOf(false)

    val primaryColor: Color
        get() = when (currentThemeCode) {
            "blue" -> Color(0xFF1565C0)
            "teal" -> Color(0xFF00796B)
            "green" -> Color(0xFF2E7D32)
            "amber" -> Color(0xFFE65100)
            else -> Color(0xFF6F5B40) // clay
        }

    val primaryActiveColor: Color
        get() = when (currentThemeCode) {
            "blue" -> Color(0xFF0D47A1)
            "teal" -> Color(0xFF004D40)
            "green" -> Color(0xFF1B5E20)
            "amber" -> Color(0xFFBF360C)
            else -> Color(0xFF5A4933) // clay active
        }

    val backgroundColor: Color
        get() = if (isDarkTheme) {
            Color(0xFF121212)
        } else {
            when (currentThemeCode) {
                "blue" -> Color(0xFFF1F5F9)
                "teal" -> Color(0xFFF2F8F8)
                "green" -> Color(0xFFF1F6F2)
                "amber" -> Color(0xFFFFFBEB)
                else -> Color(0xFFFDF8F3) // clay background
            }
        }

    val textPrimaryColor: Color
        get() = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF1F1B16)

    val textMutedColor: Color
        get() = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF85735D)

    val textDarkColor: Color
        get() = if (isDarkTheme) Color(0xFFFFFFFF) else Color(0xFF221B11)

    val accentLightColor: Color
        get() = if (isDarkTheme) {
            when (currentThemeCode) {
                "blue" -> Color(0xFF1E293B)
                "teal" -> Color(0xFF0F2C2C)
                "green" -> Color(0xFF0F2615)
                "amber" -> Color(0xFF2C1C0F)
                else -> Color(0xFF2B2217)
            }
        } else {
            when (currentThemeCode) {
                "blue" -> Color(0xFFE3F2FD)
                "teal" -> Color(0xFFE0F2F1)
                "green" -> Color(0xFFE8F5E9)
                "amber" -> Color(0xFFFFF3E0)
                else -> Color(0xFFEADDFF)
            }
        }

    val accentDarkColor: Color
        get() = if (isDarkTheme) {
            when (currentThemeCode) {
                "blue" -> Color(0xFF90CAF9)
                "teal" -> Color(0xFF80CBC4)
                "green" -> Color(0xFFA5D6A7)
                "amber" -> Color(0xFFFFCC80)
                else -> Color(0xFFDCD0FF)
            }
        } else {
            when (currentThemeCode) {
                "blue" -> Color(0xFF0D47A1)
                "teal" -> Color(0xFF004D40)
                "green" -> Color(0xFF1B5E20)
                "amber" -> Color(0xFFE65100)
                else -> Color(0xFF21005D)
            }
        }

    val badgeBgColor: Color
        get() = if (isDarkTheme) Color(0xFF334155) else Color(0xFFD7C4B1)

    val cardBgColor: Color
        get() = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFCFAF7)

    val borderColor: Color
        get() = if (isDarkTheme) Color(0xFF334155) else Color(0xFFEBE0D4)
}

// ==============================================================
// TOP LEVEL DYNAMIC PROPERTIES (ZERO-CHANGE CODE BRIDGE)
// ==============================================================
val GeoBackground: Color get() = AppThemeManager.backgroundColor
val GeoTextPrimary: Color get() = AppThemeManager.textPrimaryColor
val GeoTextMuted: Color get() = AppThemeManager.textMutedColor
val GeoTextDark: Color get() = AppThemeManager.textDarkColor
val GeoPrimaryAction: Color get() = AppThemeManager.primaryColor
val GeoPrimaryActive: Color get() = AppThemeManager.primaryActiveColor
val GeoAccentLight: Color get() = AppThemeManager.accentLightColor
val GeoAccentDark: Color get() = AppThemeManager.accentDarkColor
val GeoBadgeBg: Color get() = AppThemeManager.badgeBgColor
val GeoBadgeText: Color get() = if (AppThemeManager.isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF4D4439)
val GeoCardBg: Color get() = AppThemeManager.cardBgColor
val GeoBorder: Color get() = AppThemeManager.borderColor
val GeoNavBorder: Color get() = if (AppThemeManager.isDarkTheme) Color(0xFF1E293B) else Color(0xFFF0EBE5)

val DangerRed = Color(0xFFEF4444)
val SuccessGreen = Color(0xFF10B981)
