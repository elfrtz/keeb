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

    // Numeric layout forced while editing USDC amount
    private var amountMode = false

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

    private data class KeyMetrics(
        val heightDp: Int,
        val rowGapDp: Int,
        val hMarginDp: Int,
        val cornerDp: Int,
        val letterSp: Float,
        val rowIndentDp: Int
    )

    private fun metrics(service: KeebInputMethodService): KeyMetrics {
        val h = service.settings.keyHeight.dp
        return KeyMetrics(
            heightDp = h,
            rowGapDp = 4,
            hMarginDp = 2,
            cornerDp = 12,
            letterSp = 14f,
            rowIndentDp = if (h <= 46) 14 else 16
        )
    }

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
    /** Show number/symbol keys while the user edits the USDC amount. */
    fun enterAmountMode(
        ctx: Context,
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        if (amountMode && showingSymbols) return
        amountMode = true
        showingSymbols = true
        buildSymbolLayout(ctx, service, container, onKey)
        symbolToggleKey?.text = "ABC"
    }

    /** Return to letter keys after amount editing. */
    fun exitAmountMode(
        ctx: Context,
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        if (!amountMode) return
        amountMode = false
        showingSymbols = false
        buildLetterLayout(ctx, service, container, onKey)
        refreshShiftKey(ctx, service.stateManager)
    }

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

        val m = metrics(service)

        // Rows 0–2: QWERTY letter rows
        for ((index, chars) in LETTER_ROWS.withIndex()) {
            val row = makeRow(ctx, m.rowGapDp)

            when (index) {
                0 -> { /* no indent */ }
                1 -> addSpacer(row, m.rowIndentDp)
                2 -> {
                    val shift = makeShiftKey(ctx, service, m)
                    shiftKey = shift
                    row.addView(shift)
                    addSpacer(row, 3)
                }
            }

            for (ch in chars) {
                val label = if (service.stateManager.isUpperCase) ch.uppercaseChar().toString()
                            else ch.lowercaseChar().toString()
                row.addView(makeLetterKey(ctx, ch.toString(), label, onKey, m))
            }

            when (index) {
                1 -> addSpacer(row, m.rowIndentDp)
                2 -> {
                    addSpacer(row, 3)
                    row.addView(makeDeleteKey(ctx, service, m))
                }
            }

            container.addView(row)
        }

        // Bottom row
        val bottom = makeRow(ctx, m.rowGapDp)
        val symToggle = makeActionKey(ctx, "?123", 1.5f, m) {
            toggleSymbols(ctx, service, container, onKey)
        }
        symbolToggleKey = symToggle
        bottom.addView(symToggle)
        bottom.addView(makeLetterKey(ctx, ",", ",", onKey, m, weight = 0.8f))
        bottom.addView(makeSpaceKey(ctx, onKey, m))
        bottom.addView(makeLetterKey(ctx, ".", ".", onKey, m, weight = 0.8f))
        bottom.addView(makeEnterKey(ctx, service, m))
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

        val m = metrics(service)

        for (chars in SYMBOL_ROWS) {
            val row = makeRow(ctx, m.rowGapDp)
            for (ch in chars) {
                row.addView(makeLetterKey(ctx, ch.toString(), ch.toString(), onKey, m))
            }
            container.addView(row)
        }

        val bottom = makeRow(ctx, m.rowGapDp)
        val abcToggle = makeActionKey(ctx, "ABC", 1.5f, m) {
            toggleSymbols(ctx, service, container, onKey)
        }
        symbolToggleKey = abcToggle
        bottom.addView(abcToggle)
        bottom.addView(makeLetterKey(ctx, " ", "space", onKey, m, weight = 4f, isSpace = true))
        bottom.addView(makeDeleteKey(ctx, service, m, weight = 1.5f))
        container.addView(bottom)
    }

    private fun toggleSymbols(
        ctx: Context,
        service: KeebInputMethodService,
        container: LinearLayout,
        onKey: (String) -> Unit
    ) {
        amountMode = false
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
    private fun applyCompactKeyStyle(btn: MaterialButton, ctx: Context, m: KeyMetrics) {
        btn.insetTop = 0
        btn.insetBottom = 0
        btn.minWidth = 0
        btn.minHeight = 0
        btn.cornerRadius = dpToPx(ctx, m.cornerDp)
        btn.elevation = 0f
        btn.stateListAnimator = null
        btn.setPadding(0, 0, 0, 0)
        btn.isAllCaps = false
    }

    private fun keyLayoutParams(
        ctx: Context,
        m: KeyMetrics,
        weight: Float
    ): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(0, dpToPx(ctx, m.heightDp), weight).apply {
            marginStart = dpToPx(ctx, m.hMarginDp)
            marginEnd = dpToPx(ctx, m.hMarginDp)
            bottomMargin = dpToPx(ctx, m.rowGapDp)
        }

    private fun makeLetterKey(
        ctx: Context,
        charValue: String,
        displayLabel: String,
        onKey: (String) -> Unit,
        m: KeyMetrics,
        weight: Float = 1f,
        isSpace: Boolean = false
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = displayLabel
            tag = charValue
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (isSpace) 11f else m.letterSp)
            setTextColor(ctx.getColor(R.color.key_text))
            typeface = Typeface.DEFAULT
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_bg))
            strokeWidth = 0
            applyCompactKeyStyle(this, ctx, m)
            layoutParams = keyLayoutParams(ctx, m, weight)
            setOnTouchListener { _, event ->
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

    @SuppressLint("ClickableViewAccessibility")
    private fun makeShiftKey(
        ctx: Context,
        service: KeebInputMethodService,
        m: KeyMetrics
    ): MaterialButton {
        val state = service.stateManager
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = state.shiftKeyLabel
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(
                if (state.isShiftActive) ctx.getColor(R.color.shift_active_color)
                else ctx.getColor(R.color.key_pressed)
            )
            strokeWidth = 0
            applyCompactKeyStyle(this, ctx, m)
            layoutParams = keyLayoutParams(ctx, m, 1.4f)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun makeDeleteKey(
        ctx: Context,
        service: KeebInputMethodService,
        m: KeyMetrics,
        weight: Float = 1.4f
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "⌫"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
            strokeWidth = 0
            applyCompactKeyStyle(this, ctx, m)
            layoutParams = keyLayoutParams(ctx, m, weight)
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
                true
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeSpaceKey(
        ctx: Context,
        onKey: (String) -> Unit,
        m: KeyMetrics
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "space"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(ctx.getColor(R.color.key_text))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_bg))
            strokeWidth = 0
            applyCompactKeyStyle(this, ctx, m)
            layoutParams = keyLayoutParams(ctx, m, 3.6f)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun makeEnterKey(
        ctx: Context,
        service: KeebInputMethodService,
        m: KeyMetrics
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "↵"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
            strokeWidth = 0
            applyCompactKeyStyle(this, ctx, m)
            layoutParams = keyLayoutParams(ctx, m, 1.4f)
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

    @SuppressLint("ClickableViewAccessibility")
    private fun makeActionKey(
        ctx: Context,
        label: String,
        weight: Float,
        m: KeyMetrics,
        onClick: () -> Unit
    ): MaterialButton {
        return MaterialButton(
            ctx, null, com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(ctx.getColor(R.color.accent))
            backgroundTintList = ColorStateList.valueOf(ctx.getColor(R.color.key_pressed))
            strokeWidth = 0
            applyCompactKeyStyle(this, ctx, m)
            layoutParams = keyLayoutParams(ctx, m, weight)
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

    private fun makeRow(ctx: Context, rowGap: Int = 6): LinearLayout {
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
