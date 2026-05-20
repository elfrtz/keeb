package com.elfrtz.keeb

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.elfrtz.keeb.wallet.WalletManager

/**
 * Launcher activity — onboarding + wallet status hub.
 *
 * Flow:
 *  1. Enable Keeb in system settings
 *  2. Switch to Keeb keyboard
 *  3. Connect wallet (optional — demo wallet works out of the box)
 *  4. Open Telegram/Discord and start typing
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Init wallet in background so status is ready on resume
        Thread {
            try { WalletManager.init(this) } catch (_: Exception) {}
        }.start()

        findViewById<Button>(R.id.btn_enable_keyboard).setOnClickListener {
            startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
        }

        findViewById<Button>(R.id.btn_switch_keyboard).setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            @Suppress("DEPRECATION")
            imm.showInputMethodPicker()
        }

        findViewById<Button>(R.id.btn_open_wallet).setOnClickListener {
            startActivity(Intent(this, WalletActivity::class.java))
        }

        findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateKeyboardStatus()
        updateWalletStatus()
    }

    private fun updateKeyboardStatus() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        findViewById<TextView>(R.id.tv_keyboard_status).text =
            if (enabled) "✅ Keeb keyboard enabled" else "⚠️ Keeb not enabled yet — tap step 1"
    }

    private fun updateWalletStatus() {
        val tv = findViewById<TextView>(R.id.tv_wallet_status)
        tv.text = when (WalletManager.connectionState) {
            WalletManager.ConnectionState.CONNECTED ->
                "🟢 ${WalletManager.shortAddress}"
            WalletManager.ConnectionState.DEMO ->
                "🟡 Demo wallet: ${WalletManager.shortAddress}"
            WalletManager.ConnectionState.DISCONNECTED ->
                "⚪ No wallet connected"
        }
    }
}
