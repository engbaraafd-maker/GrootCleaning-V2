package com.grootcleaning.ui

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.grootcleaning.R
import com.grootcleaning.data.DatabaseProvider
import com.grootcleaning.data.SelectionRepository
import com.grootcleaning.models.RunStatus
import com.grootcleaning.service.accessibility.GrootAccessibilityService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class HomeFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 24))
        }

        val hero = Ui.card(ctx, elevated = true)
        root.addView(hero)
        val brand = Ui.row(ctx)
        brand.addView(android.widget.ImageView(ctx).apply {
            setImageResource(R.drawable.ic_groot_logo)
            layoutParams = LinearLayout.LayoutParams(Ui.dp(ctx, 48), Ui.dp(ctx, 48))
        })
        val titleCol = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        titleCol.addView(Ui.tv(ctx, getString(R.string.app_name), 22f, true))
        titleCol.addView(Ui.subtitle(ctx, getString(R.string.app_description)))
        brand.addView(titleCol, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 12) })
        hero.addView(brand)

        val statusRow = Ui.row(ctx).apply { setPadding(0, Ui.dp(ctx, 14), 0, Ui.dp(ctx, 8)) }
        val statusText = if (GrootAccessibilityService.INSTANCE != null) getString(R.string.ready).uppercase() else getString(R.string.accessibility_disabled).uppercase()
        val statusColor = ContextCompatCompat.color(ctx, if (GrootAccessibilityService.INSTANCE != null) R.color.gc_success else R.color.gc_warning)
        statusRow.addView(Ui.statusDot(ctx, statusText, statusColor))
        statusRow.addView(android.view.View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        hero.addView(statusRow)

        val metrics = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val row1 = Ui.row(ctx)
        val appsValue = Ui.metric(ctx, getString(R.string.applications), "0")
        val accountsValue = Ui.metric(ctx, getString(R.string.accounts), "0")
        row1.addView(appsValue, LinearLayout.LayoutParams(0, -2, 1f))
        row1.addView(accountsValue, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 8) })
        val row2 = Ui.row(ctx)
        val lastRunValue = Ui.metric(ctx, getString(R.string.last_run), "—")
        val resultValue = Ui.metric(ctx, getString(R.string.last_result), "—")
        row2.addView(lastRunValue, LinearLayout.LayoutParams(0, -2, 1f))
        row2.addView(resultValue, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 8) })
        metrics.addView(row1); metrics.addView(row2)
        hero.addView(metrics)

        val start = Ui.button(ctx, getString(R.string.start_cleaning), primary = true)
        hero.addView(start, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 54)).apply { topMargin = Ui.dp(ctx, 12) })
        start.setOnClickListener { (activity as? MainActivity)?.startAutomation() }

        root.addView(Ui.sectionTitle(ctx, getString(R.string.quick_actions), getString(R.string.quick_actions_caption)), LinearLayout.LayoutParams(-1, -2))
        val actions = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val a1 = Ui.row(ctx)
        val apps = Ui.button(ctx, getString(R.string.apps))
        val accounts = Ui.button(ctx, getString(R.string.accounts))
        a1.addView(apps, LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f)); a1.addView(accounts, LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f).apply { marginStart = Ui.dp(ctx, 8) })
        val a2 = Ui.row(ctx)
        val permissions = Ui.button(ctx, getString(R.string.permissions))
        val test = Ui.button(ctx, getString(R.string.test_automation))
        a2.addView(permissions, LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f)); a2.addView(test, LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f).apply { marginStart = Ui.dp(ctx, 8) })
        actions.addView(a1); actions.addView(a2)
        root.addView(actions)
        apps.setOnClickListener { (activity as? MainActivity)?.show(AppsFragment(), navId = R.id.nav_apps) }
        accounts.setOnClickListener { (activity as? MainActivity)?.show(AccountsFragment(), navId = R.id.nav_accounts) }
        permissions.setOnClickListener { (activity as? MainActivity)?.show(SettingsFragment(), navId = R.id.nav_settings) }
        test.setOnClickListener { (activity as? MainActivity)?.openTest() }

        root.addView(Ui.sectionTitle(ctx, getString(R.string.system_check), getString(R.string.system_check_caption)))
        val checkCard = Ui.card(ctx)
        checkCard.addView(checkRow(ctx, getString(R.string.accessibility_status), GrootAccessibilityService.INSTANCE != null, getString(R.string.accessibility_explanation)))
        checkCard.addView(Ui.divider(ctx))
        checkCard.addView(checkRow(ctx, getString(R.string.applications), false, getString(R.string.select_apps_before_run)))
        checkCard.addView(Ui.divider(ctx))
        checkCard.addView(checkRow(ctx, getString(R.string.automation_engine), true, getString(R.string.engine_ready)))
        root.addView(checkCard)

        executor.execute {
            val repo = SelectionRepository(ctx)
            val db = DatabaseProvider.get(ctx)
            val appCount = repo.apps.selected().size
            val accountCount = repo.accounts.selected().size
            val run = db.runDao().recent(1).firstOrNull()
            requireActivity().runOnUiThread {
                updateMetric(appsValue, appCount.toString())
                updateMetric(accountsValue, accountCount.toString())
                if (run != null) {
                    updateMetric(lastRunValue, SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(run.startedAt)))
                    val result = when (run.status) {
                        RunStatus.COMPLETED -> getString(R.string.success)
                        RunStatus.COMPLETED_WITH_ERRORS -> getString(R.string.partial)
                        RunStatus.FAILED -> getString(R.string.failed)
                        RunStatus.STOPPED -> getString(R.string.task_stopped)
                        else -> run.status.name
                    }
                    updateMetric(resultValue, result)
                }
            }
        }

        return android.widget.ScrollView(ctx).apply { addView(root) }
    }

    private fun checkRow(context: android.content.Context, title: String, ok: Boolean, detail: String): LinearLayout {
        val row = Ui.row(context)
        val icon = Ui.tv(context, if (ok) "✓" else "!", 17f, true).apply {
            setTextColor(ContextCompatCompat.color(context, if (ok) R.color.gc_success else R.color.gc_warning))
        }
        row.addView(icon, LinearLayout.LayoutParams(Ui.dp(context, 28), -2))
        val col = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        col.addView(Ui.tv(context, title, 14f, true))
        col.addView(Ui.subtitle(context, detail))
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(Ui.chip(context, if (ok) getString(R.string.detected) else getString(R.string.action_required), ContextCompatCompat.color(context, if (ok) R.color.gc_success_dark else R.color.gc_warning_dark)))
        return row
    }

    private fun updateMetric(metric: LinearLayout, value: String) {
        (metric.getChildAt(1) as? android.widget.TextView)?.text = value
    }
}

private object ContextCompatCompat {
    fun color(context: android.content.Context, id: Int): Int = androidx.core.content.ContextCompat.getColor(context, id)
}
