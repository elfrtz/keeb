package com.elfrtz.keeb.keyboard

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import com.elfrtz.keeb.R
import com.google.android.material.button.MaterialButton

/**
 * Generates a simple QWERTY keyboard with a bottom action row.
 * Stateless utility — all state lives in KeebInputMethodService.
 */
object KeyboardView {

    private val ROWS = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    fun populate(
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        container.removeAllViews()
        val ctx = container.context

        // Letter rows
        for ((index, chars) in ROWS.withIndex()) {
            val row = makeRow(ctx)

            // Indent middle rows slightly for staggered look
            if (index == 1) addSpacer(row, 18)
            if (index == 2) addSpacer(row, 36)

            for (ch in chars) {
                row.addView(makeKey(ctx, ch.uppercase(), onKey))
            }

            // Backspace on last row
            if (index == 2) {
                addSpacer(row, 8)
                row.addView(makeActionKey(ctx, "⌫", 1.3f) {
                    service.handleBackspace()
                })
            }

            if (index == 1) addSpacer(row, 18)
            if (index == 2) addSpacer(row, 0) // balance

            container.addView(row)
        }

        // Bottom row: space + enter
        val bottom = makeRow(ctx)
        bottom.addView(makeActionKey(ctx, "123", 1.2f) { /* no-op for MVP */ })
        bottom.addView(makeKey(ctx, ",", onKey, weight = 0.8f))
        bottom.addView(makeKey(ctx, " ", onKey, label = "space", weight = 4f))
        bottom.addView(makeKey(ctx, ".", onKey, weight = 0.8f))
        bottom.addView(makeActionKey(ctx, "↵", 1.2f) {
            service.handleEnter()
        })
        container.addView(bottom)
    }

    private fun makeRow(ctx: Context): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 4 }
        }
    }

    private fun makeKey(
        ctx: Context,
        value: String,
        onKey: (String) -> Unit,
        label: String? = null,
        weight: Float = 1f
    ): MaterialButton {
        return MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label ?: value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label == "space") 12f else 16f)
            setTextColor(ctx.getColor(R.color.key_text))
            typeface = Typeface.DEFAULT
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_bg))
            strokeWidth = 0
            cornerRadius = 8
            insetTop = 0
            insetBottom = 0
            minWidth = 0
            minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            elevation = 2f
            layoutParams = LinearLayout.LayoutParams(
                0, dpToPx(ctx, 42), weight
            ).apply {
                marginStart = 2
                marginEnd = 2
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onKey(value)
            }
        }
    }

    private fun makeActionKey(
        ctx: Context,
        label: String,
        weight: Float,
        onClick: () -> Unit
    ): MaterialButton {
        return MaterialButton(ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
            strokeWidth = 0
            cornerRadius = 8
            insetTop = 0
            insetBottom = 0
            minWidth = 0
            minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                0, dpToPx(ctx, 42), weight
            ).apply {
                marginStart = 2
                marginEnd = 2
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        }
    }

    private fun addSpacer(row: LinearLayout, widthDp: Int) {
        row.addView(Space(row.context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(row.context, widthDp), 1)
        })
    }

    private fun dpToPx(ctx: Context, dp: Int): Int {
        return (dp * ctx.resources.displayMetrics.density).toInt()
    }
}
