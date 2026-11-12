package org.zwylair.msldex

import de.robv.android.xposed.XC_MethodHook.MethodHookParam
import org.telegram.tgnet.TLRPC
import org.telegram.ui.Components.UItem
import org.telegram.ui.SettingsActivity

class ListItemsManager {

    private val reorderGroups = listOf(
        listOf(-1, -2, -3),          // Client menus
        listOf(1, 3, 8),             // Account & Security
        listOf(2, 7, 10),            // Interface
        listOf(5, 6, 9),             // Notifications & Data
        listOf(11, 12, 13, 25, 15, 16),  // Monetisation
        listOf(17, 18, 23, 19),      // Info
        listOf(24, 26, 20, 21, 22),  // Experimental & Debug
    )

    fun setupHooks() {
        try {
            val settingsActivityClazz = findClass("org.telegram.ui.SettingsActivity")!!
            settingsActivityClazz.findMethodMaxArgs("fillItems")!!.after { onFillItemsAfter(it) }
        } catch (e: Throwable) {
            PluginCore.log("ListItemsManager.setupHooks failed:", exception = e)
        }
    }

    private fun onFillItemsAfter(param: MethodHookParam) {
        (param.thisObject as? SettingsActivity)?.let { PluginCore.settingsActivityManager.track(it) }

        @Suppress("UNCHECKED_CAST")
        val items = param.args[0] as? ArrayList<UItem> ?: return

        val doReorder = SettingsState.reorderSettingsList
        val doDebloat = SettingsState.needDebloatApplying()
        if (!doReorder && !doDebloat) return

        var list: MutableList<UItem> = ArrayList(items)
        if (doDebloat) list = applyDebloat(list)
        if (doReorder) list = applyReorder(list)
        list = cleanShadows(list)

        items.clear()
        items.addAll(list)
    }

    private fun isSettingOption(item: UItem) = item.instanceOf(SettingsActivity.SettingCell.Factory::class.java)
    private fun isVersionShadow(item: UItem) = item.viewType == -4
    private fun isPureShadow(item: UItem) = item.viewType == 7
    private fun isHeader(item: UItem) = item.viewType == 0

    private fun cleanShadows(items: MutableList<UItem>): MutableList<UItem> {
        if (items.isEmpty()) return items

        val cleaned = ArrayList<UItem>()
        for (item in items) {
            if (isPureShadow(item) && cleaned.isNotEmpty() && isPureShadow(cleaned.last())) continue
            cleaned.add(item)
        }

        if (cleaned.isEmpty()) return cleaned

        val hasVersion = isVersionShadow(cleaned.last())
        val versionItem = if (hasVersion) cleaned.removeAt(cleaned.size - 1) else null

        while (cleaned.isNotEmpty() && isPureShadow(cleaned.last())) {
            cleaned.removeAt(cleaned.size - 1)
        }

        if (versionItem != null) cleaned.add(versionItem)
        return cleaned
    }

    private fun applyDebloat(items: MutableList<UItem>): MutableList<UItem> {
        val blacklistedIds = mutableListOf<Int>()
        if (SettingsState.debloatTgPrem) blacklistedIds.add(11)
        if (SettingsState.debloatTgStars) blacklistedIds.add(12)
        if (SettingsState.debloatMyTon) blacklistedIds.add(13)
        if (SettingsState.debloatTgBusiness) blacklistedIds.add(15)
        if (SettingsState.debloatSendGift) blacklistedIds.add(16)
        if (SettingsState.debloatWallet) blacklistedIds.add(25)
        if (SettingsState.debloatHelpSection) blacklistedIds.addAll(listOf(17, 18, 23, 19))

        if (blacklistedIds.isEmpty()) return items

        val debloated = ArrayList<UItem>()
        for (item in items) {
            if (item.`object` is TLRPC.TL_attachMenuBot && SettingsState.debloatWallet) continue
            if (item.id in blacklistedIds) continue
            if (isHeader(item)) continue
            debloated.add(item)
        }
        return debloated
    }

    private fun applyReorder(items: MutableList<UItem>): MutableList<UItem> {
        val settingsMap = LinkedHashMap<Int, UItem>()
        val botsList = mutableListOf<UItem>()

        for (item in items) {
            if (!isSettingOption(item)) continue

            if (item.`object` is TLRPC.TL_attachMenuBot) {
                botsList.add(item)
            } else if (item.id != 0) {
                settingsMap[item.id] = item
            }
        }

        if (settingsMap.isEmpty() && botsList.isEmpty()) return items

        var firstIdx = -1
        for ((idx, item) in items.withIndex()) {
            if (isSettingOption(item)) {
                firstIdx = idx
                break
            }
        }
        if (firstIdx == -1) return items

        var lastIdx = items.size - 1
        if (!isVersionShadow(items[lastIdx])) lastIdx = items.size

        val head = ArrayList(items.subList(0, firstIdx))
        val tail = ArrayList(items.subList(lastIdx, items.size))

        val reorderedBlock = mutableListOf<UItem>()
        for (group in reorderGroups) {
            var hasAdded = false

            for (itemId in group) {
                settingsMap.remove(itemId)?.let {
                    reorderedBlock.add(it)
                    hasAdded = true
                }
            }

            if (group[0] == 11 && botsList.isNotEmpty()) {
                reorderedBlock.addAll(botsList)
                hasAdded = true
            }

            if (hasAdded) reorderedBlock.add(UItem.asShadow(null))
        }

        reorderedBlock.addAll(settingsMap.values)

        val result = ArrayList<UItem>(head.size + reorderedBlock.size + tail.size)
        result.addAll(head)
        result.addAll(reorderedBlock)
        result.addAll(tail)
        return result
    }
}
