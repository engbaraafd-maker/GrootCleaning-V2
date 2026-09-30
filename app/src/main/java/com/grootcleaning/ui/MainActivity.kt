package com.grootcleaning.ui

import android.content.*
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.grootcleaning.R
import com.grootcleaning.service.accessibility.GrootAccessibilityService
import com.grootcleaning.utils.AppPreferences
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val background = Executors.newSingleThreadExecutor()
    private lateinit var container: FrameLayout
    private lateinit var bottom: LinearLayout
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var toolbarStatus: TextView
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra(GrootAccessibilityService.EXTRA_MESSAGE)
            (supportFragmentManager.findFragmentById(container.id) as? ExecutionFragment)?.refreshFromBroadcast(message)
            updateToolbarStatus(message)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        container = findViewById(R.id.main_container)
        bottom = findViewById(R.id.bottom_nav)
        title = findViewById(R.id.toolbar_title)
        subtitle = findViewById(R.id.toolbar_subtitle)
        toolbarStatus = findViewById(R.id.toolbar_status)
        setupNav()
        show(HomeFragment(), navId = R.id.nav_home)
        maybeShowAccessibilityDisclosure()
        updateToolbarStatus(null)
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter(GrootAccessibilityService.ACTION_STATE)
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED) else registerReceiver(receiver, filter)
        updateToolbarStatus(null)
    }

    override fun onStop() {
        try { unregisterReceiver(receiver) } catch (_: Exception) {}
        super.onStop()
    }

    fun show(fragment: Fragment, hideNav: Boolean = false, navId: Int? = null) {
        supportFragmentManager.beginTransaction().replace(R.id.main_container, fragment).commit()
        bottom.visibility = if (hideNav) View.GONE else View.VISIBLE
        navId?.let(::setActiveNav)
        toolbarStatus.text = if (hideNav) getString(R.string.running) else getString(R.string.ready).uppercase()
    }

    private fun setupNav() {
        val items = listOf(
            R.id.nav_home to { show(HomeFragment(), navId = R.id.nav_home) },
            R.id.nav_apps to { show(AppsFragment(), navId = R.id.nav_apps) },
            R.id.nav_accounts to { show(AccountsFragment(), navId = R.id.nav_accounts) },
            R.id.nav_logs to { show(LogsFragment(), navId = R.id.nav_logs) },
            R.id.nav_settings to { show(SettingsFragment(), navId = R.id.nav_settings) }
        )
        items.forEach { (id, action) -> findViewById<TextView>(id).setOnClickListener { action() } }
    }

    private fun setActiveNav(activeId: Int) {
        val ids = intArrayOf(R.id.nav_home, R.id.nav_apps, R.id.nav_accounts, R.id.nav_logs, R.id.nav_settings)
        ids.forEach { id ->
            val view = findViewById<TextView>(id)
            val active = id == activeId
            view.isSelected = active
            view.setTextColor(ContextCompat.getColor(this, if (active) R.color.gc_text_primary else R.color.gc_text_secondary))
            view.background = ContextCompat.getDrawable(this, if (active) R.drawable.bg_nav_active else R.drawable.bg_nav_inactive)
        }
    }

    private fun updateToolbarStatus(message: String?) {
        val active = message ?: ""
        val text: String
        val color: Int
        when {
            active.startsWith("RUNNING") || active.startsWith("TASK_") || active == "RESUMING" -> {
                text = getString(R.string.cleaning)
                color = R.color.gc_primary
            }
            active.startsWith("REQUIRES_USER_ACTION") -> {
                text = getString(R.string.action_required)
                color = R.color.gc_warning
            }
            active.startsWith("FINISHED") -> {
                text = if (active.contains("COMPLETED")) getString(R.string.ready).uppercase() else getString(R.string.failed).uppercase()
                color = if (active.contains("COMPLETED")) R.color.gc_success else R.color.gc_error
            }
            else -> {
                text = if (GrootAccessibilityService.INSTANCE != null) getString(R.string.ready).uppercase() else getString(R.string.accessibility_disabled).uppercase()
                color = if (GrootAccessibilityService.INSTANCE != null) R.color.gc_success else R.color.gc_warning
            }
        }
        toolbarStatus.text = text
        toolbarStatus.setTextColor(ContextCompat.getColor(this, color))
    }

    fun openAccessibilitySettings() { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    fun openUsageSettings() { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }

    fun startAutomation() {
        val service = GrootAccessibilityService.INSTANCE
        if (service == null) {
            showPreflight(listOf(getString(R.string.accessibility_disabled)))
            return
        }
        background.execute {
            val result = com.grootcleaning.automation.engine.PreflightChecker(this).run()
            runOnUiThread {
                if (result.ready) {
                    sendBroadcast(Intent(GrootAccessibilityService.ACTION_START).setPackage(packageName))
                    show(ExecutionFragment(), hideNav = true)
                    updateToolbarStatus("RUNNING")
                } else showPreflight(result.issues)
            }
        }
    }

    private fun showPreflight(issues: List<String>) {
        val body = buildString {
            append(getString(R.string.system_check)).append("\n\n")
            append(if (GrootAccessibilityService.INSTANCE != null) "✓ " else "✕ ").append(getString(R.string.accessibility_status)).append("\n")
            append(if (issues.none { it.contains("selected", true) }) "✓ " else "✕ ").append(getString(R.string.applications_selected, com.grootcleaning.data.DatabaseProvider.get(this@MainActivity).appSelectionDao().selected().size)).append("\n")
            if (issues.isNotEmpty()) {
                append("\n").append(getString(R.string.action_required)).append(":\n")
                issues.forEach { append("• ").append(it).append("\n") }
            }
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(if (issues.isEmpty()) getString(R.string.system_ready) else getString(R.string.action_required))
            .setMessage(body)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(getString(R.string.open_settings)) { _, _ -> openAccessibilitySettings() }
            .show()
    }

    fun stopAutomation() { sendBroadcast(Intent(GrootAccessibilityService.ACTION_STOP).setPackage(packageName)) }
    fun resumeAutomation() { sendBroadcast(Intent(GrootAccessibilityService.ACTION_RESUME).setPackage(packageName)); show(ExecutionFragment(), hideNav = true) }
    fun retryFailed() { sendBroadcast(Intent(GrootAccessibilityService.ACTION_RETRY_FAILED).setPackage(packageName)); show(ExecutionFragment(), hideNav = true) }
    fun skipCurrent() { sendBroadcast(Intent(GrootAccessibilityService.ACTION_SKIP).setPackage(packageName)) }
    fun openTest() { show(AutomationTestFragment(), hideNav = true) }
    fun setLanguage(tag: String) { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) }

    private fun maybeShowAccessibilityDisclosure() {
        if (AppPreferences.disclosureAccepted(this)) return
        val cb = android.widget.CheckBox(this).apply { text = getString(R.string.i_understand); setTextColor(ContextCompat.getColor(this@MainActivity, R.color.gc_text_primary)) }
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(Ui.dp(this@MainActivity, 18), Ui.dp(this@MainActivity, 10), Ui.dp(this@MainActivity, 18), 0) }
        box.addView(Ui.tv(this, getString(R.string.prominent_disclosure_body), 14f))
        box.addView(cb)
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.prominent_disclosure_title))
            .setView(box)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(getString(R.string.continue_to_settings)) { _, _ ->
                if (cb.isChecked) { AppPreferences.setDisclosureAccepted(this, true); openAccessibilitySettings() }
                else android.widget.Toast.makeText(this, getString(R.string.i_understand), android.widget.Toast.LENGTH_SHORT).show()
            }.show()
    }
}
