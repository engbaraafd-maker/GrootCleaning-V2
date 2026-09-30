package com.grootcleaning.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.grootcleaning.models.AutomationSettings
import com.grootcleaning.models.Operation
import com.grootcleaning.models.VerificationLevel
import java.security.MessageDigest

class SelectionRepository(context: Context) {
    private val db = DatabaseProvider.get(context)
    val apps = db.appSelectionDao()
    val accounts = db.accountSelectionDao()
    val runs = db.runDao()
    val tasks = db.taskLogDao()
    val steps = db.stepLogDao()
    val preferences = db.preferencesDao()

    fun loadSettings(): AutomationSettings {
        val p = preferences.get() ?: return AutomationSettings()
        return AutomationSettings(
            timeoutSeconds = p.timeoutSeconds,
            retryCount = p.retryCount,
            delayBetweenTasksMs = p.delayBetweenTasksMs,
            continueAfterFailure = p.continueAfterFailure,
            autoResume = p.autoResume,
            simulationMode = p.simulationMode,
            debugLogging = p.debugLogging,
            verificationLevel = p.verificationLevel,
            includeSystemApps = p.includeSystemApps
        )
    }

    fun saveSettings(s: AutomationSettings) = preferences.save(
        PreferencesEntity(
            timeoutSeconds = s.timeoutSeconds,
            retryCount = s.retryCount,
            delayBetweenTasksMs = s.delayBetweenTasksMs,
            continueAfterFailure = s.continueAfterFailure,
            autoResume = s.autoResume,
            simulationMode = s.simulationMode,
            debugLogging = s.debugLogging,
            verificationLevel = s.verificationLevel,
            includeSystemApps = s.includeSystemApps
        )
    )
}

data class InstalledApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val info: ApplicationInfo
)

class AppCatalog(private val context: Context) {
    private val pm = context.packageManager

    fun load(includeSystemApps: Boolean): List<InstalledApp> {
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { (it.flags and ApplicationInfo.FLAG_INSTALLED) != 0 }
            .filter { includeSystemApps || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { InstalledApp(it.packageName, it.loadLabel(pm).toString(), (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0, it) }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }
}

data class VisibleAccount(val name: String, val type: String, val id: String)

class AccountCatalog(context: Context) {
    private val manager = AccountManager.get(context.applicationContext)

    fun load(): List<VisibleAccount> = try {
        manager.accounts.map { account ->
            VisibleAccount(account.name, account.type, accountId(account))
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    } catch (_: SecurityException) {
        emptyList()
    }

    fun stillVisible(name: String, type: String): Boolean = try {
        manager.accounts.any { it.name == name && it.type == type }
    } catch (_: SecurityException) {
        true
    }

    companion object {
        fun accountId(account: Account): String {
            val bytes = MessageDigest.getInstance("SHA-256")
                .digest("${account.type}\u0000${account.name}".toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
