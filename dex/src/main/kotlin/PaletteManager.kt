package org.zwylair.msldex

import android.graphics.Color
import androidx.core.graphics.ColorUtils
import org.telegram.ui.ActionBar.Theme

data class PaletteColors(val bgColor: Int, val iconColor: Int)

class PaletteSet {
    var default: PaletteColors = PaletteColors(0, 0)
    var bot: PaletteColors = PaletteColors(0, 0)
    val categories: HashMap<Int, PaletteColors> = HashMap()

    fun forButtonId(id: Int): PaletteColors = categories[id] ?: default
    fun clear() = categories.clear()
}

class PaletteManager {
    private val darkPalette = PaletteSet()
    private val lightPalette = PaletteSet()
    private val monetDarkPalette = PaletteSet()
    private val monetLightPalette = PaletteSet()

    fun getPalette(): PaletteSet = when {
        Theme.isCurrentThemeMonet() -> if (Theme.isCurrentThemeDark()) monetDarkPalette else monetLightPalette
        else -> if (Theme.isCurrentThemeDark()) darkPalette else lightPalette
    }

    fun bakePalettes() {
        try {
            darkPalette.clear()
            darkPalette.default = generateM3Pair(DEFAULT_BASE_COLOR, isDark = true, isMonet = false)
            darkPalette.bot = generateM3Pair(BOT_BASE_COLOR, isDark = true, isMonet = false)

            lightPalette.clear()
            lightPalette.default = generateM3Pair(DEFAULT_BASE_COLOR, isDark = false, isMonet = false)
            lightPalette.bot = generateM3Pair(BOT_BASE_COLOR, isDark = false, isMonet = false)

            monetDarkPalette.clear()
            monetDarkPalette.default = generateM3Pair(DEFAULT_BASE_COLOR, isDark = true, isMonet = true)
            monetDarkPalette.bot = generateM3Pair(BOT_BASE_COLOR, isDark = true, isMonet = true)

            monetLightPalette.clear()
            monetLightPalette.default = generateM3Pair(DEFAULT_BASE_COLOR, isDark = false, isMonet = true)
            monetLightPalette.bot = generateM3Pair(BOT_BASE_COLOR, isDark = false, isMonet = true)

            val categoryColors =
                if (SettingsState.reorderSettingsList) BASE_CATEGORY_COLORS_REGROUP else BASE_CATEGORY_COLORS

            for ((buttonId, baseColor) in categoryColors) {
                darkPalette.categories[buttonId] = generateM3Pair(baseColor, isDark = true, isMonet = false)
                lightPalette.categories[buttonId] = generateM3Pair(baseColor, isDark = false, isMonet = false)
                monetDarkPalette.categories[buttonId] = generateM3Pair(baseColor, isDark = true, isMonet = true)
                monetLightPalette.categories[buttonId] = generateM3Pair(baseColor, isDark = false, isMonet = true)
            }

            PluginCore.log("Palettes baked.")
        } catch (e: Throwable) {
            PluginCore.log("PaletteManager.bakePalettes failed:", exception = e)
        }
    }

    private fun generateM3Pair(baseColor: Int, isDark: Boolean, isMonet: Boolean): PaletteColors {
        val hsl = FloatArray(3)
        org.telegram.ui.SettingsActivity()
        ColorUtils.colorToHSL(baseColor, hsl)

        val hue = hsl[0]
        val sat = hsl[1].coerceIn(0.55f, 1.0f)

        val (bgColor, iconColor) = if (isDark) {
            if (isMonet)
                ColorUtils.HSLToColor(floatArrayOf(hue, sat * 0.65f, 0.675f)) to
                    ColorUtils.HSLToColor(floatArrayOf(hue, sat * 0.90f, 0.18f))
            else
                ColorUtils.HSLToColor(floatArrayOf(hue, sat * 0.60f, 0.80f)) to
                    ColorUtils.HSLToColor(floatArrayOf(hue, sat * 0.90f, 0.25f))
        } else {
            if (isMonet)
                ColorUtils.HSLToColor(floatArrayOf(hue, sat * 0.50f, 0.88f)) to
                    ColorUtils.HSLToColor(floatArrayOf(hue, sat * 0.95f, 0.35f))
            else
                ColorUtils.HSLToColor(floatArrayOf(hue, sat, 0.82f)) to
                    ColorUtils.HSLToColor(floatArrayOf(hue, sat, 0.32f))
        }

        return PaletteColors(bgColor, iconColor)
    }

    companion object {
        val COLOR_EXTERA = Color.parseColor("#EA6161")
        val COLOR_AYU = Color.parseColor("#B161EA")
        val COLOR_BLUE = Color.parseColor("#1796E7")
        val COLOR_GREEN = Color.parseColor("#3EC03D")
        val COLOR_ORANGE = Color.parseColor("#EA7722")
        val COLOR_GOLD = Color.parseColor("#EFA612")
        val COLOR_CYAN = Color.parseColor("#1BA4ED")

        val DEFAULT_BASE_COLOR = Color.parseColor("#8699AA")
        val BOT_BASE_COLOR = COLOR_GOLD

        val BASE_CATEGORY_COLORS_REGROUP: Map<Int, Int> = mapOf(
            -1 to COLOR_EXTERA,
            -2 to COLOR_AYU,

            1 to COLOR_BLUE,   // Account
            3 to COLOR_BLUE,   // PrivacySecurity
            8 to COLOR_BLUE,   // Devices

            2 to COLOR_GREEN,  // Chat
            7 to COLOR_GREEN,  // Folders
            10 to COLOR_GREEN, // Language

            5 to COLOR_ORANGE, // Notifications
            6 to COLOR_ORANGE, // Data
            9 to COLOR_ORANGE, // PowerSaving

            11 to COLOR_GOLD,  // Premium
            12 to COLOR_GOLD,  // Stars
            13 to COLOR_GOLD,  // MyTON
            15 to COLOR_GOLD,  // Business
            16 to COLOR_GOLD,  // SendAGift

            17 to COLOR_CYAN,  // AskAQuestion
            18 to COLOR_CYAN,  // FAQ
            23 to COLOR_CYAN,  // Features
            19 to COLOR_CYAN,  // PrivacyPolicy

            // Debug Section
            20 to COLOR_GREEN,
            21 to COLOR_GREEN,
            22 to Color.parseColor("#EA4555"),
        )

        val BASE_CATEGORY_COLORS: Map<Int, Int> = mapOf(
            -1 to Color.parseColor("#E83030"),  // Preferences (exteraGram)
            -2 to Color.parseColor("#9570FF"),  // Preferences (AyuGram)
            1 to Color.parseColor("#1796E7"),   // SettingsAccount
            2 to Color.parseColor("#E89416"),   // SettingsChat
            3 to Color.parseColor("#3EC03D"),   // SettingsPrivacySecurity
            5 to Color.parseColor("#EA4555"),   // SettingsNotifications
            6 to Color.parseColor("#4276EE"),   // SettingsData
            7 to Color.parseColor("#1795E7"),   // SettingsFolders
            8 to Color.parseColor("#27ADCA"),   // SettingsDevices
            9 to Color.parseColor("#EA7722"),   // SettingsPowerSaving
            10 to Color.parseColor("#B161EA"),  // SettingsLanguage
            11 to Color.parseColor("#B659FF"),  // Telegram Premium
            12 to Color.parseColor("#EFA612"),  // Telegram Stars
            13 to Color.parseColor("#1BA4ED"),  // MyTON
            15 to Color.parseColor("#F45255"),  // Telegram Business
            16 to Color.parseColor("#F38B31"),  // Send a Gift
            17 to Color.parseColor("#E89416"),  // AskAQuestion
            18 to Color.parseColor("#1BA4ED"),  // TelegramFAQ
            23 to Color.parseColor("#B161EA"),  // TelegramFeatures
            19 to Color.parseColor("#3EC03D"),  // PrivacyPolicy
            20 to Color.parseColor("#3EC03D"),  // DebugSendLogs
            21 to Color.parseColor("#3EC03D"),  // DebugSendLastLogs
            22 to Color.parseColor("#EA4555"),  // DebugClearLogs
        )
    }
}
