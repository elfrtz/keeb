package com.elfrtz.keeb.keyboard

/**
 * The three capitalization states of the shift key.
 *
 * LOWER      — all letters lowercase (user explicitly shifted down)
 * SHIFT_ONCE — next letter uppercase, then auto-return to LOWER
 * CAPS_LOCK  — all letters uppercase until toggled off
 *
 * Sentence-case (auto-capitalize after sentence end) is handled separately
 * in KeyboardStateManager and does NOT change the ShiftState enum value —
 * it is a transient override that only applies to the very next character.
 */
enum class ShiftState {
    LOWER,
    SHIFT_ONCE,
    CAPS_LOCK
}
