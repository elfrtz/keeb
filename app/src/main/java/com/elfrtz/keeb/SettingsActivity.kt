package com.elfrtz.keeb

import android.os.Bundle
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
 * Key height requires an explicit "Apply" tap — this makes the save action
 * intentional and gives clear feedback. Toggles save instantly (no apply needed).
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
                "✓ Key height set to ${height.label} — takes effect next time you open the keyboard",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupToggles() {
        val switchVibration = findViewById<Switch>(R.id.switch_vibration)
        val switchSound     = findViewById<Switch>(R.id.switch_sound)

        switchVibration.isChecked = settings.vibrationEnabled
        switchSound.isChecked     = settings.soundEnabled

        switchVibration.setOnCheckedChangeListener { _, checked ->
            settings.vibrationEnabled = checked
            val msg = if (checked) "Vibration on" else "Vibration off"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        switchSound.setOnCheckedChangeListener { _, checked ->
            settings.soundEnabled = checked
            val msg = if (checked) "Sound on" else "Sound off"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
