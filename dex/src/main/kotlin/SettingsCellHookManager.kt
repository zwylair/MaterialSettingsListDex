package org.zwylair.msldex

import android.graphics.PorterDuff
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import org.telegram.messenger.R as Res
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.UItem
import org.telegram.ui.SettingsActivity.SettingCell

class SettingsCellHookManager(private val paletteManager: PaletteManager) {
    fun setupHooks() {
        try {
            val factoryClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell\$Factory")!!
            factoryClazz.findMethodMaxArgs("bindView")!!.before { onBindViewHook(it) }

            val cellClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell")!!
            cellClazz.findMethodMaxArgs("updateColors")!!.after { applyColors(it.thisObject as SettingCell) }
        } catch (e: Throwable) {
            PluginCore.log("SettingsCellHookManager.setupHooks failed:", exception = e)
        }
    }

    private fun onBindViewHook(param: MethodHookParam) {
        val cell = param.args[0] as? SettingCell ?: return
        val item = param.args[1] as? UItem ?: return

        cell.iconView.tag = when {
            item.longValue == 0L -> null
            item.`object` is TLRPC.TL_attachMenuBot -> BOT_KEY
            else -> item.id
        }
        if (item.id == -1) item.iconResId = Res.drawable.ic_foreground_solid
    }

    private fun applyColors(cell: SettingCell) {
        val key = cell.iconView.tag as? Int ?: return
        val palette = paletteManager.getPalette()
        val colors = if (key == BOT_KEY) palette.bot else palette.forButtonId(key)

        (cell.getFieldValue("iconBackground") as? SettingCell.Background)
            ?.setColor(colors.bgColor, colors.bgColor, false)
        cell.iconView.setColorFilter(colors.iconColor, PorterDuff.Mode.SRC_IN)
    }

    companion object {
        private const val BOT_KEY = Int.MIN_VALUE
    }
}
