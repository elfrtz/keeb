package com.elfrtz.keeb.keyboard

import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.elfrtz.keeb.R
import com.elfrtz.keeb.wallet.TransactionService
import com.elfrtz.keeb.wallet.WalletManager
import com.google.android.material.button.MaterialButton

/**
 * Core IME service. Manages the keyboard view, clipboard monitoring,
 * and the payment chip lifecycle.
 */
class KeebInputMethodService : InputMethodService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var clipboardMonitor: ClipboardMonitor
    private var chipContainer: FrameLayout? = null
    private var chipView: View? = null
    private var detectedAddress: String? = null
    private var isSending = false

    // ── Lifecycle ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        WalletManager.init(this)
        clipboardMonitor = ClipboardMonitor(
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        ) { address ->
            mainHandler.post { onAddressDetected(address) }
        }
    }

    override fun onCreateInputView(): View {
        val root = LayoutInflater.from(this)
            .inflate(R.layout.view_keyboard, null) as LinearLayout

        chipContainer = root.findViewById(R.id.chip_container)

        // Build keyboard keys
        val keyRows = root.findViewById<LinearLayout>(R.id.key_rows_container)
        KeyboardView.populate(this, keyRows) { text -> commitTyped(text) }

        clipboardMonitor.start()
        return root
    }

    override fun onDestroy() {
        clipboardMonitor.stop()
        super.onDestroy()
    }

    // ── Typing ─────────────────────────────────────────────────

    private fun commitTyped(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    fun handleBackspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    fun handleEnter() {
        currentInputConnection?.commitText("\n", 1)
    }

    // ── Address Detection ──────────────────────────────────────

    private fun onAddressDetected(address: String) {
        if (address == detectedAddress && chipView != null) return
        detectedAddress = address
        showPaymentChip(address)
    }

    // ── Payment Chip ───────────────────────────────────────────

    private fun showPaymentChip(address: String) {
        dismissChip()

        chipView = LayoutInflater.from(this)
            .inflate(R.layout.view_payment_chip, chipContainer, false)

        val tvAddress = chipView!!.findViewById<TextView>(R.id.tv_address)
        val etAmount = chipView!!.findViewById<EditText>(R.id.et_amount)
        val btnSend = chipView!!.findViewById<MaterialButton>(R.id.btn_send)
        val btnClose = chipView!!.findViewById<ImageButton>(R.id.btn_close)

        // Show shortened address: 0x1234…abcd
        val short = address.take(6) + "…" + address.takeLast(4)
        tvAddress.text = short
        etAmount.setText("5")

        btnSend.setOnClickListener {
            if (isSending) return@setOnClickListener
            val amount = etAmount.text.toString().ifBlank { "5" }
            executeSend(address, amount, btnSend)
        }

        btnClose.setOnClickListener { dismissChip() }

        chipContainer?.removeAllViews()
        chipContainer?.addView(chipView)
        chipContainer?.visibility = View.VISIBLE

        // Slide-up animation
        val anim = AnimationUtils.loadAnimation(this, R.anim.slide_up)
        chipView!!.startAnimation(anim)
    }

    private fun executeSend(
        address: String,
        amount: String,
        btnSend: MaterialButton
    ) {
        isSending = true
        btnSend.text = getString(R.string.sending)
        btnSend.isEnabled = false

        TransactionService.sendUSDC(address, amount) { success, txHash ->
            mainHandler.post {
                isSending = false
                if (success) {
                    // Insert confirmation into chat
                    currentInputConnection?.commitText(
                        getString(R.string.payment_success, amount), 1
                    )
                    Toast.makeText(this, "Tx: $txHash", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, getString(R.string.send_failed), Toast.LENGTH_SHORT).show()
                    btnSend.text = getString(R.string.send_label)
                    btnSend.isEnabled = true
                }
                dismissChip()
            }
        }
    }

    private fun dismissChip() {
        chipView?.let { v ->
            val anim = AnimationUtils.loadAnimation(this, R.anim.fade_out)
            anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(a: android.view.animation.Animation?) {}
                override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
                override fun onAnimationEnd(a: android.view.animation.Animation?) {
                    chipContainer?.removeAllViews()
                    chipContainer?.visibility = View.GONE
                }
            })
            v.startAnimation(anim)
        }
        chipView = null
        detectedAddress = null
    }
}
