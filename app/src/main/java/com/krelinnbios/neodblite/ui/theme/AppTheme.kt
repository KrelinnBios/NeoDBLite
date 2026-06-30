package com.krelinnbios.neodblite.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 可切换的应用主题。每个主题给出一套完整 Material3 配色与是否深色（用于状态栏图标）。
 * 默认 [BLUE_BLACK] 是对齐 YamiboReaderLite 的经典蓝黑。
 */
enum class AppTheme(val label: String, val isDark: Boolean, val scheme: ColorScheme) {

    BLUE_BLACK(
        label = "蓝黑",
        isDark = true,
        scheme = darkColorScheme(
            primary = Color(0xFF4EA1FF),
            onPrimary = Color(0xFF04243F),
            primaryContainer = Color(0xFF1C3A57),
            onPrimaryContainer = Color(0xFFCFE5FF),
            secondary = Color(0xFF8FC0F2),
            onSecondary = Color(0xFF06243C),
            secondaryContainer = Color(0xFF223247),
            onSecondaryContainer = Color(0xFFCFE5FF),
            background = Color(0xFF0D141D),
            onBackground = Color(0xFFE6EDF5),
            surface = Color(0xFF182332),
            onSurface = Color(0xFFE6EDF5),
            surfaceVariant = Color(0xFF223247),
            onSurfaceVariant = Color(0xFFA9BBD0),
            outline = Color(0xFF3A4A5E),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005),
            surfaceDim = Color(0xFF0D141D),
            surfaceBright = Color(0xFF2A3B50),
            surfaceContainerLowest = Color(0xFF080D14),
            surfaceContainerLow = Color(0xFF141D29),
            surfaceContainer = Color(0xFF1A2735),
            surfaceContainerHigh = Color(0xFF223247),
            surfaceContainerHighest = Color(0xFF293B50)
        )
    ),

    TEAL_LIGHT(
        label = "海青·浅",
        isDark = false,
        scheme = lightColorScheme(
            primary = md_primary,
            onPrimary = md_onPrimary,
            primaryContainer = md_primaryContainer,
            onPrimaryContainer = md_onPrimaryContainer,
            secondary = md_secondary,
            onSecondary = md_onSecondary,
            secondaryContainer = md_secondaryContainer,
            onSecondaryContainer = md_onSecondaryContainer,
            background = md_background,
            onBackground = md_onBackground,
            surface = md_surface,
            onSurface = md_onSurface,
            surfaceVariant = md_surfaceVariant,
            onSurfaceVariant = md_onSurfaceVariant,
            outline = md_outline,
            error = md_error,
            surfaceDim = Color(0xFFDBE1DD),
            surfaceBright = Color(0xFFFBFDFA),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF4F8F5),
            surfaceContainer = Color(0xFFEEF3EF),
            surfaceContainerHigh = Color(0xFFE8EEEA),
            surfaceContainerHighest = Color(0xFFE2E8E4)
        )
    ),

    TEAL_DARK(
        label = "墨绿·深",
        isDark = true,
        scheme = darkColorScheme(
            primary = md_dark_primary,
            onPrimary = md_dark_onPrimary,
            primaryContainer = md_dark_primaryContainer,
            onPrimaryContainer = md_dark_onPrimaryContainer,
            secondary = md_dark_secondary,
            onSecondary = md_dark_onSecondary,
            secondaryContainer = md_dark_secondaryContainer,
            onSecondaryContainer = md_dark_onSecondaryContainer,
            background = md_dark_background,
            onBackground = md_dark_onBackground,
            surface = md_dark_surface,
            onSurface = md_dark_onSurface,
            surfaceVariant = md_dark_surfaceVariant,
            onSurfaceVariant = md_dark_onSurfaceVariant,
            outline = md_dark_outline,
            error = md_dark_error,
            surfaceDim = Color(0xFF111413),
            surfaceBright = Color(0xFF363A38),
            surfaceContainerLowest = Color(0xFF0F1211),
            surfaceContainerLow = Color(0xFF1B201E),
            surfaceContainer = Color(0xFF1F2523),
            surfaceContainerHigh = Color(0xFF2A302E),
            surfaceContainerHighest = Color(0xFF353B39)
        )
    ),

    SAKURA(
        label = "樱粉·浅",
        isDark = false,
        scheme = lightColorScheme(
            primary = Color(0xFFB3325E),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFD9E1),
            onPrimaryContainer = Color(0xFF3E001D),
            secondary = Color(0xFF75565C),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFFFD9E1),
            onSecondaryContainer = Color(0xFF2B151A),
            background = Color(0xFFFFF8F8),
            onBackground = Color(0xFF201A1B),
            surface = Color(0xFFFFF8F8),
            onSurface = Color(0xFF201A1B),
            surfaceVariant = Color(0xFFF2DDE1),
            onSurfaceVariant = Color(0xFF514347),
            outline = Color(0xFF837377),
            error = md_error,
            surfaceDim = Color(0xFFE7D6D9),
            surfaceBright = Color(0xFFFFF8F8),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFFCEFF1),
            surfaceContainer = Color(0xFFFCE9EC),
            surfaceContainerHigh = Color(0xFFF8E3E6),
            surfaceContainerHighest = Color(0xFFF2DDE1)
        )
    ),

    MIDNIGHT_PURPLE(
        label = "暮紫·深",
        isDark = true,
        scheme = darkColorScheme(
            primary = Color(0xFFCFBCFF),
            onPrimary = Color(0xFF381E72),
            primaryContainer = Color(0xFF4F378B),
            onPrimaryContainer = Color(0xFFEADDFF),
            secondary = Color(0xFFCBC2DB),
            onSecondary = Color(0xFF332D41),
            secondaryContainer = Color(0xFF4A4458),
            onSecondaryContainer = Color(0xFFE8DEF8),
            background = Color(0xFF141218),
            onBackground = Color(0xFFE6E0E9),
            surface = Color(0xFF1D1B20),
            onSurface = Color(0xFFE6E0E9),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            outline = Color(0xFF938F99),
            error = Color(0xFFFFB4AB),
            surfaceDim = Color(0xFF141218),
            surfaceBright = Color(0xFF3B383F),
            surfaceContainerLowest = Color(0xFF0F0D13),
            surfaceContainerLow = Color(0xFF1D1B20),
            surfaceContainer = Color(0xFF211F26),
            surfaceContainerHigh = Color(0xFF2B2930),
            surfaceContainerHighest = Color(0xFF36343B)
        )
    );

    companion object {
        val DEFAULT = BLUE_BLACK
        fun fromName(name: String?): AppTheme =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

object AppThemePreference {
    private const val PREFS_NAME = "neodb_ui"
    private const val KEY_THEME = "app_theme"

    fun load(context: Context): AppTheme {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, null)
        return AppTheme.fromName(name)
    }

    fun save(context: Context, theme: AppTheme) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, theme.name)
            .apply()
    }
}
