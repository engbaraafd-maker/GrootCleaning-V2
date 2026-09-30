package com.grootcleaning.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.grootcleaning.models.Operation

@Entity(tableName = "app_selection")
data class AppSelectionEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val operation: Operation,
    val selected: Boolean,
    val isSystemApp: Boolean,
    val updatedAt: Long
)
