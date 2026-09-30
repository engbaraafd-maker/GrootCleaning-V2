package com.grootcleaning.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.grootcleaning.R
import com.grootcleaning.data.AccountCatalog
import com.grootcleaning.data.AccountSelectionEntity
import com.grootcleaning.data.DatabaseProvider
import java.util.concurrent.Executors

class AccountsFragment : Fragment() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var adapter: AccountAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val root = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(Ui.dp(ctx, 16), Ui.dp(ctx, 8), Ui.dp(ctx, 16), Ui.dp(ctx, 18)) }
        root.addView(Ui.sectionTitle(ctx, getString(R.string.accounts_title), getString(R.string.account_screen_caption)))

        val warning = Ui.card(ctx)
        warning.addView(Ui.tv(ctx, getString(R.string.account_warning), 12f).apply { setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.gc_warning)) })
        root.addView(warning)

        val grant = Ui.button(ctx, getString(R.string.grant_account_access))
        root.addView(grant, LinearLayout.LayoutParams(-1, Ui.dp(ctx, 48)).apply { bottomMargin = Ui.dp(ctx, 8) })
        grant.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) requestPermissions(arrayOf(Manifest.permission.GET_ACCOUNTS), REQUEST_ACCOUNTS)
            else loadAccounts(ctx)
        }

        val list = RecyclerView(ctx).apply { layoutManager = LinearLayoutManager(ctx); setHasFixedSize(true) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        adapter = AccountAdapter(ctx) { row ->
            executor.execute {
                DatabaseProvider.get(ctx).accountSelectionDao().upsert(
                    AccountSelectionEntity(row.id, row.name, row.type, row.selected, System.currentTimeMillis())
                )
            }
        }
        list.adapter = adapter
        loadAccounts(ctx)
        return root
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_ACCOUNTS) loadAccounts(requireContext())
    }

    private fun loadAccounts(ctx: android.content.Context) {
        executor.execute {
            val saved = DatabaseProvider.get(ctx).accountSelectionDao().all().associateBy { it.id }
            val rows = AccountCatalog(ctx).load().map { a -> AccountRow(a.id, a.name, a.type, saved[a.id]?.selected ?: false) }
            requireActivity().runOnUiThread {
                adapter.submit(rows)
            }
        }
    }

    companion object { private const val REQUEST_ACCOUNTS = 4101 }
}

data class AccountRow(val id: String, val name: String, val type: String, var selected: Boolean)

class AccountAdapter(private val context: android.content.Context, private val onChanged: (AccountRow) -> Unit) : RecyclerView.Adapter<AccountAdapter.VH>() {
    private val rows = mutableListOf<AccountRow>()

    fun submit(items: List<AccountRow>) { rows.clear(); rows.addAll(items); notifyDataSetChanged() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val c = parent.context
        val card = Ui.card(c)
        val row = Ui.row(c)
        val icon = Ui.tv(c, "@", 18f, true).apply {
            gravity = android.view.Gravity.CENTER
            background = androidx.core.content.ContextCompat.getDrawable(c, R.drawable.bg_icon_surface)
        }
        row.addView(icon, LinearLayout.LayoutParams(Ui.dp(c, 42), Ui.dp(c, 42)))
        val col = LinearLayout(c).apply { orientation = LinearLayout.VERTICAL }
        val name = Ui.tv(c, "", 14f, true)
        val type = Ui.subtitle(c, "")
        col.addView(name); col.addView(type)
        row.addView(col, LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = Ui.dp(c, 10) })
        val cb = CheckBox(c)
        row.addView(cb)
        card.addView(row)
        val remove = Ui.tv(c, c.getString(R.string.remove_account), 11f).apply { setTextColor(androidx.core.content.ContextCompat.getColor(c, R.color.gc_text_muted)); setPadding(0, Ui.dp(c, 8), 0, 0) }
        card.addView(remove)
        return VH(card, name, type, cb, remove, icon)
    }

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, position: Int) {
        val r = rows[position]
        h.name.text = r.name
        h.type.text = r.type
        h.icon.text = r.type.firstOrNull()?.uppercase() ?: "@"
        h.check.setOnCheckedChangeListener(null)
        h.check.isChecked = r.selected
        h.check.setOnCheckedChangeListener { _, value -> r.selected = value; onChanged(r) }
        h.itemView.setOnClickListener { h.check.isChecked = !h.check.isChecked }
    }

    class VH(root: View, val name: TextView, val type: TextView, val check: CheckBox, val remove: TextView, val icon: TextView) : RecyclerView.ViewHolder(root)
}
