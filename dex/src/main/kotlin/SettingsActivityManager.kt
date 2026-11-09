package org.zwylair.msldex

import android.util.SparseArray
import java.util.Collections
import java.util.WeakHashMap
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.Components.UniversalRecyclerView
import org.telegram.ui.SettingsActivity

class SettingsActivityManager {
    private val activities: MutableSet<SettingsActivity> = Collections.newSetFromMap(WeakHashMap())

    fun track(activity: SettingsActivity) {
        synchronized(activities) { activities.add(activity) }
    }

    private fun collectFromFragmentStack() {
        val launchActivity = findClass("org.telegram.ui.LaunchActivity")?.getField("instance")?.get(null) ?: return
        val layout = launchActivity.getFieldValue("actionBarLayout") as? INavigationLayout ?: return
        val stack = layout.fragmentStack ?: return
        val viewPagerClazz = findClass("org.telegram.ui.ViewPagerActivity")
        for (fragment in stack.toList()) {
            if (fragment is SettingsActivity) {
                track(fragment)
                continue
            }
            if (viewPagerClazz?.isInstance(fragment) != true) continue

            val states = fragment.getFieldValue("fragmentsArr") as? SparseArray<*> ?: continue
            for (i in 0 until states.size()) {
                val tab = states.valueAt(i)?.getFieldValue("fragment") as? BaseFragment
                if (tab is SettingsActivity) track(tab)
            }
        }
    }

    fun refreshSettingsActivities() {
        AndroidUtilities.runOnUIThread {
            try {
                collectFromFragmentStack()
                val snapshot = synchronized(activities) { activities.toList() }
                for (activity in snapshot) {
                    (activity.getFieldValue("listView") as? UniversalRecyclerView)?.adapter?.update(false)
                }
                PluginCore.log("Refreshed ${snapshot.size} SettingsActivity instance(s).")
            } catch (e: Throwable) {
                PluginCore.log("SettingsActivityManager.refreshSettingsActivities failed:", exception = e)
            }
        }
    }
}
