package com.grootcleaning.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.grootcleaning.R

object Ui {
    private fun rounded(context: Context, fill: Int, stroke: Int? = null, radiusDp: Float = 14f): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusDp * density
            setColor(fill)
            if (stroke != null) setStroke((1 * density).toInt().coerceAtLeast(1), stroke)
        }
    }

    fun tv(context: Context, text: String, size: Float = 14f, bold: Boolean = false): TextView = TextView(context).apply {
        this.text = text
        textSize = size
        setTextColor(ContextCompat.getColor(context, R.color.gc_text_primary))
        if (bold) setTypeface(typeface, Typeface.BOLD)
        includeFontPadding = false
    }

    fun subtitle(context: Context, text: String): TextView = tv(context, text, 12f).apply {
        setTextColor(ContextCompat.getColor(context, R.color.gc_text_secondary))
        setPadding(0, 4, 0, 0)
    }

    fun card(context: Context, elevated: Boolean = false): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = ContextCompat.getDrawable(context, if (elevated) R.drawable.bg_card_elevated else R.drawable.bg_card)
        setPadding(dp(context, 16))
        layoutParams = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(context, 12)
        }
    }

    fun sectionTitle(context: Context, title: String, caption: String? = null): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 8, 0, 10)
        addView(tv(context, title, 17f, true))
        if (!caption.isNullOrBlank()) addView(subtitle(context, caption))
    }

    fun button(context: Context, text: String, primary: Boolean = false): Button = Button(context).apply {
        this.text = text
        textSize = 13f
        setTextColor(Color.WHITE)
        background = ContextCompat.getDrawable(context, if (primary) R.drawable.bg_primary_button else R.drawable.bg_secondary_button)
        isAllCaps = false
        minHeight = dp(context, 48)
        minimumHeight = dp(context, 48)
        stateListAnimator = null
        includeFontPadding = false
        setPadding(dp(context, 12), 0, dp(context, 12), 0)
    }

    fun chip(context: Context, text: String, fill: Int, textColor: Int = Color.WHITE): TextView = TextView(context).apply {
        this.text = text
        textSize = 11f
        setTextColor(textColor)
        gravity = Gravity.CENTER
        background = rounded(context, fill, null, 20f)
        setPadding(dp(context, 10), dp(context, 6), dp(context, 10), dp(context, 6))
    }

    fun metric(context: Context, label: String, value: String): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(context, ContextCompat.getColor(context, R.color.gc_surface_elevated), ContextCompat.getColor(context, R.color.gc_navy_border), 12f)
        setPadding(dp(context, 12))
        addView(tv(context, label, 11f).apply { setTextColor(ContextCompat.getColor(context, R.color.gc_text_secondary)) })
        addView(tv(context, value, 19f, true).apply { setPadding(0, dp(context, 5), 0, 0) })
    }

    fun divider(context: Context): View = View(context).apply {
        setBackgroundColor(ContextCompat.getColor(context, R.color.gc_divider))
        layoutParams = LinearLayout.LayoutParams(-1, dp(context, 1)).apply {
            topMargin = dp(context, 8)
            bottomMargin = dp(context, 8)
        }
    }

    fun row(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(0, dp(context, 6), 0, dp(context, 6))
    }

    fun statusDot(context: Context, status: String, color: Int): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(View(context).apply {
            background = rounded(context, color, null, 50f)
            layoutParams = LinearLayout.LayoutParams(dp(context, 8), dp(context, 8)).apply { marginEnd = dp(context, 7) }
        })
        addView(tv(context, status, 12f, true).apply { setTextColor(color) })
    }

    fun outlined(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(context, ContextCompat.getColor(context, R.color.gc_background_secondary), ContextCompat.getColor(context, R.color.gc_navy_border), 12f)
    }

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).roundToIntSafe()

    private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()
}
