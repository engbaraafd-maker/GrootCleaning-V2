package com.grootcleaning.automation.engine

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.grootcleaning.automation.adapters.SystemUiAdapter
import com.grootcleaning.automation.detection.AccessibilityBridge
import com.grootcleaning.automation.detection.NodeMatchSpec
import com.grootcleaning.automation.detection.NodeSnapshotMatcher
import com.grootcleaning.automation.detection.TextCatalog
import com.grootcleaning.automation.verification.StorageStatsProvider
import com.grootcleaning.automation.verification.VerificationEngine
import com.grootcleaning.models.AutomationSettings
import com.grootcleaning.models.Operation
import com.grootcleaning.models.VerificationResult
import com.grootcleaning.utils.GcLog

class StorageAutomation(
    private val bridge: AccessibilityBridge,
    private val adapter: SystemUiAdapter,
    private val stats: StorageStatsProvider,
    private val verification: VerificationEngine,
    private val settings: AutomationSettings
) {
    data class Result(val status: StepOutcome, val verification: VerificationResult?, val detail: String? = null)
    enum class StepOutcome { SUCCESS, FAILED, REQUIRES_USER_ACTION, SKIPPED }

    fun openAppInfo(packageName: String): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply { data = Uri.parse("package:$packageName") }
        return bridge.open(intent)
    }

    fun ensureAppInfo(packageName: String, appName: String): Boolean = bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes ->
        val pkg = bridge.currentWindowPackage()
        val packageOk = adapter.settingsPackages.contains(pkg) || (pkg?.contains("settings", ignoreCase = true) == true)
        packageOk && NodeSnapshotMatcher.anyText(nodes, listOf(appName, packageName))
    }

    fun openStorage(appName: String): Boolean {
        return clickText(adapter.labels.storage) && bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes ->
            NodeSnapshotMatcher.anyText(nodes, adapter.labels.clearCache + adapter.labels.clearData + adapter.labels.manageSpace)
        }
    }

    fun clearCache(packageName: String, simulate: Boolean): Result {
        if (simulate) {
            val found = bridge.snapshot().any { node -> NodeSnapshotMatcher.anyText(listOf(node), adapter.labels.clearCache) }
            return if (found) Result(StepOutcome.SUCCESS, VerificationResult.SUCCESS, "Simulation: would click Clear Cache") else Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Clear Cache control not detected")
        }
        val before = stats.read(packageName)
        val beforeNodes = bridge.snapshot()
        val clicked = clickText(adapter.labels.clearCache)
        if (!clicked) return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Clear Cache control not found")
        Thread.sleep(450)
        val afterNodes = bridge.snapshot()
        val after = stats.read(packageName)
        val targetStillEnabled = isEnabledText(afterNodes, adapter.labels.clearCache)
        val result = verification.verifyCache(before, after, clicked, beforeNodes, afterNodes, targetStillEnabled)
        GcLog.verification("Cache verification: $result")
        return when (result) {
            VerificationResult.FAILED -> Result(StepOutcome.FAILED, result, "Cache verification failed")
            VerificationResult.UNCERTAIN -> Result(StepOutcome.REQUIRES_USER_ACTION, result, "Cache action executed but could not be verified reliably")
            VerificationResult.SUCCESS -> Result(StepOutcome.SUCCESS, result)
        }
    }

    fun clearData(packageName: String, simulate: Boolean): Result {
        if (simulate) return simulateDataPath()

        val before = stats.read(packageName)
        val beforeNodes = bridge.snapshot()
        if (hasConfirmationDialog()) clickConfirmationIfSafe()

        if (clickText(adapter.labels.clearData)) {
            waitAndConfirmIfPresent()
        } else if (clickText(adapter.labels.manageSpace)) {
            if (!bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes -> NodeSnapshotMatcher.anyText(nodes, adapter.labels.all + adapter.labels.clearAllData) }) {
                return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Manage Space flow not detected")
            }
            if (NodeSnapshotMatcher.anyText(bridge.snapshot(), adapter.labels.all)) clickText(adapter.labels.all)
            if (!clickText(adapter.labels.clearAllData)) return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Clear All Data control not found")
            waitAndConfirmIfPresent()
        } else {
            return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Clear Data / Manage Space control not found")
        }

        Thread.sleep(700)
        val afterNodes = bridge.snapshot()
        val after = stats.read(packageName)
        val targetStillEnabled = isEnabledText(
            afterNodes,
            adapter.labels.clearData + adapter.labels.clearAllData
        )
        val result = verification.verifyData(before, after, true, beforeNodes, afterNodes, targetStillEnabled)
        GcLog.verification("Data verification: $result")
        return when (result) {
            VerificationResult.FAILED -> Result(StepOutcome.FAILED, result, "Data verification failed")
            VerificationResult.UNCERTAIN -> Result(StepOutcome.REQUIRES_USER_ACTION, result, "Data action executed but could not be verified reliably")
            VerificationResult.SUCCESS -> Result(StepOutcome.SUCCESS, result)
        }
    }

    private fun simulateDataPath(): Result {
        var nodes = bridge.snapshot()
        if (NodeSnapshotMatcher.anyText(nodes, adapter.labels.clearData)) {
            return Result(StepOutcome.SUCCESS, VerificationResult.SUCCESS, "Simulation: would click Clear Data")
        }
        if (NodeSnapshotMatcher.anyText(nodes, adapter.labels.manageSpace)) {
            if (!clickText(adapter.labels.manageSpace)) return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Manage Space could not be opened in simulation")
            val opened = bridge.waitFor(settings.timeoutSeconds * 1000L) { current -> NodeSnapshotMatcher.anyText(current, adapter.labels.all + adapter.labels.clearAllData) }
            if (!opened) return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Manage Space page not detected in simulation")
            nodes = bridge.snapshot()
            if (NodeSnapshotMatcher.anyText(nodes, adapter.labels.all)) clickText(adapter.labels.all)
            if (NodeSnapshotMatcher.anyText(bridge.snapshot(), adapter.labels.clearAllData)) return Result(StepOutcome.SUCCESS, VerificationResult.SUCCESS, "Simulation: would click Clear All Data")
            return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Clear All Data control not detected in simulation")
        }
        return Result(StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Clear Data and Manage Space not detected")
    }

    private fun clickText(texts: List<String>): Boolean = bridge.click(NodeMatchSpec(texts = texts, requireClickable = false, requireEnabled = true))

    private fun isEnabledText(nodes: List<com.grootcleaning.automation.detection.NodeSnapshot>, texts: List<String>): Boolean =
        nodes.any { node ->
            node.enabled && (texts.any { term ->
                node.text.contains(term, ignoreCase = true) ||
                    node.contentDescription.contains(term, ignoreCase = true)
            })
        }

    private fun hasConfirmationDialog(): Boolean {
        val nodes = bridge.snapshot()
        val hasConfirm = NodeSnapshotMatcher.anyText(nodes, TextCatalog.confirm)
        val hasDanger = NodeSnapshotMatcher.anyText(nodes, TextCatalog.clearData + TextCatalog.clearAllData)
        return hasConfirm && hasDanger
    }

    private fun clickConfirmationIfSafe(): Boolean {
        val nodes = bridge.snapshot()
        if (!NodeSnapshotMatcher.anyText(nodes, TextCatalog.clearData + TextCatalog.clearAllData)) return false
        return bridge.click(NodeMatchSpec(texts = TextCatalog.confirm, requireClickable = true, requireEnabled = true))
    }

    private fun waitAndConfirmIfPresent() {
        repeat(8) {
            if (hasConfirmationDialog()) {
                clickConfirmationIfSafe()
                return
            }
            Thread.sleep(180)
        }
    }
}
