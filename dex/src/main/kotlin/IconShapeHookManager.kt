package org.zwylair.msldex

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.LocaleController
import org.telegram.ui.Cells.TextCell
import org.telegram.ui.SettingsActivity.SettingCell

class IconShapeHookManager(private val paletteManager: PaletteManager) {
    fun setupHooks() {
        try {
            val backgroundClazz = findClass("org.telegram.ui.SettingsActivity\$SettingCell\$Background")!!
            backgroundClazz.findMethod("draw", 1)!!.before { onBackgroundDraw(it) }

            val textCellClazz = findClass("org.telegram.ui.Cells.TextCell")!!
            textCellClazz.findMethodMaxArgs("setColorfulIcon")!!.after { onSetColorfulIconAfter(it) }
            textCellClazz.findMethodMaxArgs("onLayout")!!.after { onTextCellLayoutAfter(it) }

            findClass("org.telegram.ui.Cells.PollCreateCheckCell")
                ?.findMethodMaxArgs("setTextAndValueAndIconAndCheck")
                ?.after { onPollCheckCellBindAfter(it) }
            findClass("org.telegram.ui.DataUsage2Activity\$Cell")
                ?.findMethodMaxArgs("set")
                ?.after { onDataUsageCellSetAfter(it) }
            findClass("org.telegram.ui.community.cells.CommunityRequestsCell")
                ?.findMethodMaxArgs("set")
                ?.after { onCommunityRequestsCellSetAfter(it) }
            findClass("com.exteragram.messenger.pillstack.custom.PillIconBadgeView")
                ?.findMethod("updateColors", 0)
                ?.after { onPillBadgeUpdateColorsAfter(it) }
        } catch (e: Throwable) {
            PluginCore.log("IconShapeHookManager.setupHooks failed:", exception = e)
        }
    }

    private fun onBackgroundDraw(param: MethodHookParam) {
        if (!SettingsState.md3Style) return
        val background = param.thisObject as SettingCell.Background
        val canvas = param.args[0] as? Canvas ?: return
        val paint = background.getFieldValue("paint") as? Paint ?: return

        val bounds = background.bounds
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), minOf(bounds.width(), bounds.height()) / 2f, paint)
        param.result = null
    }

    private fun applyPalette(imageView: ImageView, baseColor: Int) {
        val background = imageView.background as? SettingCell.Background ?: return
        val colors = paletteManager.forBaseColor(baseColor)

        background.setColor(colors.bgColor, colors.bgColor, false)
        imageView.setColorFilter(colors.iconColor, PorterDuff.Mode.SRC_IN)
    }

    private fun View.growIcon(withTop: Boolean) {
        val lp = layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val size = dp(MD3_ICON_SIZE.toFloat())
        if (lp.width == size || lp.width <= 0) return
        val grow = (size - lp.width) / 2
        lp.width = size
        lp.height = size
        lp.leftMargin -= grow
        lp.rightMargin -= grow
        if (withTop) lp.topMargin -= grow
        layoutParams = lp
    }

    private fun isColorfulIcon(imageView: ImageView) = imageView.background is SettingCell.Background

    private fun onSetColorfulIconAfter(param: MethodHookParam) {
        if (!SettingsState.md3Style) return
        val imageView = (param.thisObject as TextCell).imageView
        val padding = dp(TEXT_CELL_ICON_PADDING.toFloat())
        imageView.setPadding(padding, padding, padding, padding)
        if (param.args[3] == true) applyPalette(imageView, param.args[0] as Int)
    }

    private fun onTextCellLayoutAfter(param: MethodHookParam) {
        val imageView = (param.thisObject as TextCell).imageView
        if (!SettingsState.md3Style || !isColorfulIcon(imageView)) return

        val shift = dp((TEXT_CELL_ICON_PADDING - TEXT_CELL_STOCK_PADDING).toFloat())
        if (imageView.paddingLeft != dp(TEXT_CELL_ICON_PADDING.toFloat())) return

        imageView.offsetLeftAndRight(if (LocaleController.isRTL) shift else -shift)
        imageView.offsetTopAndBottom(shift)
    }

    private fun onPollCheckCellBindAfter(param: MethodHookParam) {
        if (!SettingsState.md3Style) return
        val imageView = param.thisObject.getFieldValue("imageView") as? ImageView ?: return
        val baseColor = param.args[2]?.getFieldValue("top") as? Int ?: return
        applyPalette(imageView, baseColor)
        imageView.growIcon(withTop = true)
    }

    private fun onDataUsageCellSetAfter(param: MethodHookParam) {
        if (!SettingsState.md3Style || param.args[2] == 0) return
        val imageView = param.thisObject.getFieldValue("imageView") as? ImageView ?: return
        applyPalette(imageView, param.args[0] as Int)
        imageView.growIcon(withTop = false)
    }

    private fun onCommunityRequestsCellSetAfter(param: MethodHookParam) {
        if (!SettingsState.md3Style) return
        val background = param.thisObject.getFieldValue("iconBackground") as? SettingCell.Background ?: return
        val iconView = param.thisObject.getFieldValue("iconView") as? ImageView ?: return
        val colors = paletteManager.forBaseColor(param.args[0] as Int)

        background.setColor(colors.bgColor, colors.bgColor, false)
        iconView.setColorFilter(colors.iconColor, PorterDuff.Mode.SRC_IN)
        (param.thisObject.getFieldValue("iconLayout") as? View)?.growIcon(withTop = false)
    }

    private fun onPillBadgeUpdateColorsAfter(param: MethodHookParam) {
        if (!SettingsState.md3Style) return
        val badge = param.thisObject
        val background = badge.getFieldValue("background") as? SettingCell.Background ?: return
        val iconView = badge.getFieldValue("iconView") ?: return
        val colors = paletteManager.forBaseColor(badge.getFieldValue("colorTop") as? Int ?: return)

        background.setColor(colors.bgColor, colors.bgColor, false)
        iconView.safeInvoke("setColor", 1, colors.iconColor)
        (badge as? View)?.invalidate()
    }

    companion object {
        private const val TEXT_CELL_STOCK_PADDING = 2
        private const val TEXT_CELL_ICON_PADDING = 5
        private const val MD3_ICON_SIZE = 34
    }
}
