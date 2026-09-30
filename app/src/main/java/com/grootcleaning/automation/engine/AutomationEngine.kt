package com.grootcleaning.automation.engine

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.grootcleaning.automation.adapters.SystemUiAdapterRegistry
import com.grootcleaning.automation.detection.AccessibilityBridge
import com.grootcleaning.automation.verification.StorageStatsProvider
import com.grootcleaning.automation.verification.VerificationEngine
import com.grootcleaning.data.AccountCatalog
import com.grootcleaning.data.DatabaseProvider
import com.grootcleaning.data.SelectionRepository
import com.grootcleaning.data.StepLogEntity
import com.grootcleaning.data.TaskLogEntity
import com.grootcleaning.data.RunEntity
import com.grootcleaning.models.AutomationSettings
import com.grootcleaning.models.AutomationTask
import com.grootcleaning.models.Operation
import com.grootcleaning.models.RunStatus
import com.grootcleaning.models.StepStatus
import com.grootcleaning.models.TaskStatus
import com.grootcleaning.models.VerificationResult
import com.grootcleaning.service.accessibility.GrootAccessibilityService
import com.grootcleaning.utils.AppPreferences
import com.grootcleaning.utils.GcLog
import java.util.concurrent.Executors

class AutomationEngine(
    private val context: Context,
    private val service: GrootAccessibilityService
) {
    private val executor = Executors.newSingleThreadExecutor()
    private val db = DatabaseProvider.get(context)
    private val repo = SelectionRepository(context)
    private val accountCatalog = AccountCatalog(context)
    private val bridge = AccessibilityBridge(service)
    @Volatile private var stopRequested = false
    @Volatile private var skipRequested = false
    @Volatile private var running = false
    private var queue: TaskQueue? = null
    private var runId: Long = 0L
    private var settings: AutomationSettings = repo.loadSettings()

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (settings.debugLogging && event != null) GcLog.accessibility("event=${event.eventType} pkg=${event.packageName}")
    }

    fun startFresh() {
        if (running) return
        executor.execute {
            settings = repo.loadSettings()
            val tasks = buildTasks()
            if (tasks.isEmpty()) {
                broadcast("NO_TARGETS")
                return@execute
            }
            stopRequested = false
            skipRequested = false
            runId = db.runDao().insert(RunEntity(startedAt = System.currentTimeMillis(), finishedAt = null, status = RunStatus.RUNNING, successful = 0, failed = 0, userAction = 0, skipped = 0, currentIndex = 0, totalTasks = tasks.size))
            AppPreferences.setLastRunId(context, runId)
            AppPreferences.clearResume(context)
            queue = TaskQueue(tasks)
            running = true
            broadcast("RUNNING")
            executeQueue(0)
        }
    }

    fun resume() {
        if (running) return
        executor.execute {
            val resume = AppPreferences.getResume(context) ?: return@execute
            settings = repo.loadSettings()
            val tasks = buildTasks()
            if (tasks.isEmpty()) return@execute
            runId = resume.first
            queue = TaskQueue(tasks).also { it.setIndex(resume.second) }
            stopRequested = false
            running = true
            broadcast("RESUMING")
            executeQueue(resume.second, resumeStep = resume.third)
        }
    }

    fun retryFailed() {
        if (running) return
        executor.execute {
            val last = AppPreferences.lastRunId(context)
            if (last == 0L) return@execute
            val previous = db.taskLogDao().byRun(last).filter { it.status == TaskStatus.FAILED || it.status == TaskStatus.REQUIRES_USER_ACTION }
            val all = buildTasks()
            val retry = all.filter { task -> previous.any { it.packageName == task.packageName && it.accountName == task.accountName } }
            if (retry.isEmpty()) return@execute
            runId = db.runDao().insert(RunEntity(startedAt = System.currentTimeMillis(), finishedAt = null, status = RunStatus.RUNNING, successful = 0, failed = 0, userAction = 0, skipped = 0, currentIndex = 0, totalTasks = retry.size))
            queue = TaskQueue(retry)
            stopRequested = false
            skipRequested = false
            running = true
            AppPreferences.setLastRunId(context, runId)
            broadcast("RETRY_FAILED")
            executeQueue(0)
        }
    }

    fun requestStop() { stopRequested = true }

    fun skipCurrent() { if (running) skipRequested = true }

    private fun executeQueue(startIndex: Int, onlyRetryFailed: Boolean = false, resumeStep: Int = 0) {
        val q = queue ?: return finish(RunStatus.FAILED)
        var idx = startIndex
        while (idx < q.size()) {
            if (stopRequested) {
                finish(RunStatus.STOPPED)
                return
            }
            q.setIndex(idx)
            if (onlyRetryFailed && !wasTaskFailed(idx)) { idx++; continue }
            val task = q.current() ?: break
            AppPreferences.saveResume(context, runId, idx, 0)
            val existingLogId = if (idx == startIndex && resumeStep > 0) db.taskLogDao().byRun(runId).getOrNull(idx)?.id else null
            val status = runTask(task, idx, if (idx == startIndex) resumeStep else 0, existingLogId)
            if (stopRequested) {
                finish(RunStatus.STOPPED)
                return
            }
            when (status) {
                TaskStatus.SUCCESS, TaskStatus.SKIPPED -> idx++
                TaskStatus.FAILED, TaskStatus.REQUIRES_USER_ACTION -> {
                    if (settings.continueAfterFailure) idx++ else {
                        AppPreferences.saveResume(context, runId, idx, 0)
                        finish(if (status == TaskStatus.REQUIRES_USER_ACTION) RunStatus.COMPLETED_WITH_ERRORS else RunStatus.FAILED)
                        return
                    }
                }
                else -> idx++
            }
            Thread.sleep(settings.delayBetweenTasksMs.coerceAtLeast(0))
        }
        AppPreferences.clearResume(context)
        finish(RunStatus.COMPLETED)
    }

    private fun runTask(task: AutomationTask, index: Int, startStep: Int = 0, existingLogId: Long? = null): TaskStatus {
        val now = System.currentTimeMillis()
        val existing = existingLogId?.let { id -> db.taskLogDao().byRun(runId).firstOrNull { it.id == id } }
        val logId = if (existing != null) {
            db.taskLogDao().update(existing.copy(status = TaskStatus.RUNNING, finishedAt = null, currentStep = existing.currentStep.ifBlank { "PENDING" }, errorMessage = null))
            existing.id
        } else db.taskLogDao().insert(TaskLogEntity(runId = runId, taskType = task.taskType, packageName = task.packageName, appName = task.appName, accountName = task.accountName, accountType = task.accountType, operation = task.operation, status = TaskStatus.RUNNING, startedAt = now, finishedAt = null, currentStep = "PENDING", errorMessage = null, verification = null))
        return try {
            if (task.taskType == "APP") runAppTask(task, logId, index, startStep) else runAccountTask(task, logId, index, startStep)
        } catch (t: Throwable) {
            db.taskLogDao().byRun(runId).firstOrNull { it.id == logId }?.let { row -> db.taskLogDao().update(row.copy(status = TaskStatus.FAILED, finishedAt = System.currentTimeMillis(), currentStep = "EXCEPTION", errorMessage = t.message)) }
            GcLog.error("Automation", "Task exception: ${t.stackTraceToString()}")
            TaskStatus.FAILED
        }
    }

    private fun runAppTask(task: AutomationTask, logId: Long, index: Int, startStep: Int): TaskStatus {
        val adapter = SystemUiAdapterRegistry.current()
        val verification = VerificationEngine(settings.verificationLevel)
        val storage = StorageAutomation(bridge, adapter, StorageStatsProvider(context), verification, settings)
        val packageName = task.packageName ?: return TaskStatus.FAILED
        val appName = task.appName ?: packageName

        if (startStep <= 0) {
            checkpoint(index, 0)
            if (!step(logId, "Open App Info") { storage.openAppInfo(packageName) }) {
                return fail(logId, "Could not open App Info")
            }
        }
        if (startStep > 0) {
            val onStorage = bridge.waitFor(500L) { nodes ->
                com.grootcleaning.automation.detection.NodeSnapshotMatcher.anyText(nodes, adapter.labels.clearCache + adapter.labels.clearData + adapter.labels.manageSpace)
            }
            if (!onStorage) {
                if (!storage.ensureAppInfo(packageName, appName)) {
                    if (!storage.openAppInfo(packageName) || !storage.ensureAppInfo(packageName, appName)) {
                        return userAction(logId, "App Info screen not verified while resuming")
                    }
                }
            }
        } else if (!storage.ensureAppInfo(packageName, appName)) {
            return userAction(logId, "App Info screen not verified")
        }
        if (stopRequested) return stopped(logId)
        if (skipRequested) return skipped(logId, "Skipped by user")
        if (startStep <= 1) {
            checkpoint(index, 1)
            if (!step(logId, "Open Storage") { storage.openStorage(appName) }) {
                return fail(logId, "Could not open Storage")
            }
        } else {
            if (!bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes -> com.grootcleaning.automation.detection.NodeSnapshotMatcher.anyText(nodes, adapter.labels.clearCache + adapter.labels.clearData + adapter.labels.manageSpace) }) {
                storage.openAppInfo(packageName)
                if (!storage.ensureAppInfo(packageName, appName)) return userAction(logId, "Could not recover App Info screen")
                storage.openStorage(appName)
            }
        }
        if (!bridge.waitFor(settings.timeoutSeconds * 1000L) { nodes ->
                com.grootcleaning.automation.detection.NodeSnapshotMatcher.anyText(nodes, adapter.labels.clearCache + adapter.labels.clearData + adapter.labels.manageSpace)
            }) return userAction(logId, "Storage screen not detected")
        if (stopRequested) return stopped(logId)
        if (skipRequested) return skipped(logId, "Skipped by user")

        var overallVerification: VerificationResult? = null
        if ((task.operation == Operation.CLEAR_CACHE || task.operation == Operation.CLEAR_DATA_AND_CACHE) && startStep <= 2) {
            checkpoint(index, 2)
            val result = storage.clearCache(packageName, settings.simulationMode)
            logResult(logId, "Clear Cache", result.status, result.detail)
            overallVerification = result.verification
            if (result.status == StorageAutomation.StepOutcome.FAILED) return fail(logId, result.detail)
            if (result.status == StorageAutomation.StepOutcome.REQUIRES_USER_ACTION) return userAction(logId, result.detail ?: "Clear Cache requires user action")
            if (stopRequested) return stopped(logId)
            if (skipRequested) return skipped(logId, "Skipped by user")
        }
        if (task.operation == Operation.CLEAR_DATA || task.operation == Operation.CLEAR_DATA_AND_CACHE) {
            checkpoint(index, 3)
            val result = storage.clearData(packageName, settings.simulationMode)
            logResult(logId, "Clear Data", result.status, result.detail)
            overallVerification = result.verification ?: overallVerification
            if (result.status == StorageAutomation.StepOutcome.FAILED) return fail(logId, result.detail)
            if (result.status == StorageAutomation.StepOutcome.REQUIRES_USER_ACTION) return userAction(logId, result.detail ?: "Clear Data requires user action")
            if (stopRequested) return stopped(logId)
            if (skipRequested) return skipped(logId, "Skipped by user")
        }

        db.taskLogDao().update(db.taskLogDao().byRun(runId).first { it.id == logId }.copy(status = TaskStatus.SUCCESS, finishedAt = System.currentTimeMillis(), currentStep = "Complete", errorMessage = null, verification = overallVerification ?: VerificationResult.SUCCESS))
        broadcast("TASK_SUCCESS:$index")
        return TaskStatus.SUCCESS
    }

    private fun runAccountTask(task: AutomationTask, logId: Long, index: Int, startStep: Int): TaskStatus {
        val adapter = SystemUiAdapterRegistry.current()
        val verification = VerificationEngine(settings.verificationLevel)
        val account = AccountAutomation(bridge, adapter, accountCatalog, verification, settings)
        val name = task.accountName ?: return TaskStatus.FAILED
        val type = task.accountType ?: return TaskStatus.FAILED

        if (startStep <= 0) {
            checkpoint(index, 0)
            if (!step(logId, "Open Accounts") { account.openAccounts() }) {
                return fail(logId, "Could not open Accounts settings")
            }
        }
        if (!account.ensureAccountsScreen()) {
            account.openAccounts()
            if (!account.ensureAccountsScreen()) return userAction(logId, "Accounts screen not verified")
        }
        if (stopRequested) return stopped(logId)
        if (skipRequested) return skipped(logId, "Skipped by user")
        checkpoint(index, 1)
        val result = account.remove(name, type, settings.simulationMode)
        logResult(logId, "Remove Account", result.outcome, result.detail)
        if (result.outcome == StorageAutomation.StepOutcome.REQUIRES_USER_ACTION) return userAction(logId, result.detail ?: "Remove Account requires user action")
        if (result.outcome == StorageAutomation.StepOutcome.FAILED) return fail(logId, result.detail)
        val accountVerification = result.verification ?: VerificationResult.UNCERTAIN
        if (accountVerification != VerificationResult.SUCCESS) {
            return userAction(logId, result.detail ?: "Account removal could not be verified with sufficient evidence")
        }
        db.taskLogDao().update(db.taskLogDao().byRun(runId).first { it.id == logId }.copy(status = TaskStatus.SUCCESS, finishedAt = System.currentTimeMillis(), currentStep = "Complete", verification = accountVerification))
        broadcast("TASK_SUCCESS:$index")
        return TaskStatus.SUCCESS
    }

    private fun step(logId: Long, name: String, action: () -> Boolean): Boolean {
        val task = db.taskLogDao().byRun(runId).firstOrNull { it.id == logId }
        if (task != null) db.taskLogDao().update(task.copy(currentStep = name, status = TaskStatus.RUNNING))
        val started = System.currentTimeMillis()
        val stepId = db.stepLogDao().insert(StepLogEntity(taskLogId = logId, name = name, status = StepStatus.RUNNING, startedAt = started, finishedAt = null, detail = null))
        val ok = RetryPolicy(settings.retryCount).run({ action() }) { result -> !result }
        val status = if (ok) StepStatus.SUCCESS else StepStatus.FAILED
        val row = db.stepLogDao().byTask(logId).firstOrNull { it.id == stepId }
        if (row != null) db.stepLogDao().update(row.copy(status = status, finishedAt = System.currentTimeMillis(), detail = if (ok) null else "Action returned false"))
        GcLog.automation("$name -> $status")
        return ok
    }

    private fun logResult(logId: Long, name: String, status: StorageAutomation.StepOutcome, detail: String?) {
        val stepStatus = when (status) {
            StorageAutomation.StepOutcome.SUCCESS -> StepStatus.SUCCESS
            StorageAutomation.StepOutcome.FAILED -> StepStatus.FAILED
            StorageAutomation.StepOutcome.REQUIRES_USER_ACTION -> StepStatus.REQUIRES_USER_ACTION
            StorageAutomation.StepOutcome.SKIPPED -> StepStatus.SKIPPED
        }
        db.stepLogDao().insert(StepLogEntity(taskLogId = logId, name = name, status = stepStatus, startedAt = System.currentTimeMillis(), finishedAt = System.currentTimeMillis(), detail = detail))
        db.taskLogDao().byRun(runId).firstOrNull { it.id == logId }?.let { row ->
            db.taskLogDao().update(row.copy(currentStep = name, errorMessage = detail))
        }
    }

    private fun fail(logId: Long, message: String?): TaskStatus {
        db.taskLogDao().update(db.taskLogDao().byRun(runId).first { it.id == logId }.copy(status = TaskStatus.FAILED, finishedAt = System.currentTimeMillis(), currentStep = "Failed", errorMessage = message))
        broadcast("TASK_FAILED")
        return TaskStatus.FAILED
    }

    private fun userAction(logId: Long, message: String): TaskStatus {
        db.taskLogDao().update(db.taskLogDao().byRun(runId).first { it.id == logId }.copy(status = TaskStatus.REQUIRES_USER_ACTION, finishedAt = System.currentTimeMillis(), currentStep = "REQUIRES_USER_ACTION", errorMessage = message, verification = VerificationResult.UNCERTAIN))
        broadcast("REQUIRES_USER_ACTION:$message")
        return TaskStatus.REQUIRES_USER_ACTION
    }

    private fun stopped(logId: Long? = null): TaskStatus {
        if (logId != null) db.taskLogDao().byRun(runId).firstOrNull { it.id == logId }?.let { db.taskLogDao().update(it.copy(status = TaskStatus.PENDING, currentStep = "Stopped", errorMessage = "Stopped by user")) }
        broadcast("STOPPED")
        return TaskStatus.PENDING
    }

    private fun skipped(logId: Long, detail: String): TaskStatus {
        db.taskLogDao().byRun(runId).firstOrNull { it.id == logId }?.let { db.taskLogDao().update(it.copy(status = TaskStatus.SKIPPED, finishedAt = System.currentTimeMillis(), currentStep = "Skipped", errorMessage = detail)) }
        broadcast("SKIPPED")
        return TaskStatus.SKIPPED
    }

    private fun checkpoint(taskIndex: Int, stepIndex: Int) {
        AppPreferences.saveResume(context, runId, taskIndex, stepIndex)
        if (stopRequested) broadcast("STOP_REQUESTED")
    }

    private fun finish(status: RunStatus) {
        running = false
        val logs = db.taskLogDao().byRun(runId)
        val successful = logs.count { it.status == TaskStatus.SUCCESS }
        val failed = logs.count { it.status == TaskStatus.FAILED }
        val userAction = logs.count { it.status == TaskStatus.REQUIRES_USER_ACTION }
        val skipped = logs.count { it.status == TaskStatus.SKIPPED }
        val existing = db.runDao().byId(runId) ?: return
        val finalStatus = when {
            status == RunStatus.COMPLETED && (failed > 0 || userAction > 0) -> RunStatus.COMPLETED_WITH_ERRORS
            else -> status
        }
        db.runDao().update(existing.copy(finishedAt = System.currentTimeMillis(), status = finalStatus, successful = successful, failed = failed, userAction = userAction, skipped = skipped, currentIndex = queue?.currentIndex() ?: existing.currentIndex))
        broadcast("FINISHED:${status.name}")
        if (status != RunStatus.STOPPED) AppPreferences.clearResume(context)
    }

    private fun buildTasks(): List<AutomationTask> {
        val apps = db.appSelectionDao().selected().mapIndexed { index, e -> AutomationTask(id = index.toLong(), packageName = e.packageName, appName = e.appName, operation = e.operation, taskLabel = e.appName, taskType = "APP") }
        val accounts = db.accountSelectionDao().selected().mapIndexed { index, e -> AutomationTask(id = 10_000L + index, accountName = e.accountName, accountType = e.accountType, taskLabel = e.accountName, taskType = "ACCOUNT") }
        return apps + accounts
    }

    private fun wasTaskFailed(index: Int): Boolean {
        val logs = db.taskLogDao().byRun(runId)
        val apps = buildTasks()
        val task = apps.getOrNull(index) ?: return false
        return logs.any { it.packageName == task.packageName && it.accountName == task.accountName && it.status != TaskStatus.SUCCESS }
    }

    private fun broadcast(message: String) {
        context.sendBroadcast(Intent(GrootAccessibilityService.ACTION_STATE).setPackage(context.packageName).putExtra(GrootAccessibilityService.EXTRA_MESSAGE, message))
    }
}
