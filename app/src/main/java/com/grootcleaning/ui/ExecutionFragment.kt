package com.grootcleaning.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.grootcleaning.R
import com.grootcleaning.data.DatabaseProvider
import com.grootcleaning.data.StepLogEntity
import com.grootcleaning.data.TaskLogEntity
import com.grootcleaning.models.RunStatus
import com.grootcleaning.models.StepStatus
import com.grootcleaning.models.TaskStatus
import com.grootcleaning.service.accessibility.GrootAccessibilityService
import com.grootcleaning.utils.AppPreferences
import java.util.concurrent.Executors

class ExecutionFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private var root: LinearLayout? = null
    private var current: TextView? = null
    private var operation: TextView? = null
    private var progress: ProgressBar? = null
    private var progressLabel: TextView? = null
    private var timeline: LinearLayout? = null
    private var actionRow: LinearLayout? = null
    private var stop: Button? = null
    private val poller = object : Runnable {
        override fun run() {
            refreshFromBroadcast(null)
            if (isAdded) handler.postDelayed(this, 700)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 10), Ui.dp(ctx, 16), Ui.dp(ctx, 22))
        }

        root!!.addView(Ui.sectionTitle(ctx, getString(R.string.cleaning_progress), getString(R.string.result_summary)))
        val hero = Ui.card(ctx, elevated = true)
        root!!.addView(hero)
        current = Ui.tv(ctx, "—", 21f, true)
        hero.addView(current)
        operation = Ui.subtitle(ctx, "")
        hero.addView(operation)
        progressLabel = Ui.tv(ctx, "0 / 0", 11f, true).apply { setGravity(Gravity.END) }
        hero.addView(progressLabel)
        progress = ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100 }
        hero.addView(progress, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 8)).apply { topMargin = Ui.dp(ctx, 5) })

        root!!.addView(Ui.sectionTitle(ctx, getString(R.string.step_timeline)))
        timeline = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root!!.addView(timeline, LinearLayout.LayoutParams(-1, -2))

        actionRow = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        root!!.addView(actionRow)
        return ScrollView(ctx).apply { addView(root) }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(poller)
        handler.post(poller)
    }

    override fun onPause() {
        handler.removeCallbacks(poller)
        super.onPause()
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

    fun refreshFromBroadcast(message: String?) {
        if (!isAdded) return
        executor.execute {
            val ctx = requireContext()
            val runId = AppPreferences.lastRunId(ctx)
            if (runId == 0L) {
                postEmpty(ctx)
                return@execute
            }
            val db = DatabaseProvider.get(ctx)
            val run = db.runDao().byId(runId)
            val tasks = db.taskLogDao().byRun(runId)
            val active = tasks.lastOrNull { it.status == TaskStatus.RUNNING }
                ?: tasks.lastOrNull { it.status == TaskStatus.REQUIRES_USER_ACTION }
                ?: tasks.lastOrNull()
            val steps = active?.let { db.stepLogDao().byTask(it.id) }.orEmpty()
            requireActivity().runOnUiThread {
                renderRun(run, active, steps, message)
            }
        }
    }

    private fun renderRun(run: com.grootcleaning.data.RunEntity?, task: TaskLogEntity?, steps: List<StepLogEntity>, message: String?) {
        val ctx = requireContext()
        if (run == null) return postEmpty(ctx)

        val completed = run.successful + run.failed + run.userAction + run.skipped
        val total = run.totalTasks.coerceAtLeast(1)
        val percent = (completed * 100 / total).coerceIn(0, 100)
        progress?.progress = percent
        progressLabel?.text = "$completed / ${run.totalTasks}  •  $percent%"
        current?.text = task?.appName ?: task?.accountName ?: getString(R.string.waiting)
        operation?.text = buildString {
            task?.operation?.let { append(it.name.replace('_', ' ')) }
            if (message != null && message.isNotBlank()) {
                if (isNotEmpty()) append("  •  ")
                append(message.substringBefore(':'))
            }
        }
        renderTimeline(ctx, steps)
        renderActions(ctx, run.status)
    }

    private fun renderTimeline(ctx: android.content.Context, steps: List<StepLogEntity>) {
        val container = timeline ?: return
        container.removeAllViews()
        if (steps.isEmpty()) {
            container.addView(Ui.card(ctx).apply {
                addView(Ui.tv(ctx, getString(R.string.waiting), 13f))
            })
            return
        }
        steps.forEach { step ->
            val card = Ui.card(ctx)
            card.setPadding(Ui.dp(ctx, 12))
            val row = Ui.row(ctx)
            val (icon, color, label) = when (step.status) {
                StepStatus.SUCCESS -> Triple("✓", R.color.gc_success, getString(R.string.completed))
                StepStatus.RUNNING -> Triple("⟳", R.color.gc_primary, getString(R.string.in_progress))
                StepStatus.FAILED -> Triple("×", R.color.gc_error, getString(R.string.failed))
                StepStatus.REQUIRES_USER_ACTION -> Triple("!", R.color.gc_warning, getString(R.string.action_required))
                StepStatus.SKIPPED -> Triple("–", R.color.gc_text_secondary, getString(R.string.skipped_count, 1))
                StepStatus.PENDING -> Triple("○", R.color.gc_text_secondary, getString(R.string.waiting))
            }
            row.addView(Ui.tv(ctx, icon, 17f, true).apply { setTextColor(androidx.core.content.ContextCompat.getColor(ctx, color)) }, LinearLayout.LayoutParams(Ui.dp(ctx, 28), -2))
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            col.addView(Ui.tv(ctx, step.name, 13f, true))
            val detail = buildString {
                append(label)
                step.detail?.let { append(" • ").append(it) }
            }
            col.addView(Ui.subtitle(ctx, detail))
            row.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
            card.addView(row)
            container.addView(card)
        }
    }

    private fun renderActions(ctx: android.content.Context, status: RunStatus) {
        val container = actionRow ?: return
        container.removeAllViews()
        when (status) {
            RunStatus.RUNNING, RunStatus.PENDING -> {
                stop = Ui.button(ctx, getString(R.string.stop), primary = true)
                stop!!.setOnClickListener { (activity as? MainActivity)?.stopAutomation() }
                container.addView(stop, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 52)).apply { topMargin = Ui.dp(ctx, 10) })
            }
            RunStatus.COMPLETED, RunStatus.COMPLETED_WITH_ERRORS, RunStatus.FAILED, RunStatus.STOPPED -> {
                val card = Ui.card(ctx, elevated = true)
                val title = Ui.tv(ctx, if (status == RunStatus.COMPLETED) getString(R.string.cleaning_complete) else getString(R.string.completed_with_errors), 20f, true)
                card.addView(title)
                card.addView(Ui.subtitle(ctx, getString(R.string.result_summary)))
                val db = DatabaseProvider.get(ctx)
                val runId = AppPreferences.lastRunId(ctx)
                val run = db.runDao().byId(runId)
                if (run != null) {
                    val stats = Ui.row(ctx)
                    stats.addView(Ui.metric(ctx, getString(R.string.successful), run.successful.toString()), LinearLayout.LayoutParams(0, -2, 1f))
                    stats.addView(Ui.metric(ctx, getString(R.string.failed), run.failed.toString()), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 6) })
                    stats.addView(Ui.metric(ctx, getString(R.string.user_action), run.userAction.toString()), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 6) })
                    card.addView(stats)
                }
                container.addView(card)
                val details = Ui.button(ctx, getString(R.string.view_details), primary = true)
                details.setOnClickListener { (activity as? MainActivity)?.show(LogsFragment(), navId = R.id.nav_logs) }
                container.addView(details, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 50)).apply { topMargin = Ui.dp(ctx, 8) })
                val actions = Ui.row(ctx)
                val retry = Ui.button(ctx, getString(R.string.retry_failed))
                val home = Ui.button(ctx, getString(R.string.back_home))
                actions.addView(retry, LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f))
                actions.addView(home, LinearLayout.LayoutParams(0, Ui.dp(ctx, 48), 1f).apply { marginStart = Ui.dp(ctx, 8) })
                container.addView(actions)
                retry.setOnClickListener { (activity as? MainActivity)?.retryFailed() }
                home.setOnClickListener { (activity as? MainActivity)?.show(HomeFragment(), navId = R.id.nav_home) }
            }
        }
    }

    private fun postEmpty(ctx: android.content.Context) {
        requireActivity().runOnUiThread {
            current?.text = getString(R.string.waiting)
            operation?.text = getString(R.string.no_selected_targets)
        }
    }

    private fun openCurrentManually() {
        executor.execute {
            val id = AppPreferences.lastRunId(requireContext())
            val task = if (id != 0L) DatabaseProvider.get(requireContext()).taskLogDao().byRun(id).lastOrNull() else null
            requireActivity().runOnUiThread {
                when {
                    task?.packageName != null -> startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${task.packageName}")))
                    task?.accountName != null -> startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
                    else -> Toast.makeText(requireContext(), getString(R.string.no_selected_targets), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
