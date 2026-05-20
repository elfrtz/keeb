package com.elfrtz.keeb.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import com.elfrtz.keeb.R
import com.google.android.material.button.MaterialButton

/**
 * Builds and manages the QWERTY keyboard layout.
 *
 * Features:
 * - Shift key with 3 states (LOWER / SHIFT_ONCE / CAPS_LOCK)
 * - Long-press delete with repeat
 * - Symbol/number row toggle
 * - Larger, thumb-friendly key sizes
 * - Visual pressed feedback
 * - Sentence-case auto-capitalization
 *
 * [ctx] MUST be a ContextThemeWrapper with a Material theme — bare IME context
 * will cause MaterialButton to produce invisible/zero-size views.
 */
object KeyboardView {

    private const val TAG = "KeyboardView"

    // ── Layout constants ───────────────────────────────────────

    private val LETTER_ROWS = listOf(
        "qwertyuiop",
        "asdfghjkl",
        "zxcvbnm"
    )

    // Symbol page shown when user taps "?123"
    private val SYMBOL_ROWS = listOf(
        "1234567890",
        "@#\$%&-+()",
        "!\"';:/?.,",
    )

    // ── State ──────────────────────────────────────────────────

    // Whether the symbol page is currently shown
    private var showingSymbols = false

    // Reference to the shift key so we can update its label/color
    private var shiftKey: MaterialButton? = null

    // Reference to the symbol toggle key
    private var symbolToggleKey: MaterialButton? = null

    // Reference to the container so we can rebuild on shift/symbol toggle
    private var keyRowsContainer: LinearLayout? = null

    // Callbacks stored so we can rebuild without re-passing them
    private var storedCtx: Context? = null
    private var storedService: KeebInputMethodService? = null
    private var storedOnKey: ((String) -> Unit)? = null

    // ── Public API ─────────────────────────────────────────────

    /**
     * Build the full keyboard into [container].
     * Call once from onCreateInputView; use [refreshShiftKey] to update visuals.
     */
    fun populate(
        ctx: Context,
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        storedCtx = ctx
        storedService = service
        storedOnKey = onKey
        keyRowsContainer = container
        showingSymbols = false

        buildLetterLayout(ctx, service, container, onKey)
        Log.d(TAG, "populate complete — rows: ${container.childCount}")
    }

    /**
     * Update the shift key's visual appearance to match current state.
     * Call this whenever ShiftState changes.
     */
    fun refreshShiftKey(ctx: Context, state: KeyboardStateManager) {
        shiftKey?.let { key ->
            key.text = state.shiftKeyLabel
            val color = when {
                state.isCapsLock   -> ctx.getColor(R.color.shift_caps_color)
                state.isShiftActive -> ctx.getColor(R.color.shift_active_color)
                else               -> ctx.getColor(R.color.key_pressed)
            }
            key.backgroundTintList = ColorStateList.valueOf(color)
        }

        // Also update letter key labels to reflect case
        keyRowsContainer?.let { container ->
            updateLetterLabels(container, state.isUpperCase)
        }
    }

    // ── Layout builders ────────────────────────────────────────

    private fun buildLetterLayout(
        ctx: Context,
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        container.removeAllViews()
        shiftKey = null
        symbolToggleKey = null

        val keyHeightDp = service.settings.keyHeight.dp

        // Rows 0–2: QWERTY letter rows
        for ((index, chars) in LETTER_ROWS.withIndex()) {
            val row = makeRow(ctx)

            when (index) {
                0 -> { /* no indent */ }
                1 -> addSpacer(row, 20)
                2 -> {
                    // Shift key on the left of row 2
                    val shift = makeShiftKey(ctx, service, keyHeightDp)
                    shiftKey = shift
                    row.addView(shift)
                    addSpacer(row, 4)
                }
            }

            for (ch in chars) {
                val displayLabel = if (service.stateManager.isUpperCase) ch.uppercaseChar().toString()
                                   else ch.lowercaseChar().toString()
                row.addView(makeLetterKey(ctx, ch.toString(), displayLabel, onKey, keyHeightDp))
            }

            when (index) {
                1 -> addSpacer(row, 20)
                2 -> {
                    addSpacer(row, 4)
                    // Backspace on the right of row 2
                    row.addView(makeDeleteKey(ctx, service, keyHeightDp))
                }
            }

            container.addView(row)
        }

        // Row 3: bottom action row
        val bottom = makeRow(ctx)
        val symToggle = makeActionKey(ctx, "?123", 1.5f, keyHeightDp) {
            toggleSymbols(ctx, service, container, onKey)
        }
        symbolToggleKey = symToggle
        bottom.addView(symToggle)
        bottom.addView(makeLetterKey(ctx, ",", ",", onKey, keyHeightDp, weight = 0.8f))
        bottom.addView(makeSpaceKey(ctx, onKey, keyHeightDp))
        bottom.addView(makeLetterKey(ctx, ".", ".", onKey, keyHeightDp, weight = 0.8f))
        bottom.addView(makeEnterKey(ctx, service, keyHeightDp))
        container.addView(bottom)
    }

    private fun buildSymbolLayout(
        ctx: Context,
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        container.removeAllViews()
        shiftKey = null

        val keyHeightDp = service.settings.keyHeight.dp

        for (chars in SYMBOL_ROWS) {
            val row = makeRow(ctx)
            for (ch in chars) {
                row.addView(makeLetterKey(ctx, ch.toString(), ch.toString(), onKey, keyHeightDp))
            }
            container.addView(row)
        }

        // Bottom row for symbols
        val bottom = makeRow(ctx)
        val abcToggle = makeActionKey(ctx, "ABC", 1.5f, keyHeightDp) {
            toggleSymbols(ctx, service, container, onKey)
        }
        symbolToggleKey = abcToggle
        bottom.addView(abcToggle)
        bottom.addView(makeLetterKey(ctx, " ", "space", onKey, keyHeightDp, weight = 4f, isSpace = true))
        bottom.addView(makeDeleteKey(ctx, service, keyHeightDp, weight = 1.5f))
        container.addView(bottom)
    }

    private fun toggleSymbols(
        ctx: Context,
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        showingSymbols = !showingSymbols
        if (showingSymbols) {
            buildSymbolLayout(ctx, service, container, onKey)
        } else {
            buildLetterLayout(ctx, service, container, onKey)
            refreshShiftKey(ctx, service.stateManager)
        }
    }

    private fun updateLetterLabels(container: LinearLayout, uppercase: Boolean) {
        // Walk the row containers and update letter key text
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i) as? LinearLayout ?: continue
            for (j in 0 until row.childCount) {
                val key = row.getChildAt(j) as? MaterialButton ?: continue
                val tag = key.tag as? String ?: continue
                if (tag.length == 1 && tag[0].isLetter()) {
                    key.text = if (uppercase) tag.uppercase() else tag.lowercase()
                }
            }
        }
    }

    // ── Key factories ──────────────────────────────────────────

    /**
     * A standard letter/character key. [charValue] is what gets committed;
     * [displayLabel] is what's shown (may differ for case).
     * [tag] is set to [charValue] so updateLetterLabels can find letter keys.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun makeLetterKey(
        ctx: Context,
        charValue: String,
        displayLabel: String,
        onKey: (String) -> Unit,
        heightDp: Int,
        weight: Float = 1f,
        isSpace: Boolean = false
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = displayLabel
            tag = charValue  // used by updateLetterLabels
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isSpace) 12f else 15f)
            setTextColor(ctx.getColor(R.color.key_text))
            typeface = Typeface.DEFAULT
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_bg))
            strokeWidth = 0
            cornerRadius = 10
            insetTop = 0; insetBottom = 0
            minWidth = 0; minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            elevation = 3f
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, heightDp), weight).apply {
                marginStart = 3; marginEnd = 3; bottomMargin = 5
            }
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed_feedback))
                        false
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_bg))
                        false
                    }
                    else -> false
                }
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onKey(charValue)
            }
        }
    }

    /** The shift key — tap cycles LOWER↔SHIFT_ONCE, long-press toggles CAPS_LOCK. */
    @SuppressLint("ClickableViewAccessibility")
    private fun makeShiftKey(
        ctx: Context,
        service: KeebInputMethodService,
        heightDp: Int
    ): MaterialButton {
        val state = service.stateManager
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = state.shiftKeyLabel
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(
                if (state.isShiftActive) ctx.getColor(R.color.shift_active_color)
                else ctx.getColor(R.color.key_pressed)
            )
            strokeWidth = 0
            cornerRadius = 10
            insetTop = 0; insetBottom = 0
            minWidth = 0; minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            elevation = 3f
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, heightDp), 1.4f).apply {
                marginStart = 3; marginEnd = 3; bottomMargin = 5
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                service.handleShiftTap()
            }
            setOnLongClickListener {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                service.handleShiftLongPress()
                true
            }
        }
    }

    /** The delete key — tap deletes one char, long-press repeats. */
    @SuppressLint("ClickableViewAccessibility")
    private fun makeDeleteKey(
        ctx: Context,
        service: KeebInputMethodService,
        heightDp: Int,
        weight: Float = 1.4f
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "⌫"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
            strokeWidth = 0
            cornerRadius = 10
            insetTop = 0; insetBottom = 0
            minWidth = 0; minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            elevation = 3f
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, heightDp), weight).apply {
                marginStart = 3; marginEnd = 3; bottomMargin = 5
            }
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        service.deleteRepeater.start { service.handleBackspace() }
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed_feedback))
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        service.deleteRepeater.stop()
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
                    }
                }
                true  // consume — we handle click via repeater
            }
        }
    }

    /** The spacebar. */
    @SuppressLint("ClickableViewAccessibility")
    private fun makeSpaceKey(
        ctx: Context,
        onKey: (String) -> Unit,
        heightDp: Int
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "space"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ctx.getColor(R.color.key_text))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_bg))
            strokeWidth = 0
            cornerRadius = 10
            insetTop = 0; insetBottom = 0
            minWidth = 0; minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            elevation = 3f
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, heightDp), 4f).apply {
                marginStart = 3; marginEnd = 3; bottomMargin = 5
            }
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN ->
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed_feedback))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_bg))
                }
                false
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onKey(" ")
            }
        }
    }

    /** The enter/return key. */
    @SuppressLint("ClickableViewAccessibility")
    private fun makeEnterKey(
        ctx: Context,
        service: KeebInputMethodService,
        heightDp: Int
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "↵"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
            strokeWidth = 0
            cornerRadius = 10
            insetTop = 0; insetBottom = 0
            minWidth = 0; minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            elevation = 3f
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, heightDp), 1.5f).apply {
                marginStart = 3; marginEnd = 3; bottomMargin = 5
            }
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN ->
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed_feedback))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
                }
                false
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                service.handleEnter()
            }
        }
    }

    /** A generic action key (symbol toggle, etc.). */
    @SuppressLint("ClickableViewAccessibility")
    private fun makeActionKey(
        ctx: Context,
        label: String,
        weight: Float,
        heightDp: Int,
        onClick: () -> Unit
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
            strokeWidth = 0
            cornerRadius = 10
            insetTop = 0; insetBottom = 0
            minWidth = 0; minHeight = 0
            setPadding(0, 0, 0, 0)
            isAllCaps = false
            elevation = 3f
            layoutParams = LinearLayout.LayoutParams(0, dpToPx(ctx, heightDp), weight).apply {
                marginStart = 3; marginEnd = 3; bottomMargin = 5
            }
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN ->
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed_feedback))
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                        backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
                }
                false
            }
            setOnClickListener {
                performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────

    private fun makeRow(ctx: Context): LinearLayout {
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun addSpacer(row: LinearLayout, widthDp: Int) {
        row.addView(Space(row.context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(row.context, widthDp), 1)
        })
    }

    private fun dpToPx(ctx: Context, dp: Int): Int =
        (dp * ctx.resources.displayMetrics.density).toInt()
}
