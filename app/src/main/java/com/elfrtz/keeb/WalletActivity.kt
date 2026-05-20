package com.elfrtz.keeb

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.elfrtz.keeb.wallet.WalletManager

/**
 * Wallet setup screen.
 *
 * Keeb signs USDC transfers locally with a test private key. MetaMask does not expose
 * a simple connect callback to third-party keyboards — paste the key exported from
 * your MetaMask Base Sepolia account instead.
 */
class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)

        Thread {
            try {
                WalletManager.init(applicationContext)
            } catch (_: Exception) {
            }
            runOnUiThread { updateUI() }
        }.start()

        findViewById<Button>(R.id.btn_wallet_back).setOnClickListener { finish() }
        findViewById<Button>(R.id.btn_connect_metamask).setOnClickListener {
            showWalletSetupDialog()
        }
        findViewById<Button>(R.id.btn_open_metamask).setOnClickListener {
            val opened = WalletManager.openMetaMaskApp(this)
            if (!opened) {
                Toast.makeText(this, R.string.wallet_metamask_not_installed, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btn_disconnect_wallet).setOnClickListener {
            WalletManager.disconnect(this)
            updateUI()
            Toast.makeText(this, R.string.wallet_disconnected, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (WalletManager.canSend) updateUI()
    }

    private fun showWalletSetupDialog() {
        val input = EditText(this).apply {
            hint = getString(R.string.wallet_private_key_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setPadding(48, 32, 48, 16)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.wallet_setup_title)
            .setMessage(R.string.wallet_setup_message)
            .setView(input)
            .setPositiveButton(R.string.wallet_save_key) { _, _ ->
                val key = input.text?.toString().orEmpty()
                if (key.isBlank()) {
                    Toast.makeText(this, R.string.wallet_key_empty, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val error = WalletManager.setDemoPrivateKey(this, key)
                if (error != null) {
                    Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, R.string.wallet_setup_success, Toast.LENGTH_SHORT).show()
                    updateUI()
                }
            }
            .setNeutralButton(R.string.wallet_open_metamask) { _, _ ->
                WalletManager.openMetaMaskApp(this)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun updateUI() {
        val tvState = findViewById<TextView>(R.id.tv_wallet_state)
        val tvAddress = findViewById<TextView>(R.id.tv_wallet_address)
        val tvHint = findViewById<TextView>(R.id.tv_wallet_hint)
        val btnSetup = findViewById<Button>(R.id.btn_connect_metamask)
        val btnOpenMm = findViewById<Button>(R.id.btn_open_metamask)
        val btnDisconnect = findViewById<Button>(R.id.btn_disconnect_wallet)

        if (WalletManager.canSend) {
            tvState.text = getString(R.string.wallet_state_ready)
            tvState.setTextColor(getColor(R.color.green_primary))
            tvAddress.text = WalletManager.activeAddress ?: "—"
            tvHint.text = getString(R.string.wallet_hint_ready)
            btnSetup.text = getString(R.string.wallet_change_key)
            btnOpenMm.visibility = android.view.View.GONE
            btnDisconnect.visibility = android.view.View.VISIBLE
        } else {
            tvState.text = getString(R.string.wallet_state_not_ready)
            tvState.setTextColor(getColor(R.color.dismiss_tint))
            tvAddress.text = getString(R.string.wallet_no_address)
            tvHint.text = getString(R.string.wallet_hint_setup)
            btnSetup.text = getString(R.string.wallet_setup_button)
            btnOpenMm.visibility = android.view.View.VISIBLE
            btnDisconnect.visibility = android.view.View.GONE
        }
    }
}
