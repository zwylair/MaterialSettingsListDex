package org.zwylair.msldex

object SettingsState {
    @Volatile var reorderSettingsList: Boolean = false

    @Volatile var debloatTgPrem: Boolean = false
    @Volatile var debloatTgStars: Boolean = false
    @Volatile var debloatMyTon: Boolean = false
    @Volatile var debloatWallet: Boolean = false
    @Volatile var debloatTgBusiness: Boolean = false
    @Volatile var debloatSendGift: Boolean = false
    @Volatile var debloatHelpSection: Boolean = false

    fun needDebloatApplying(): Boolean =
        debloatTgPrem || debloatTgStars || debloatMyTon || debloatWallet ||
            debloatTgBusiness || debloatSendGift || debloatHelpSection
}
