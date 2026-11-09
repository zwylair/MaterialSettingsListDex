package org.zwylair.msldex

import android.os.Handler
import android.os.Looper

@Suppress("unused")
object SettingsSyncManager {
    private val debounceHandler = Handler(Looper.getMainLooper())
    private var pendingDebloatRefresh: Runnable? = null

    @Suppress("unused")
    @JvmStatic
    fun syncInitialState(
        reorderSettingsList: Boolean,
        debloatTgPrem: Boolean,
        debloatTgStars: Boolean,
        debloatMyTon: Boolean,
        debloatWallet: Boolean,
        debloatTgBusiness: Boolean,
        debloatSendGift: Boolean,
        debloatHelpSection: Boolean,
    ) {
        SettingsState.reorderSettingsList = reorderSettingsList
        SettingsState.debloatTgPrem = debloatTgPrem
        SettingsState.debloatTgStars = debloatTgStars
        SettingsState.debloatMyTon = debloatMyTon
        SettingsState.debloatWallet = debloatWallet
        SettingsState.debloatTgBusiness = debloatTgBusiness
        SettingsState.debloatSendGift = debloatSendGift
        SettingsState.debloatHelpSection = debloatHelpSection

        PluginCore.paletteManager.bakePalettes()
        PluginCore.settingsActivityManager.refreshSettingsActivities()
    }

    @Suppress("unused")
    @JvmStatic
    fun setReorderSettingsList(value: Boolean) {
        SettingsState.reorderSettingsList = value
        PluginCore.paletteManager.bakePalettes()
        PluginCore.settingsActivityManager.refreshSettingsActivities()
    }

    @Suppress("unused")
    @JvmStatic
    fun setDebloatTgPrem(value: Boolean) = setDebloatFlag { SettingsState.debloatTgPrem = value }

    @Suppress("unused")
    @JvmStatic
    fun setDebloatTgStars(value: Boolean) = setDebloatFlag { SettingsState.debloatTgStars = value }

    @Suppress("unused")
    @JvmStatic
    fun setDebloatMyTon(value: Boolean) = setDebloatFlag { SettingsState.debloatMyTon = value }

    @Suppress("unused")
    @JvmStatic
    fun setDebloatWallet(value: Boolean) = setDebloatFlag { SettingsState.debloatWallet = value }

    @Suppress("unused")
    @JvmStatic
    fun setDebloatTgBusiness(value: Boolean) = setDebloatFlag { SettingsState.debloatTgBusiness = value }

    @Suppress("unused")
    @JvmStatic
    fun setDebloatSendGift(value: Boolean) = setDebloatFlag { SettingsState.debloatSendGift = value }

    @Suppress("unused")
    @JvmStatic
    fun setDebloatHelpSection(value: Boolean) = setDebloatFlag { SettingsState.debloatHelpSection = value }

    private fun setDebloatFlag(applyValue: () -> Unit) {
        applyValue()

        pendingDebloatRefresh?.let { debounceHandler.removeCallbacks(it) }
        val runnable = Runnable { PluginCore.settingsActivityManager.refreshSettingsActivities() }
        pendingDebloatRefresh = runnable
        debounceHandler.postDelayed(runnable, 1_000L)
    }
}
