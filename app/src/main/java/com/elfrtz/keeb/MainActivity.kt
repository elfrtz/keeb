package com.elfrtz.keeb

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.elfrtz.keeb.ui.screens.HomeScreen
import com.elfrtz.keeb.wallet.WalletManager

class MainActivity : ComponentActivity() {

    private var keyboardEnabled by mutableStateOf(false)
    private var walletConnected by mutableStateOf(false)
    private var walletStatusText by mutableStateOf("Not connected")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
        }
        window.statusBarColor = android.graphics.Color.parseColor("#0D1117")
        window.navigationBarColor = android.graphics.Color.parseColor("#0D1117")

        Thread {
            try {
                WalletManager.init(this)
            } catch (_: Exception) {
            }
            runOnUiThread { refreshState() }
        }.start()

        setContent {
            HomeScreen(
                keyboardEnabled = keyboardEnabled,
                walletConnected = walletConnected,
                walletStatusText = walletStatusText,
                onEnableKeyboard = {
                    startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                },
                onSwitchKeyboard = {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    @Suppress("DEPRECATION")
                    imm.showInputMethodPicker()
                },
                onOpenWallet = {
                    startActivity(Intent(this, WalletActivity::class.java))
                },
                onOpenSettings = {
                    startActivity(Intent(this, SettingsActivity::class.java))
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        keyboardEnabled = imm.enabledInputMethodList.any { it.packageName == packageName }
        walletConnected = WalletManager.canSend
        walletStatusText = when (WalletManager.connectionState) {
            WalletManager.ConnectionState.DEMO -> WalletManager.shortAddress ?: "Connected"
            WalletManager.ConnectionState.DISCONNECTED -> "Not connected"
        }
    }
}
