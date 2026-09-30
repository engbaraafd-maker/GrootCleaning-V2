package com.grootcleaning.automation.verification

import android.app.AppOpsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Process
import android.os.storage.StorageManager
import com.grootcleaning.utils.GcLog
import java.io.IOException

data class AppStorageStats(val dataBytes: Long, val cacheBytes: Long)

class StorageStatsProvider(private val context: Context) {
    fun isAvailable(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return try {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName) == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) { false }
    }

    fun read(packageName: String): AppStorageStats? {
        if (!isAvailable()) return null
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            val manager = context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val stats = manager.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName, android.os.Process.myUserHandle())
            AppStorageStats(stats.dataBytes, stats.cacheBytes)
        } catch (e: SecurityException) {
            GcLog.verification("Storage stats not available: ${e.message}")
            null
        } catch (e: IOException) {
            GcLog.verification("Storage stats I/O error: ${e.message}")
            null
        } catch (e: Exception) {
            GcLog.verification("Storage stats error: ${e.message}")
            null
        }
    }
}
