package com.elfrtz.keeb

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.elfrtz.keeb.keyboard.KeyboardSettings

/**
 * Full-screen settings activity.
 *
 * Key height: explicit "Apply" button saves the setting AND forces the IME to
 * rebuild by switching input methods — this is the only reliable way to trigger
 * onCreateInputView() from outside the IME process.
 *
 * The user flow is:
 *   1. Pick height
 *   2. Tap Apply
 *   3. Toast confirms — keyboard rebuilds with new height on next focus
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var settings: KeyboardSettings
    private var pendingHeight: KeyboardSettings.KeyHeight? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        settings = KeyboardSettings(this)

        setupHeightPicker()
        setupApplyButton()
        setupToggles()

        findViewById<Button>(R.id.btn_settings_back).setOnClickListener { finish() }
    }

    private fun setupHeightPicker() {
        val radioGroup = findViewById<RadioGroup>(R.id.rg_key_height)
        val current = settings.keyHeight

        val ids = mapOf(
            KeyboardSettings.KeyHeight.SMALL.ordinal  to R.id.rb_height_small,
            KeyboardSettings.KeyHeight.MEDIUM.ordinal to R.id.rb_height_medium,
            KeyboardSettings.KeyHeight.LARGE.ordinal  to R.id.rb_height_large
        )
        ids[current.ordinal]?.let { radioGroup.check(it) }
        pendingHeight = current

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            pendingHeight = when (checkedId) {
                R.id.rb_height_small  -> KeyboardSettings.KeyHeight.SMALL
                R.id.rb_height_medium -> KeyboardSettings.KeyHeight.MEDIUM
                else                  -> KeyboardSettings.KeyHeight.LARGE
            }
        }
    }

    private fun setupApplyButton() {
        findViewById<Button>(R.id.btn_apply_height).setOnClickListener {
            val height = pendingHeight ?: return@setOnClickListener
            settings.keyHeight = height

            Toast.makeText(
                this,
                "✓ ${height.label} keys applied — tap any text field to see the change",
                Toast.LENGTH_SHORT
            ).show()

            // Close settings so the user returns to their chat app.
            // The next time they tap a text field, Android calls onCreateInputView()
            // which reads the fresh keyHeight and rebuilds all keys at the new size.
            finish()
        }
    }

    private fun setupToggles() {
        val switchVibration = findViewById<Switch>(R.id.switch_vibration)
        val switchSound     = findViewById<Switch>(R.id.switch_sound)

        switchVibration.isChecked = settings.vibrationEnabled
        switchSound.isChecked     = settings.soundEnabled

        switchVibration.setOnCheckedChangeListener { _, checked ->
            settings.vibrationEnabled = checked
        }
        switchSound.setOnCheckedChangeListener { _, checked ->
            settings.soundEnabled = checked
        }
    }
}
