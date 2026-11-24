package org.zwylair.msldex

import android.graphics.PorterDuff
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.util.WeakHashMap
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController
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
            factoryClazz.findMethodMaxArgs("of")!!.after { onFactoryOfAfter(it) }

            val cellClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell")!!
            cellClazz.findMethodMaxArgs("updateColors")!!.after { applyColors(it.thisObject as SettingCell) }
        } catch (e: Throwable) {
            PluginCore.log("SettingsCellHookManager.setupHooks failed:", exception = e)
        }
    }

    private fun onFactoryOfAfter(param: MethodHookParam) {
        if (!SettingsState.md3Style || param.args.size != 7) return
        val item = param.result as? UItem ?: return
        val subtitle = param.args[5] as? CharSequence
        val value = param.args[6] as? CharSequence

        when {
            !subtitle.isNullOrEmpty() -> item.subtext = subtitle
            !value.isNullOrEmpty() -> {
                item.subtext = value
                item.textValue = null
            }
            else -> item.subtext = SettingsState.forkSubtitles[item.id]
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

        val styled = paletteKeys.containsKey(cell)
        if (item.id == -1 && styled) cell.iconView.setImageResource(Res.drawable.ic_foreground_solid)
        cell.iconView.scaleX = if (isMd3Cell(cell)) FORK_ICON_SCALES[item.id] ?: 1f else 1f
        cell.iconView.scaleY = cell.iconView.scaleX

        applyLayout(cell)
    }

    private fun isMd3Cell(cell: SettingCell) = SettingsState.md3Style && paletteKeys.containsKey(cell)

    private fun applyLayout(cell: SettingCell) {
        val md3 = isMd3Cell(cell)
        val iconView = cell.iconView
        val iconLayout = iconView.parent as? View ?: return
        val textLayout = cell.getFieldValue("textLayout") as? View ?: return
        val subtitleView = cell.getFieldValue("subtitleView") as? TextView ?: return

        iconLayout.updateParams(if (md3) MD3_ICON_BG_SIZE else ICON_BG_SIZE, if (md3) MD3_ICON_MARGIN else ICON_MARGIN)
        textLayout.updateParams(null, if (md3 && iconLayout.visibility == View.VISIBLE) MD3_TEXT_MARGIN else TEXT_MARGIN)

        (subtitleView.layoutParams as? ViewGroup.MarginLayoutParams)?.let {
            val margin = dp((if (md3) MD3_SUBTITLE_MARGIN else SUBTITLE_MARGIN).toFloat())
            if (it.topMargin != margin) {
                it.topMargin = margin
                subtitleView.layoutParams = it
            }
        }
        subtitleView.isSingleLine = md3
        subtitleView.ellipsize = if (md3) TextUtils.TruncateAt.END else null
    }

    private fun View.updateParams(sizeDp: Int?, startMarginDp: Int?) {
        val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        var changed = false

        if (sizeDp != null && lp.width != dp(sizeDp.toFloat())) {
            lp.width = dp(sizeDp.toFloat())
            lp.height = lp.width
            changed = true
        }
        if (startMarginDp != null) {
            val margin = dp(startMarginDp.toFloat())
            if (LocaleController.isRTL && lp.rightMargin != margin) {
                lp.rightMargin = margin
                changed = true
            } else if (!LocaleController.isRTL && lp.leftMargin != margin) {
                lp.leftMargin = margin
                changed = true
            }
        }
        if (changed) layoutParams = lp
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
        private val FORK_ICON_SCALES = mapOf(-1 to 1.35f, -2 to 1.25f, -3 to 1.25f)

        private const val ICON_BG_SIZE = 28
        private const val ICON_MARGIN = 18
        private const val TEXT_MARGIN = 18
        private const val SUBTITLE_MARGIN = 4

        private const val MD3_ICON_BG_SIZE = 34
        private const val MD3_ICON_MARGIN = 15
        private const val MD3_TEXT_MARGIN = 19
        private const val MD3_SUBTITLE_MARGIN = 1
    }
}
