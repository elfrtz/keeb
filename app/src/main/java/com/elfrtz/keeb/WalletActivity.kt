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
import com.elfrtz.keeb.ui.screens.WalletScreen
import com.elfrtz.keeb.wallet.WalletManager

class WalletActivity : ComponentActivity() {

    private var isReady by mutableStateOf(false)
    private var address by mutableStateOf<String?>(null)

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
                WalletManager.init(applicationContext)
            } catch (_: Exception) {
            }
            runOnUiThread { updateState() }
        }.start()

        setContent {
            WalletScreen(
                isReady = isReady,
                address = address,
                onBack = { finish() },
                onSaveKey = { key ->
                    if (key.isBlank()) {
                        Toast.makeText(this, R.string.wallet_key_empty, Toast.LENGTH_SHORT).show()
                        return@WalletScreen
                    }
                    val error = WalletManager.setDemoPrivateKey(this, key)
                    if (error != null) {
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, R.string.wallet_setup_success, Toast.LENGTH_SHORT).show()
                        updateState()
                    }
                },
                onDisconnect = {
                    WalletManager.disconnect(this)
                    updateState()
                    Toast.makeText(this, R.string.wallet_disconnected, Toast.LENGTH_SHORT).show()
                },
                onOpenMetaMask = {
                    val opened = WalletManager.openMetaMaskApp(this)
                    if (!opened) {
                        Toast.makeText(this, R.string.wallet_metamask_not_installed, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (WalletManager.canSend) updateState()
    }

    private fun updateState() {
        isReady = WalletManager.canSend
        address = WalletManager.activeAddress
    }
}
