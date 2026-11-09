package org.zwylair.msldex

import android.graphics.PorterDuff
import android.view.View
import java.util.WeakHashMap
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import org.telegram.messenger.R as Res
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.UItem
import org.telegram.ui.SettingsActivity.SettingCell

class SettingsCellHookManager(private val paletteManager: PaletteManager) {
    private val paletteKeys = WeakHashMap<SettingCell, Int>()

    fun setupHooks() {
        try {
            val factoryClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell\$Factory")!!
            val bindView = factoryClazz.findMethodMaxArgs("bindView")!!
            bindView.before { onBindViewBefore(it) }
            bindView.after { onBindViewAfter(it) }

            val cellClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell")!!
            cellClazz.findMethodMaxArgs("updateColors")!!.after { applyColors(it.thisObject as SettingCell) }
        } catch (e: Throwable) {
            PluginCore.log("SettingsCellHookManager.setupHooks failed:", exception = e)
        }
    }

    private fun onBindViewBefore(param: MethodHookParam) {
        val cell = param.args[0] as? SettingCell ?: return
        val item = param.args[1] as? UItem ?: return

        when {
            item.longValue == 0L -> paletteKeys.remove(cell)
            item.`object` is TLRPC.TL_attachMenuBot -> paletteKeys[cell] = BOT_KEY
            else -> paletteKeys[cell] = item.id
        }
    }

    private fun onBindViewAfter(param: MethodHookParam) {
        val cell = param.args[0] as? SettingCell ?: return
        val item = param.args[1] as? UItem ?: return

        if (item.id == -1 && paletteKeys.containsKey(cell))
            cell.iconView.setImageResource(Res.drawable.ic_foreground_solid)
    }

    private fun applyColors(cell: SettingCell) {
        val key = paletteKeys[cell] ?: return
        val palette = paletteManager.getPalette()
        val colors = if (key == BOT_KEY) palette.bot else palette.forButtonId(key)

        ((cell.iconView.parent as? View)?.background as? SettingCell.Background)
            ?.setColor(colors.bgColor, colors.bgColor, false)
        cell.iconView.setColorFilter(colors.iconColor, PorterDuff.Mode.SRC_IN)
    }

    companion object {
        private const val BOT_KEY = Int.MIN_VALUE
    }
}
