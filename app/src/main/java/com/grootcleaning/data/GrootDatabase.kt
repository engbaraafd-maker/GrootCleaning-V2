package com.grootcleaning.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [AppSelectionEntity::class, AccountSelectionEntity::class, RunEntity::class, TaskLogEntity::class, StepLogEntity::class, PreferencesEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(GrootConverters::class)
abstract class GrootDatabase : RoomDatabase() {
    abstract fun appSelectionDao(): AppSelectionDao
    abstract fun accountSelectionDao(): AccountSelectionDao
    abstract fun runDao(): RunDao
    abstract fun taskLogDao(): TaskLogDao
    abstract fun stepLogDao(): StepLogDao
    abstract fun preferencesDao(): PreferencesDao
}
