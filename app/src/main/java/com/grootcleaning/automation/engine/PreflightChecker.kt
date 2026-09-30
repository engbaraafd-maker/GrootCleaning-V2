package com.grootcleaning.automation.engine

import android.content.Context
import android.os.Build
import android.content.Intent
import android.provider.Settings
import com.grootcleaning.data.DatabaseProvider
import com.grootcleaning.service.accessibility.GrootAccessibilityService

data class PreflightResult(
    val accessibility: Boolean,
    val targets: Boolean,
    val automationEngine: Boolean,
    val settingsNavigation: Boolean,
    val deviceCompatibility: Boolean,
    val issues: List<String>
) {
    val ready: Boolean get() = issues.isEmpty()
}

class PreflightChecker(private val context: Context) {
    fun run(): PreflightResult {
        val db = DatabaseProvider.get(context)
        val targetCount = db.appSelectionDao().selected().size + db.accountSelectionDao().selected().size
        val issues = mutableListOf<String>()
        val accessibility = GrootAccessibilityService.INSTANCE != null
        val targets = targetCount > 0
        val compatibility = Build.VERSION.SDK_INT >= 27
        val settingsNavigation = context.packageManager.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0) != null
        val automationEngine = accessibility
        if (!accessibility) issues += "AccessibilityService is disabled"
        if (!targets) issues += "No selected apps or accounts"
        if (!compatibility) issues += "Android 8.1/API 27 or newer is required"
        if (!settingsNavigation) issues += "Android Settings activity is not available"
        return PreflightResult(
            accessibility = accessibility,
            targets = targets,
            automationEngine = automationEngine,
            settingsNavigation = settingsNavigation,
            deviceCompatibility = compatibility,
            issues = issues
        )
    }
}
