package com.grootcleaning.automation.adapters

import android.os.Build
import com.grootcleaning.automation.detection.TextCatalog

data class UiLabels(
    val storage: List<String> = TextCatalog.storage,
    val clearCache: List<String> = TextCatalog.clearCache,
    val clearData: List<String> = TextCatalog.clearData,
    val manageSpace: List<String> = TextCatalog.manageSpace,
    val all: List<String> = TextCatalog.all,
    val clearAllData: List<String> = TextCatalog.clearAllData,
    val removeAccount: List<String> = TextCatalog.removeAccount
)

interface SystemUiAdapter {
    val id: String
    val settingsPackages: Set<String>
    val labels: UiLabels
}

open class GenericSettingsAdapter : SystemUiAdapter {
    override val id = "generic"
    override val settingsPackages = setOf("com.android.settings")
    override val labels = UiLabels()
}

class AndroidStockAdapter : GenericSettingsAdapter() { override val id = "android-stock" }
class SamsungAdapter : GenericSettingsAdapter() { override val id = "samsung"; override val settingsPackages = setOf("com.android.settings", "com.samsung.android.settings") }
class XiaomiAdapter : GenericSettingsAdapter() { override val id = "xiaomi"; override val settingsPackages = setOf("com.android.settings", "com.miui.securitycenter") }
class HuaweiAdapter : GenericSettingsAdapter() { override val id = "huawei"; override val settingsPackages = setOf("com.android.settings", "com.huawei.systemmanager") }
class OppoAdapter : GenericSettingsAdapter() { override val id = "oppo"; override val settingsPackages = setOf("com.android.settings", "com.coloros.safecenter") }

object SystemUiAdapterRegistry {
    fun current(): SystemUiAdapter = when (Build.MANUFACTURER.lowercase()) {
        "samsung" -> SamsungAdapter()
        "xiaomi", "redmi", "poco" -> XiaomiAdapter()
        "huawei", "honor" -> HuaweiAdapter()
        "oppo", "oneplus", "realme" -> OppoAdapter()
        else -> AndroidStockAdapter()
    }
}
