package org.zwylair.msldex

import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import org.telegram.messenger.R as Res
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.UItem

class SettingsCellHookManager(private val paletteManager: PaletteManager) {
    private val allowedBackgrounds: MutableSet<Any> = HashSet()

    fun setupHooks() {
        try {
            val factoryClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell\$Factory")!!
            factoryClazz.findMethodMaxArgs("of")!!.before { onFactoryHook(it) }
            factoryClazz.findMethodMaxArgs("ofBot")!!.before { onFactoryHook(it) }
            factoryClazz.findMethodMaxArgs("bindView")!!.after { onBindViewHook(it) }

            val cellClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell")!!
            cellClazz.findMethodMaxArgs("updateColors")!!.after { onUpdateColorsHook(it) }

            val backgroundClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell\$Background")!!
            backgroundClazz.findMethodMaxArgs("setColor")!!.before { onSetBackgroundHook(it) }
        } catch (e: Throwable) {
            PluginCore.log("SettingsCellHookManager.setupHooks failed:", exception = e)
        }
    }

    private fun onFactoryHook(param: MethodHookParam) {
        val arg0 = param.args[0]
        val palette = paletteManager.getPalette()

        val bgColor: Int = when (arg0) {
            is Int -> {
                if (arg0 == -1) param.args[3] = Res.drawable.ic_foreground_solid
                palette.forButtonId(arg0).bgColor
            }
            is TLRPC.TL_attachMenuBot -> palette.bot.bgColor
            else -> return
        }

        param.args[1] = bgColor
        param.args[2] = bgColor
    }

    private fun onBindViewHook(param: MethodHookParam) {
        val cell = param.args[0] ?: return
        val item = param.args[1] as? UItem ?: return

        val palette = paletteManager.getPalette()
        val iconColor = if (item.`object` is TLRPC.TL_attachMenuBot) {
            palette.bot.iconColor
        } else {
            palette.forButtonId(item.id).iconColor
        }

        val iconView = cell.safeInvoke("getIconView") as? View ?: return
        iconView.tag = iconColor
    }

    private fun onUpdateColorsHook(param: MethodHookParam) {
        val cell = param.thisObject
        cell.getFieldValue("iconBackground")?.let { allowedBackgrounds.add(it) }

        val iconView = cell.safeInvoke("getIconView") as? ImageView ?: return
        val tag = iconView.tag
        if (tag is Int)
            iconView.setColorFilter(tag, PorterDuff.Mode.SRC_IN)
    }

    private fun onSetBackgroundHook(param: MethodHookParam) {
        if (allowedBackgrounds.contains(param.thisObject) && param.args[2] == true)
            param.args[2] = false
    }
}
