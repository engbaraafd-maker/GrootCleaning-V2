package com.grootcleaning.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_selection")
data class AccountSelectionEntity(
    @PrimaryKey val id: String,
    val accountName: String,
    val accountType: String,
    val selected: Boolean,
    val updatedAt: Long
)
