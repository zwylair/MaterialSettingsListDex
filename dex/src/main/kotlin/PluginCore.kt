package org.zwylair.msldex

import android.content.Context
import com.exteragram.messenger.utils.AppUtils
import de.robv.android.xposed.XC_MethodHook

object PluginCore {
    const val DEX_VERSION = "1.1.0"

    var appContext: Context? = null
    var unhookObjs: ArrayList<XC_MethodHook.Unhook> = arrayListOf()

    @JvmField
    val paletteManager = PaletteManager()
    @JvmField
    val settingsCellHooks = SettingsCellHookManager(paletteManager)
    @JvmField
    val listItemsManager = ListItemsManager()
    @JvmField
    val settingsActivityManager = SettingsActivityManager()

    @Suppress("unused")
    @JvmStatic
    fun init(context: Context) {
        appContext = context

        paletteManager.bakePalettes()
        settingsCellHooks.setupHooks()
        listItemsManager.setupHooks()

        log("DEX v$DEX_VERSION loaded.")
    }

    @Suppress("unused")
    @JvmStatic
    fun unload() {
        try { unhookObjs.forEach { it.unhook() } }
        catch (error: Throwable) { log(exception = error) }

        clearReflectionCache()
        settingsActivityManager.refreshSettingsActivities()

        log("Unloaded.")
        appContext = null
    }

    @JvmStatic
    fun log(vararg args: String, exception: Throwable? = null) =
        AppUtils.log("[MaterialSettingsListDEX] ${args.joinToString()}", exception)
}
