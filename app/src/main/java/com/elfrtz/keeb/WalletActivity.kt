package com.elfrtz.keeb

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.elfrtz.keeb.wallet.WalletManager

/**
 * Wallet connection screen.
 *
 * Shows current wallet state and allows the user to:
 *  - Connect MetaMask via deep-link
 *  - Disconnect the current wallet
 *
 * For the hackathon demo, if a DEMO_PRIVATE_KEY is configured in BaseConfig,
 * the wallet is already in DEMO mode and the address is shown immediately.
 */
class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        findViewById<Button>(R.id.btn_wallet_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_connect_metamask).setOnClickListener {
            WalletManager.launchMetaMaskConnect(this)
        }
        findViewById<Button>(R.id.btn_disconnect_wallet).setOnClickListener {
            WalletManager.disconnect(this)
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check state when returning from MetaMask
        updateUI()
    }

    private fun updateUI() {
        val tvState   = findViewById<TextView>(R.id.tv_wallet_state)
        val tvAddress = findViewById<TextView>(R.id.tv_wallet_address)
        val btnConnect    = findViewById<Button>(R.id.btn_connect_metamask)
        val btnDisconnect = findViewById<Button>(R.id.btn_disconnect_wallet)

        when (WalletManager.connectionState) {
            WalletManager.ConnectionState.DISCONNECTED -> {
                tvState.text = "⚪ Not Connected"
                tvState.setTextColor(getColor(R.color.dismiss_tint))
                tvAddress.text = "No wallet connected"
                btnConnect.visibility = android.view.View.VISIBLE
                btnDisconnect.visibility = android.view.View.GONE
            }
            WalletManager.ConnectionState.DEMO -> {
                tvState.text = "🟡 Demo Wallet"
                tvState.setTextColor(getColor(R.color.send_btn_bg))
                tvAddress.text = WalletManager.shortAddress ?: "—"
                btnConnect.visibility = android.view.View.VISIBLE
                btnDisconnect.visibility = android.view.View.GONE
            }
            WalletManager.ConnectionState.CONNECTED -> {
                tvState.text = "🟢 MetaMask Connected"
                tvState.setTextColor(getColor(R.color.accent))
                tvAddress.text = WalletManager.shortAddress ?: "—"
                btnConnect.visibility = android.view.View.GONE
                btnDisconnect.visibility = android.view.View.VISIBLE
            }
        }
    }
}
