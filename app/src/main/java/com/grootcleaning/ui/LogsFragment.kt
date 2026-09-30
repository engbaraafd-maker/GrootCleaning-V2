package com.grootcleaning.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grootcleaning.R
import com.grootcleaning.data.DatabaseProvider
import com.grootcleaning.data.RunEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class LogsFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var adapter: RunsAdapter
    private var allRuns: List<RunEntity> = emptyList()
    private var filter = "ALL"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 12)) }
        root.addView(Ui.sectionTitle(ctx, getString(R.string.activity_log), getString(R.string.log_filter_caption)))

        val filters = Ui.row(ctx)
        val values = listOf("ALL" to getString(R.string.all), "SUCCESS" to getString(R.string.success), "FAILED" to getString(R.string.failed), "USER" to getString(R.string.action_required))
        values.forEach { (key, label) ->
            val b = Ui.button(ctx, label)
            filters.addView(b, LinearLayout.LayoutParams(0, Ui.dp(ctx, 42), 1f).apply { marginEnd = Ui.dp(ctx, 5) })
            b.setOnClickListener { filter = key; adapter.submit(allRuns.filter(::matches)); styleFilterButtons(filters, key) }
        }
        root.addView(filters)

        val list = RecyclerView(ctx).apply { layoutManager = LinearLayoutManager(ctx); setHasFixedSize(true) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        adapter = RunsAdapter { showRun(it) }
        list.adapter = adapter
        load()
        return root
    }

    private fun styleFilterButtons(row: LinearLayout, active: String) {
        val keys = listOf("ALL", "SUCCESS", "FAILED", "USER")
        keys.forEachIndexed { i, key ->
            (row.getChildAt(i) as Button).background = androidx.core.content.ContextCompat.getDrawable(requireContext(), if (key == active) R.drawable.bg_primary_button else R.drawable.bg_secondary_button)
        }
    }

    private fun matches(run: RunEntity): Boolean = when (filter) {
        "SUCCESS" -> run.status == com.grootcleaning.models.RunStatus.COMPLETED && run.failed == 0 && run.userAction == 0
        "FAILED" -> run.status == com.grootcleaning.models.RunStatus.FAILED || run.failed > 0
        "USER" -> run.userAction > 0
        else -> true
    }

    private fun load() {
        executor.execute {
            allRuns = DatabaseProvider.get(requireContext()).runDao().recent(100)
            requireActivity().runOnUiThread {
                adapter.submit(allRuns)
                view?.post { val r = view as? ViewGroup; if (r != null && r.childCount > 1) { val row = (r.getChildAt(1) as? ViewGroup); if (row != null) styleFilterButtons(row, filter) } }
            }
        }
    }

    private fun showRun(run: RunEntity) {
        executor.execute {
            val db = DatabaseProvider.get(requireContext())
            val tasks = db.taskLogDao().byRun(run.id)
            val text = buildString {
                append(getString(R.string.status_label)).append(": ").append(run.status.name).append('\n')
                append(getString(R.string.successful_count, run.successful)).append('\n')
                append(getString(R.string.failed_count, run.failed)).append('\n')
                append(getString(R.string.user_action_count, run.userAction)).append('\n')
                append(getString(R.string.skipped_count, run.skipped)).append('\n\n')
                tasks.forEach { t ->
                    append("• ").append(t.appName ?: t.accountName ?: "Unknown").append(" — ").append(t.status).append('\n')
                    db.stepLogDao().byTask(t.id).forEach { s ->
                        val icon = when (s.status) {
                            com.grootcleaning.models.StepStatus.SUCCESS -> "✓"
                            com.grootcleaning.models.StepStatus.FAILED -> "×"
                            com.grootcleaning.models.StepStatus.REQUIRES_USER_ACTION -> "!"
                            else -> "•"
                        }
                        append("    $icon ${s.name}")
                        if (!s.detail.isNullOrBlank()) append(" — ").append(s.detail)
                        append('\n')
                    }
                }
            }
            requireActivity().runOnUiThread {
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.activity_log))
                    .setMessage(text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }
    }
}

class RunsAdapter(private val onClick: (RunEntity) -> Unit) : RecyclerView.Adapter<RunsAdapter.VH>() {
    private val rows = mutableListOf<RunEntity>()
    private val df = SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault())

    fun submit(items: List<RunEntity>) { rows.clear(); rows.addAll(items); notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val card = Ui.card(ctx)
        val top = Ui.row(ctx)
        val title = Ui.tv(ctx, "", 15f, true)
        val status = Ui.chip(ctx, "", androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_surface_elevated))
        top.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(status)
        card.addView(top)
        val meta = Ui.tv(ctx, "", 11f).apply { setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_text_secondary)); setPadding(0, 5, 0, 0) }
        card.addView(meta)
        return VH(card, title, status, meta)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        h.title.text = df.format(Date(r.startedAt))
        val (label, color) = when (r.status) {
            com.grootcleaning.models.RunStatus.COMPLETED -> getStringSafe(h.itemView, R.string.success) to R.color.gc_success_dark
            com.grootcleaning.models.RunStatus.COMPLETED_WITH_ERRORS -> getStringSafe(h.itemView, R.string.partial) to R.color.gc_warning_dark
            com.grootcleaning.models.RunStatus.FAILED -> getStringSafe(h.itemView, R.string.failed) to R.color.gc_error_dark
            else -> r.status.name to R.color.gc_surface_elevated
        }
        h.status.text = label
        h.status.background = androidx.core.content.ContextCompat.getDrawable(h.itemView.context, if (r.status == com.grootcleaning.models.RunStatus.COMPLETED) R.drawable.bg_success_chip else if (r.status == com.grootcleaning.models.RunStatus.FAILED) R.drawable.bg_error_chip else R.drawable.bg_secondary_button)
        h.meta.text = "${r.successful}✓   ${r.failed}×   ${r.userAction}!   ${r.skipped}–   •   ${r.totalTasks} total"
        h.itemView.setOnClickListener { onClick(r) }
    }

    class VH(root: View, val title: TextView, val status: TextView, val meta: TextView) : RecyclerView.ViewHolder(root)

    private fun getStringSafe(view: View, id: Int): String = view.context.getString(id)
}
