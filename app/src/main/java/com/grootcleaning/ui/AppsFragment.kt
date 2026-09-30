package com.grootcleaning.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grootcleaning.R
import com.grootcleaning.data.AppCatalog
import com.grootcleaning.data.AppSelectionEntity
import com.grootcleaning.data.DatabaseProvider
import com.grootcleaning.data.InstalledApp
import com.grootcleaning.data.SelectionRepository
import com.grootcleaning.models.Operation
import java.util.concurrent.Executors

class AppsFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var adapter: AppAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 14)) }
        root.addView(Ui.sectionTitle(ctx, getString(R.string.applications), getString(R.string.apps_screen_caption)))

        val search = EditText(ctx).apply {
            hint = getString(R.string.search)
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_text_primary))
            setHintTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_text_muted))
            setSingleLine(true)
            setPadding(Ui.dp(ctx, 14), 0, Ui.dp(ctx, 14), 0)
            background = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_search)
        }
        root.addView(search, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 48)).apply { bottomMargin = Ui.dp(ctx, 8) })

        val actions = Ui.row(ctx)
        val selectAll = Ui.button(ctx, getString(R.string.select_all))
        val clearAll = Ui.button(ctx, getString(R.string.unselect_all))
        actions.addView(selectAll, LinearLayout.LayoutParams(0, Ui.dp(ctx, 44), 1f))
        actions.addView(clearAll, LinearLayout.LayoutParams(0, Ui.dp(ctx, 44), 1f).apply { marginStart = Ui.dp(ctx, 8) })
        root.addView(actions)

        val selectedLabel = Ui.tv(ctx, getString(R.string.app_selection_count, 0), 11f, true).apply {
            setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_success))
            setPadding(0, Ui.dp(ctx, 6), 0, Ui.dp(ctx, 8))
        }
        root.addView(selectedLabel)

        val list = RecyclerView(ctx).apply { layoutManager = LinearLayoutManager(ctx); setHasFixedSize(true) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))

        adapter = AppAdapter(ctx) { item ->
            executor.execute {
                DatabaseProvider.get(ctx).appSelectionDao().upsert(
                    AppSelectionEntity(item.packageName, item.appName, item.operation, item.selected, item.isSystemApp, System.currentTimeMillis())
                )
                requireActivity().runOnUiThread { selectedLabel.text = getString(R.string.app_selection_count, adapter.selectedCount()) }
            }
        }
        list.adapter = adapter

        executor.execute {
            val repo = SelectionRepository(ctx)
            val saved = repo.apps.all().associateBy { it.packageName }
            val items = AppCatalog(ctx).load(repo.loadSettings().includeSystemApps).map { app ->
                val old = saved[app.packageName]
                AppRow(app, old?.operation ?: Operation.CLEAR_DATA_AND_CACHE, old?.selected ?: false)
            }
            requireActivity().runOnUiThread {
                adapter.submit(items)
                selectedLabel.text = getString(R.string.app_selection_count, adapter.selectedCount())
            }
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = adapter.filter(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })

        selectAll.setOnClickListener {
            executor.execute {
                val dao = DatabaseProvider.get(ctx).appSelectionDao()
                adapter.currentItems().forEach { r -> dao.upsert(AppSelectionEntity(r.packageName, r.appName, r.operation, true, r.isSystemApp, System.currentTimeMillis())) }
                requireActivity().runOnUiThread { adapter.setAllSelected(true); selectedLabel.text = getString(R.string.app_selection_count, adapter.selectedCount()) }
            }
        }
        clearAll.setOnClickListener {
            executor.execute {
                val dao = DatabaseProvider.get(ctx).appSelectionDao()
                adapter.currentItems().forEach { r -> dao.upsert(AppSelectionEntity(r.packageName, r.appName, r.operation, false, r.isSystemApp, System.currentTimeMillis())) }
                requireActivity().runOnUiThread { adapter.setAllSelected(false); selectedLabel.text = getString(R.string.app_selection_count, adapter.selectedCount()) }
            }
        }

        return root
    }
}

data class AppRow(val app: InstalledApp, var operation: Operation, var selected: Boolean) {
    val packageName get() = app.packageName
    val appName get() = app.appName
    val isSystemApp get() = app.isSystemApp
    val info get() = app.info
}

class AppAdapter(private val context: android.content.Context, private val onChanged: (AppRow) -> Unit) : RecyclerView.Adapter<AppAdapter.VH>() {
    private val all = mutableListOf<AppRow>()
    private val visible = mutableListOf<AppRow>()
    private var query = ""

    fun submit(items: List<AppRow>) { all.clear(); all.addAll(items); filter(query) }
    fun filter(q: String) { query = q; visible.clear(); visible.addAll(all.filter { it.appName.contains(q, true) || it.packageName.contains(q, true) }); notifyDataSetChanged() }
    fun currentItems(): List<AppRow> = visible.toList()
    fun selectedCount() = all.count { it.selected }
    fun setAllSelected(value: Boolean) { visible.forEach { it.selected = value }; notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val ctx = parent.context
        val card = Ui.card(ctx)
        val row = Ui.row(ctx)
        val icon = ImageView(ctx)
        row.addView(icon, LinearLayout.LayoutParams(Ui.dp(ctx, 46), Ui.dp(ctx, 46)))
        val textColumn = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val name = Ui.tv(ctx, "", 14f, true)
        val pkg = Ui.tv(ctx, "", 10f).apply { setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_text_secondary)) }
        textColumn.addView(name); textColumn.addView(pkg)
        row.addView(textColumn, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(ctx, 10) })
        val check = CheckBox(ctx)
        row.addView(check)
        card.addView(row)

        val operationLabel = Ui.tv(ctx, ctx.getString(R.string.operation), 10f, true).apply { setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_text_secondary)); setPadding(0, Ui.dp(ctx, 8), 0, Ui.dp(ctx, 3)) }
        card.addView(operationLabel)
        val spinner = Spinner(ctx).apply {
            background = androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_spinner)
            adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, listOf(ctx.getString(R.string.clear_cache), ctx.getString(R.string.clear_data), ctx.getString(R.string.clear_data_cache)))
        }
        card.addView(spinner, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 46)))
        return VH(card, icon, name, pkg, check, spinner)
    }

    override fun getItemCount() = visible.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = visible[position]
        holder.icon.setImageDrawable(item.info.loadIcon(context.packageManager))
        holder.name.text = item.appName
        holder.pkg.text = item.packageName + if (item.isSystemApp) " • ${context.getString(R.string.system_app)}" else ""
        holder.check.setOnCheckedChangeListener(null)
        holder.check.isChecked = item.selected
        holder.check.setOnCheckedChangeListener { _, checked -> item.selected = checked; onChanged(item) }
        holder.spinner.onItemSelectedListener = null
        holder.spinner.setSelection(item.operation.ordinal, false)
        holder.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val op = Operation.entries[position]
                if (op != item.operation) { item.operation = op; onChanged(item) }
            }
        }
        holder.itemView.setOnClickListener { holder.check.isChecked = !holder.check.isChecked }
    }

    class VH(root: View, val icon: ImageView, val name: TextView, val pkg: TextView, val check: CheckBox, val spinner: Spinner) : RecyclerView.ViewHolder(root)
}
