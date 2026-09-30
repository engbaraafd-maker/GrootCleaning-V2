package com.grootcleaning.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grootcleaning.models.RunStatus

@Entity(tableName = "runs")
data class RunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val finishedAt: Long?,
    val status: RunStatus,
    val successful: Int,
    val failed: Int,
    val userAction: Int,
    val skipped: Int,
    val currentIndex: Int,
    val totalTasks: Int
)
