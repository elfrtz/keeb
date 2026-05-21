package com.elfrtz.keeb

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.elfrtz.keeb.keyboard.KeyboardSettings
import com.elfrtz.keeb.ui.screens.SettingsScreen

class SettingsActivity : ComponentActivity() {

    private lateinit var settings: KeyboardSettings
    private var keyHeight by mutableStateOf(KeyboardSettings.KeyHeight.LARGE)
    private var vibrationEnabled by mutableStateOf(true)
    private var soundEnabled by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
        window.statusBarColor = android.graphics.Color.parseColor("#0D1117")
        window.navigationBarColor = android.graphics.Color.parseColor("#0D1117")

        settings = KeyboardSettings(this)
        loadFromPrefs()

        setContent {
            SettingsScreen(
                keyHeight = keyHeight,
                vibrationEnabled = vibrationEnabled,
                soundEnabled = soundEnabled,
                onBack = { finish() },
                onKeyHeightSelect = { height ->
                    settings.keyHeight = height
                    keyHeight = height
                    Toast.makeText(
                        this,
                        "✓ ${height.label} keys applied — tap any text field to see the change",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                },
                onVibrationChange = {
                    settings.vibrationEnabled = it
                    vibrationEnabled = it
                },
                onSoundChange = {
                    settings.soundEnabled = it
                    soundEnabled = it
                },
                onResetDefaults = {
                    settings.resetToDefaults()
                    loadFromPrefs()
                    Toast.makeText(this, "Settings reset", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    private fun loadFromPrefs() {
        keyHeight = settings.keyHeight
        vibrationEnabled = settings.vibrationEnabled
        soundEnabled = settings.soundEnabled
    }
}
