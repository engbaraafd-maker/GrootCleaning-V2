package com.grootcleaning.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grootcleaning.models.Operation
import com.grootcleaning.models.TaskStatus
import com.grootcleaning.models.VerificationResult

@Entity(tableName = "task_logs")
data class TaskLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runId: Long,
    val taskType: String,
    val packageName: String?,
    val appName: String?,
    val accountName: String?,
    val accountType: String?,
    val operation: Operation?,
    val status: TaskStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val currentStep: String,
    val errorMessage: String?,
    val verification: VerificationResult?
)
