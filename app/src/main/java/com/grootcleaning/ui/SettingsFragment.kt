package com.grootcleaning.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.grootcleaning.R
import com.grootcleaning.data.SelectionRepository
import com.grootcleaning.models.AutomationSettings
import com.grootcleaning.models.VerificationLevel
import com.grootcleaning.service.accessibility.GrootAccessibilityService
import com.grootcleaning.utils.AppPreferences
import java.util.concurrent.Executors

class SettingsFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val c = requireContext()
        val root = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL; setPadding(Ui.dp(c, 16), Ui.dp(c, 8), Ui.dp(c, 16), Ui.dp(c, 22)) }
        root.addView(Ui.sectionTitle(c, getString(R.string.settings), getString(R.string.settings_caption)))

        root.addView(permissionCard(c))
        root.addView(advancedCard(c))
        root.addView(lifecycleCard(c))
        root.addView(languageCard(c))

        return ScrollView(c).apply { addView(root) }
    }

    private fun permissionCard(c: android.content.Context): View {
        val card = Ui.card(c, elevated = true)
        card.addView(Ui.tv(c, getString(R.string.permissions_title), 17f, true))
        card.addView(Ui.subtitle(c, getString(R.string.permissions_caption)))
        card.addView(Ui.divider(c))

        val accessibility = Ui.row(c)
        accessibility.addView(Ui.tv(c, getString(R.string.accessibility_status), 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
        accessibility.addView(Ui.statusDot(c, if (GrootAccessibilityService.INSTANCE != null) getString(R.string.accessibility_enabled) else getString(R.string.accessibility_disabled_label), androidx.core.content.ContextCompat.getColor(c, if (GrootAccessibilityService.INSTANCE != null) R.color.gc_success else R.color.gc_warning)))
        card.addView(accessibility)
        card.addView(Ui.subtitle(c, getString(R.string.accessibility_explanation)))
        val openA = Ui.button(c, getString(R.string.open_accessibility_settings), primary = true)
        card.addView(openA, LinearLayout.LayoutParams(-1, Ui.dp(c, 48)).apply { topMargin = Ui.dp(c, 10) })
        openA.setOnClickListener { showDisclosureOrOpen() }

        val account = Ui.row(c)
        account.addView(Ui.tv(c, getString(R.string.account_access), 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
        val accountOk = Build.VERSION.SDK_INT < 23 || androidx.core.content.ContextCompat.checkSelfPermission(c, Manifest.permission.GET_ACCOUNTS) == PackageManager.PERMISSION_GRANTED
        account.addView(Ui.statusDot(c, if (accountOk) getString(R.string.accessibility_enabled) else getString(R.string.accessibility_disabled_label), androidx.core.content.ContextCompat.getColor(c, if (accountOk) R.color.gc_success else R.color.gc_warning)))
        card.addView(Ui.divider(c)); card.addView(account)
        card.addView(Ui.subtitle(c, getString(R.string.account_access_explanation)))
        val openAccounts = Ui.button(c, getString(R.string.open_account_access))
        card.addView(openAccounts, LinearLayout.LayoutParams(-1, Ui.dp(c, 46)).apply { topMargin = Ui.dp(c, 8) })
        openAccounts.setOnClickListener { (activity as? MainActivity)?.show(AccountsFragment(), navId = R.id.nav_accounts) }

        val usage = Ui.row(c)
        usage.addView(Ui.tv(c, getString(R.string.usage_access), 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
        usage.addView(Ui.chip(c, getString(R.string.optional), androidx.core.content.ContextCompat.getColor(c, R.color.gc_surface_elevated)))
        card.addView(Ui.divider(c)); card.addView(usage)
        card.addView(Ui.subtitle(c, getString(R.string.usage_access_explanation)))
        val openUsage = Ui.button(c, getString(R.string.open_usage_access))
        card.addView(openUsage, LinearLayout.LayoutParams(-1, Ui.dp(c, 46)).apply { topMargin = Ui.dp(c, 8) })
        openUsage.setOnClickListener { (activity as? MainActivity)?.openUsageSettings() }

        val test = Ui.button(c, getString(R.string.test_automation))
        card.addView(test, LinearLayout.LayoutParams(-1, Ui.dp(c, 46)).apply { topMargin = Ui.dp(c, 8) })
        test.setOnClickListener { (activity as? MainActivity)?.openTest() }
        return card
    }

    private fun advancedCard(c: android.content.Context): View {
        val card = Ui.card(c)
        card.addView(Ui.tv(c, getString(R.string.advanced_settings), 17f, true))
        card.addView(Ui.subtitle(c, getString(R.string.advanced_settings_caption)))
        card.addView(Ui.divider(c))

        val timeout = numberField(c, getString(R.string.timeout_seconds))
        val retry = numberField(c, getString(R.string.retry_count))
        val delay = numberField(c, getString(R.string.delay_between_tasks))
        card.addView(timeout); card.addView(retry); card.addView(delay)

        val cont = toggle(c, getString(R.string.continue_after_failure))
        val auto = toggle(c, getString(R.string.auto_resume))
        val sim = toggle(c, getString(R.string.simulation_mode))
        val dbg = toggle(c, getString(R.string.debug_logging))
        val sys = toggle(c, getString(R.string.show_system_apps))
        listOf(cont, auto, sim, dbg, sys).forEach { card.addView(it) }

        val levelRow = Ui.row(c)
        levelRow.addView(Ui.tv(c, getString(R.string.verification_level), 14f, true), LinearLayout.LayoutParams(0, -2, 1f))
        val level = Spinner(c)
        level.adapter = ArrayAdapter(c, android.R.layout.simple_spinner_dropdown_item, listOf(getString(R.string.basic), getString(R.string.standard), getString(R.string.strict)))
        levelRow.addView(level, LinearLayout.LayoutParams(Ui.dp(c, 125), Ui.dp(c, 46)))
        card.addView(levelRow)

        val save = Ui.button(c, getString(R.string.save_settings), primary = true)
        card.addView(save, LinearLayout.LayoutParams(-1, Ui.dp(c, 50)).apply { topMargin = Ui.dp(c, 10) })
        save.setOnClickListener {
            val s = AutomationSettings(
                timeoutSeconds = (timeout.text.toString().toIntOrNull() ?: 12).coerceIn(5, 60),
                retryCount = (retry.text.toString().toIntOrNull() ?: 3).coerceIn(0, 5),
                delayBetweenTasksMs = (delay.text.toString().toLongOrNull() ?: 300L).coerceIn(0, 5000),
                continueAfterFailure = cont.isChecked,
                autoResume = auto.isChecked,
                simulationMode = sim.isChecked,
                debugLogging = dbg.isChecked,
                verificationLevel = VerificationLevel.entries[level.selectedItemPosition],
                includeSystemApps = sys.isChecked
            )
            executor.execute { SelectionRepository(c).saveSettings(s) }
            Toast.makeText(c, getString(R.string.save_settings), Toast.LENGTH_SHORT).show()
        }

        executor.execute {
            val s = SelectionRepository(c).loadSettings()
            requireActivity().runOnUiThread {
                timeout.setText(s.timeoutSeconds.toString()); retry.setText(s.retryCount.toString()); delay.setText(s.delayBetweenTasksMs.toString())
                cont.isChecked = s.continueAfterFailure; auto.isChecked = s.autoResume; sim.isChecked = s.simulationMode; dbg.isChecked = s.debugLogging; sys.isChecked = s.includeSystemApps; level.setSelection(s.verificationLevel.ordinal)
            }
        }
        return card
    }

    private fun lifecycleCard(c: android.content.Context): View {
        val card = Ui.card(c)
        card.addView(Ui.tv(c, getString(R.string.session_controls), 17f, true))
        card.addView(Ui.subtitle(c, getString(R.string.session_controls_caption)))
        val resume = Ui.button(c, getString(R.string.resume))
        val open = Ui.button(c, getString(R.string.open_manually))
        val row = Ui.row(c)
        row.addView(resume, LinearLayout.LayoutParams(0, Ui.dp(c, 46), 1f))
        row.addView(open, LinearLayout.LayoutParams(0, Ui.dp(c, 46), 1f).apply { marginStart = Ui.dp(c, 8) })
        card.addView(row)
        resume.setOnClickListener { (activity as? MainActivity)?.resumeAutomation() }
        open.setOnClickListener { startActivity(Intent(Settings.ACTION_SETTINGS)) }
        return card
    }

    private fun languageCard(c: android.content.Context): View {
        val card = Ui.card(c)
        card.addView(Ui.tv(c, getString(R.string.language), 17f, true))
        card.addView(Ui.subtitle(c, getString(R.string.language_caption)))
        val row = Ui.row(c)
        val en = Ui.button(c, "English")
        val ar = Ui.button(c, "العربية")
        row.addView(en, LinearLayout.LayoutParams(0, Ui.dp(c, 46), 1f))
        row.addView(ar, LinearLayout.LayoutParams(0, Ui.dp(c, 46), 1f).apply { marginStart = Ui.dp(c, 8) })
        card.addView(row)
        en.setOnClickListener { (activity as? MainActivity)?.setLanguage("en") }
        ar.setOnClickListener { (activity as? MainActivity)?.setLanguage("ar") }
        return card
    }

    private fun numberField(c: android.content.Context, hint: String): EditText = EditText(c).apply {
        setHint(hint); inputType = android.text.InputType.TYPE_CLASS_NUMBER; setTextColor(androidx.core.content.ContextCompat.getColor(c, R.color.gc_text_primary)); setHintTextColor(androidx.core.content.ContextCompat.getColor(c, R.color.gc_text_muted)); background = androidx.core.content.ContextCompat.getDrawable(c, R.drawable.bg_search); setPadding(Ui.dp(c, 12), 0, Ui.dp(c, 12), 0)
        layoutParams = LinearLayout.LayoutParams(-1, Ui.dp(c, 46)).apply { bottomMargin = Ui.dp(c, 7) }
    }

    private fun toggle(c: android.content.Context, label: String): Switch = Switch(c).apply {
        text = label; setTextColor(androidx.core.content.ContextCompat.getColor(c, R.color.gc_text_primary)); textSize = 13f
        layoutParams = LinearLayout.LayoutParams(-1, Ui.dp(c, 46))
    }

    private fun showDisclosureOrOpen() {
        if (AppPreferences.disclosureAccepted(requireContext())) (activity as? MainActivity)?.openAccessibilitySettings()
        else Toast.makeText(requireContext(), getString(R.string.prominent_disclosure_body), Toast.LENGTH_LONG).show()
    }
}
