package com.elfrtz.keeb.keyboard

import android.content.Context
import android.content.SharedPreferences

/**
 * Persists user keyboard preferences via SharedPreferences.
 * All reads/writes are synchronous and cheap — safe to call on the main thread.
 */
class KeyboardSettings(context: Context) {

    enum class KeyHeight(val dp: Int, val label: String) {
        SMALL(52, "Small"),
        MEDIUM(62, "Medium"),
        LARGE(72, "Large");

        companion object {
            fun fromName(name: String) = entries.firstOrNull { it.name == name } ?: LARGE
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var keyHeight: KeyHeight
        get() = KeyHeight.fromName(prefs.getString(KEY_HEIGHT, KeyHeight.LARGE.name) ?: KeyHeight.LARGE.name)
        set(value) { prefs.edit().putString(KEY_HEIGHT, value.name).apply() }

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATION, true)
        set(value) { prefs.edit().putBoolean(KEY_VIBRATION, value).apply() }

    var soundEnabled: Boolean
        get() = prefs.getBoolean(KEY_SOUND, false)
        set(value) { prefs.edit().putBoolean(KEY_SOUND, value).apply() }

    fun resetToDefaults() {
        prefs.edit()
            .putString(KEY_HEIGHT, KeyHeight.LARGE.name)
            .putBoolean(KEY_VIBRATION, true)
            .putBoolean(KEY_SOUND, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "keeb_settings"
        private const val KEY_HEIGHT = "key_height"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_SOUND = "sound_enabled"
    }
}
