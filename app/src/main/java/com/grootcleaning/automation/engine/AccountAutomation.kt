package com.grootcleaning.automation.engine

import android.content.Intent
import android.provider.Settings
import com.grootcleaning.automation.adapters.SystemUiAdapter
import com.grootcleaning.automation.detection.AccessibilityBridge
import com.grootcleaning.automation.detection.NodeMatchSpec
import com.grootcleaning.automation.detection.NodeSnapshotMatcher
import com.grootcleaning.automation.verification.VerificationEngine
import com.grootcleaning.data.AccountCatalog
import com.grootcleaning.models.AutomationSettings
import com.grootcleaning.models.VerificationResult
import com.grootcleaning.utils.GcLog

class AccountAutomation(
    private val bridge: AccessibilityBridge,
    private val adapter: SystemUiAdapter,
    private val accountCatalog: AccountCatalog,
    private val verification: VerificationEngine,
    private val settings: AutomationSettings
) {
    data class Result(val outcome: StorageAutomation.StepOutcome, val verification: VerificationResult?, val detail: String? = null)

    fun openAccounts(): Boolean = bridge.open(Intent(Settings.ACTION_SYNC_SETTINGS))

    fun ensureAccountsScreen(): Boolean = bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes ->
        val pkg = bridge.currentWindowPackage()
        val settingsWindow = adapter.settingsPackages.contains(pkg) || (pkg?.contains("settings", true) == true)
        settingsWindow && NodeSnapshotMatcher.anyText(nodes, listOf("accounts", "accounts & sync", "الحسابات", "الحسابات والمزامنة"))
    }

    fun remove(accountName: String, accountType: String, simulate: Boolean): Result {
        if (simulate) {
            val clicked = bridge.click(NodeMatchSpec(texts = listOf(accountName), requireEnabled = true))
            if (!clicked) return Result(StorageAutomation.StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Simulation: selected account row was not found")
            val removeDetected = bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes ->
                NodeSnapshotMatcher.anyText(nodes, adapter.labels.removeAccount)
            }
            bridge.back()
            return if (removeDetected) {
                Result(StorageAutomation.StepOutcome.SUCCESS, VerificationResult.SUCCESS, "Simulation: account and Remove Account control detected; no deletion performed")
            } else {
                Result(StorageAutomation.StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Simulation: Remove Account control not detected")
            }
        }
        val visibleBefore = accountCatalog.stillVisible(accountName, accountType)
        if (!visibleBefore) return Result(StorageAutomation.StepOutcome.SUCCESS, VerificationResult.SUCCESS, "Account already absent")

        val clickedAccount = bridge.click(NodeMatchSpec(texts = listOf(accountName), requireEnabled = true))
        if (!clickedAccount) return Result(StorageAutomation.StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Selected account row was not found")
        if (!bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes -> NodeSnapshotMatcher.anyText(nodes, adapter.labels.removeAccount) }) {
            return Result(StorageAutomation.StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Remove Account control not found")
        }
        if (!bridge.click(NodeMatchSpec(texts = adapter.labels.removeAccount, requireClickable = false, requireEnabled = true))) {
            return Result(StorageAutomation.StepOutcome.REQUIRES_USER_ACTION, VerificationResult.UNCERTAIN, "Remove Account control could not be activated")
        }
        repeat(8) {
            val danger = NodeSnapshotMatcher.anyText(bridge.snapshot(), adapter.labels.removeAccount + com.grootcleaning.automation.detection.TextCatalog.confirm)
            if (danger) {
                bridge.click(NodeMatchSpec(texts = com.grootcleaning.automation.detection.TextCatalog.confirm, requireClickable = true, requireEnabled = true))
                return@repeat
            }
            Thread.sleep(180)
        }
        Thread.sleep(900)
        val stillVisible = accountCatalog.stillVisible(accountName, accountType)
        val result = verification.verifyAccount(stillVisible, true)
        GcLog.accounts("Account removal verification: $result")
        return when (result) {
            VerificationResult.SUCCESS -> Result(StorageAutomation.StepOutcome.SUCCESS, result)
            VerificationResult.FAILED -> Result(StorageAutomation.StepOutcome.FAILED, result, "Account is still visible")
            VerificationResult.UNCERTAIN -> Result(StorageAutomation.StepOutcome.REQUIRES_USER_ACTION, result, "Account action executed but could not be verified reliably")
        }
    }
}
