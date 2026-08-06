package org.zwylair.msldex

class SettingsActivityManager {
    @Volatile
    private var settingsActivity: Any? = null

    fun cacheActivity(activity: Any) {
        settingsActivity = activity
    }

    fun clear() {
        settingsActivity = null
    }

    private fun getActionBarLayoutFragmentStack(): List<Any>? {
        val launchActivityClazz = findClass("org.telegram.ui.LaunchActivity") ?: return null
        val instanceField = launchActivityClazz.getDeclaredField("instance").apply { isAccessible = true }
        val instance = instanceField.get(null) ?: return null

        val actionBarLayout = instance.getFieldValue("actionBarLayout") ?: return null

        @Suppress("UNCHECKED_CAST")
        return actionBarLayout.safeInvoke("getFragmentStack") as? List<Any>
    }

    private fun getTelegramFragmentByClass(clazz: Class<*>): Any? {
        val fragmentStack = getActionBarLayoutFragmentStack() ?: return null
        return fragmentStack.firstOrNull { clazz.isInstance(it) }
    }

    fun getSettingsFragment(): Any? {
        settingsActivity?.let { return it }

        val mainTabsClazz = findClass("org.telegram.ui.MainTabsActivity") ?: return null
        val mainTabsFragment = getTelegramFragmentByClass(mainTabsClazz)
        if (mainTabsFragment == null) {
            PluginCore.log("SettingsActivity fragment not found active.")
            return null
        }

        val holders = mainTabsFragment.getFieldValue("fragmentsArr") ?: return null
        val method = mainTabsFragment.findMethod("getPositionCallsOrSettings") ?: return null
        val settingsHolderIndex = method.invoke(mainTabsFragment) ?: return null

        val holder = holders.safeInvoke("get", 1, settingsHolderIndex) ?: return null
        val fragment = holder.getFieldValue("fragment") ?: return null

        val settingsActivityClazz = findClass("org.telegram.ui.SettingsActivity") ?: return null
        if (settingsActivityClazz.isInstance(fragment)) {
            settingsActivity = fragment
            return fragment
        }

        PluginCore.log("SettingsActivity fragment not found active.")
        return null
    }

    fun refreshSettingsActivity() {
        val fragment = getSettingsFragment()
        if (fragment == null) {
            PluginCore.log("SettingsActivity is not initialised.")
            return
        }

        try {
            val listView = fragment.getFieldValue("listView") ?: return
            val adapter = listView.getFieldValue("adapter") ?: return
            adapter.safeInvoke("update", 1, false)
            PluginCore.log("SettingsActivity refreshed successfully.")
        } catch (e: Exception) {
            PluginCore.log("SettingsActivityManager.refreshSettingsActivity failed:", exception = e)
        }
    }
}
