package com.grootcleaning.models

enum class StepStatus { PENDING, RUNNING, SUCCESS, FAILED, SKIPPED, REQUIRES_USER_ACTION }
enum class RunStatus { PENDING, RUNNING, COMPLETED, COMPLETED_WITH_ERRORS, FAILED, STOPPED }
enum class VerificationLevel { BASIC, STANDARD, STRICT }
enum class VerificationResult { SUCCESS, FAILED, UNCERTAIN }

data class AutomationSettings(
    val timeoutSeconds: Int = 12,
    val retryCount: Int = 3,
    val delayBetweenTasksMs: Long = 300L,
    val continueAfterFailure: Boolean = true,
    val autoResume: Boolean = true,
    val simulationMode: Boolean = false,
    val debugLogging: Boolean = true,
    val verificationLevel: VerificationLevel = VerificationLevel.STANDARD,
    val includeSystemApps: Boolean = false
)

data class ExecutionSummary(
    val successful: Int,
    val failed: Int,
    val userAction: Int,
    val skipped: Int
)

data class AutomationTask(
    val id: Long,
    val packageName: String? = null,
    val appName: String? = null,
    val accountName: String? = null,
    val accountType: String? = null,
    val operation: Operation? = null,
    val taskLabel: String,
    val taskType: String
)
