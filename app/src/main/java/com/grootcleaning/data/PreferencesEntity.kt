package com.grootcleaning.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grootcleaning.models.VerificationLevel

@Entity(tableName = "automation_preferences")
data class PreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val timeoutSeconds: Int,
    val retryCount: Int,
    val delayBetweenTasksMs: Long,
    val continueAfterFailure: Boolean,
    val autoResume: Boolean,
    val simulationMode: Boolean,
    val debugLogging: Boolean,
    val verificationLevel: VerificationLevel,
    val includeSystemApps: Boolean
)
