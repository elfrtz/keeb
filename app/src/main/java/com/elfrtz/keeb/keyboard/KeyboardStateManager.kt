package com.elfrtz.keeb.keyboard

import android.view.inputmethod.InputConnection

/**
 * Manages all capitalization state for the keyboard.
 *
 * Responsibilities:
 * - Track ShiftState (LOWER / SHIFT_ONCE / CAPS_LOCK)
 * - Determine whether the next character should be uppercase
 * - Auto-capitalize after sentence-ending punctuation + space
 * - Update state after each character is typed
 * - Expose the current display label for the shift key
 *
 * This class is intentionally stateless with respect to the view —
 * callers must call [onShiftKeyTapped] / [onShiftKeyLongPressed] and
 * then redraw the shift key using [shiftKeyLabel] and [isShiftActive].
 */
class KeyboardStateManager {

    var shiftState: ShiftState = ShiftState.SHIFT_ONCE  // start with first-letter cap
        private set

    // True when sentence-case logic wants the next char capitalized,
    // independent of the explicit shift state.
    private var sentenceCaseNext: Boolean = true  // capitalize very first character

    // ── Public API ─────────────────────────────────────────────

    /**
     * Returns true if the next character typed should be uppercase.
     * Considers both explicit shift state and sentence-case override.
     */
    val isUpperCase: Boolean
        get() = when (shiftState) {
            ShiftState.CAPS_LOCK  -> true
            ShiftState.SHIFT_ONCE -> true
            ShiftState.LOWER      -> sentenceCaseNext
        }

    /** Label to display on the shift key for the current state. */
    val shiftKeyLabel: String
        get() = when (shiftState) {
            ShiftState.LOWER      -> "⇧"
            ShiftState.SHIFT_ONCE -> "⬆"   // filled arrow = single shift active
            ShiftState.CAPS_LOCK  -> "⇪"   // caps lock symbol
        }

    /** True when the shift key should appear visually highlighted. */
    val isShiftActive: Boolean
        get() = shiftState != ShiftState.LOWER || sentenceCaseNext

    /** True when caps lock is engaged. */
    val isCapsLock: Boolean
        get() = shiftState == ShiftState.CAPS_LOCK

    /**
     * Called when the user taps the shift key.
     * Cycles: LOWER → SHIFT_ONCE → LOWER (caps lock is long-press only).
     * If currently in CAPS_LOCK, a tap turns it off.
     */
    fun onShiftKeyTapped() {
        sentenceCaseNext = false  // explicit user action overrides sentence case
        shiftState = when (shiftState) {
            ShiftState.LOWER      -> ShiftState.SHIFT_ONCE
            ShiftState.SHIFT_ONCE -> ShiftState.LOWER
            ShiftState.CAPS_LOCK  -> ShiftState.LOWER
        }
    }

    /**
     * Called when the user long-presses the shift key.
     * Toggles CAPS_LOCK on/off.
     */
    fun onShiftKeyLongPressed() {
        sentenceCaseNext = false
        shiftState = if (shiftState == ShiftState.CAPS_LOCK) {
            ShiftState.LOWER
        } else {
            ShiftState.CAPS_LOCK
        }
    }

    /**
     * Called after a letter key is committed.
     * If in SHIFT_ONCE, returns to LOWER after the letter is typed.
     * Clears the sentence-case override.
     */
    fun onLetterTyped() {
        sentenceCaseNext = false
        if (shiftState == ShiftState.SHIFT_ONCE) {
            shiftState = ShiftState.LOWER
        }
    }

    /**
     * Called after any character is committed.
     * Inspects the current text before the cursor to determine whether
     * sentence-case should capitalize the next character.
     *
     * Capitalizes after: ". ", "! ", "? ", or at the very start of input.
     */
    fun onCharTyped(ic: InputConnection?) {
        if (shiftState == ShiftState.CAPS_LOCK) return  // caps lock overrides everything

        val textBefore = ic?.getTextBeforeCursor(3, 0)?.toString() ?: return
        sentenceCaseNext = shouldCapitalizeNext(textBefore)
    }

    /**
     * Called when the keyboard attaches to a new input field.
     * Resets to sentence-case start (capitalize first letter).
     */
    fun onInputStarted(ic: InputConnection?) {
        if (shiftState == ShiftState.CAPS_LOCK) return

        val textBefore = ic?.getTextBeforeCursor(3, 0)?.toString() ?: ""
        sentenceCaseNext = textBefore.isEmpty() || shouldCapitalizeNext(textBefore)

        // Don't override an explicit SHIFT_ONCE the user set before focusing
        if (shiftState == ShiftState.LOWER && sentenceCaseNext) {
            // Keep LOWER state but sentenceCaseNext will drive capitalization
        }
    }

    /**
     * Called when backspace is pressed — re-evaluate sentence case
     * since the character before cursor may have changed.
     */
    fun onBackspace(ic: InputConnection?) {
        if (shiftState == ShiftState.CAPS_LOCK) return
        val textBefore = ic?.getTextBeforeCursor(3, 0)?.toString() ?: ""
        sentenceCaseNext = textBefore.isEmpty() || shouldCapitalizeNext(textBefore)
    }

    // ── Private helpers ────────────────────────────────────────

    private fun shouldCapitalizeNext(textBefore: String): Boolean {
        if (textBefore.isEmpty()) return true
        // Capitalize after ". ", "! ", "? " (sentence end + space)
        if (textBefore.length >= 2) {
            val last2 = textBefore.takeLast(2)
            if (last2[1] == ' ' && last2[0] in listOf('.', '!', '?')) return true
        }
        return false
    }
}
