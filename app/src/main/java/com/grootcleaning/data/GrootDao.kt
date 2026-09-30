package com.grootcleaning.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface AppSelectionDao {
    @Query("SELECT * FROM app_selection ORDER BY appName COLLATE NOCASE") fun all(): List<AppSelectionEntity>
    @Query("SELECT * FROM app_selection WHERE selected = 1 ORDER BY appName COLLATE NOCASE") fun selected(): List<AppSelectionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(item: AppSelectionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsertAll(items: List<AppSelectionEntity>)
    @Query("DELETE FROM app_selection") fun deleteAll()
}

@Dao
interface AccountSelectionDao {
    @Query("SELECT * FROM account_selection ORDER BY accountName COLLATE NOCASE") fun all(): List<AccountSelectionEntity>
    @Query("SELECT * FROM account_selection WHERE selected = 1 ORDER BY accountName COLLATE NOCASE") fun selected(): List<AccountSelectionEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(item: AccountSelectionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsertAll(items: List<AccountSelectionEntity>)
    @Query("DELETE FROM account_selection") fun deleteAll()
}

@Dao
interface RunDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(item: RunEntity): Long
    @Update fun update(item: RunEntity)
    @Query("SELECT * FROM runs ORDER BY startedAt DESC LIMIT :limit") fun recent(limit: Int): List<RunEntity>
    @Query("SELECT * FROM runs WHERE id = :id LIMIT 1") fun byId(id: Long): RunEntity?
}

@Dao
interface TaskLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(item: TaskLogEntity): Long
    @Update fun update(item: TaskLogEntity)
    @Query("SELECT * FROM task_logs WHERE runId = :runId ORDER BY startedAt ASC") fun byRun(runId: Long): List<TaskLogEntity>
    @Query("SELECT * FROM task_logs ORDER BY startedAt DESC LIMIT :limit") fun recent(limit: Int): List<TaskLogEntity>
}

@Dao
interface StepLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun insert(item: StepLogEntity): Long
    @Update fun update(item: StepLogEntity)
    @Query("SELECT * FROM step_logs WHERE taskLogId = :taskLogId ORDER BY startedAt ASC") fun byTask(taskLogId: Long): List<StepLogEntity>
}

@Dao
interface PreferencesDao {
    @Query("SELECT * FROM automation_preferences WHERE id = 1 LIMIT 1") fun get(): PreferencesEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun save(item: PreferencesEntity)
}
