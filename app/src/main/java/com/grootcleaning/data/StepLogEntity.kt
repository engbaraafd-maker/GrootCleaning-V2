package com.grootcleaning.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grootcleaning.models.StepStatus

@Entity(tableName = "step_logs")
data class StepLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskLogId: Long,
    val name: String,
    val status: StepStatus,
    val startedAt: Long,
    val finishedAt: Long?,
    val detail: String?
)
