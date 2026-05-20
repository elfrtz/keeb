package com.elfrtz.keeb.keyboard

import android.content.ClipboardManager
import android.content.Intent
import android.content.res.ColorStateList
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.elfrtz.keeb.R
import com.elfrtz.keeb.SettingsActivity
import com.elfrtz.keeb.WalletActivity
import com.elfrtz.keeb.wallet.TransactionService
import com.elfrtz.keeb.wallet.WalletManager
import com.google.android.material.button.MaterialButton

/**
 * Core IME service.
 *
 * Payment chip state machine:
 *   IDLE → user sees amount + address + Send button
 *   WALLET_REQUIRED → no wallet; shows Connect inline
 *   SENDING → spinner text, button disabled
 *   SUCCESS → green confirmation, auto-dismiss after 2 s
 *   FAILED  → red error, Send re-enabled
 *
 * Keyboard resize:
 *   KeyboardSettings.keyHeight is read fresh on every onCreateInputView().
 *   Android calls onCreateInputView() each time the keyboard window is shown,
 *   so changing the setting and re-focusing any text field applies it immediately.
 */
class KeebInputMethodService : InputMethodService() {

    companion object {
        private const val TAG = "KeebIME"
    }

    // ── Subsystems ─────────────────────────────────────────────
    val stateManager   = KeyboardStateManager()
    val deleteRepeater = DeleteKeyRepeater()
    lateinit var settings: KeyboardSettings
        private set

    // ── Private ────────────────────────────────────────────────
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var clipboardMonitor: ClipboardMonitor
    private lateinit var themedContext: ContextThemeWrapper
    private lateinit var themedInflater: LayoutInflater

    private var keyRowsContainer: LinearLayout? = null
    private var chipContainer: FrameLayout? = null
    private var chipView: View? = null
    private var tvWalletDot: TextView? = null
    private var tvWalletLabel: TextView? = null
    private var detectedAddress: String? = null
    private var isSending = false

    // ── Chip state enum ────────────────────────────────────────
    private enum class ChipState { IDLE, WALLET_REQUIRED, SENDING, SUCCESS, FAILED }

    // ── Lifecycle ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")

        settings = KeyboardSettings(this)
        themedContext = ContextThemeWrapper(this, R.style.Theme_Keeb_Keyboard)
        themedInflater = LayoutInflater.from(themedContext)

        Thread {
            try {
                WalletManager.init(this)
                mainHandler.post { refreshWalletDot() }
            } catch (e: Exception) {
                Log.e(TAG, "WalletManager init failed", e)
            }
        }.start()

        clipboardMonitor = ClipboardMonitor(
            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        ) { address -> mainHandler.post { onAddressDetected(address) } }
    }

    /**
     * Called every time the keyboard window is shown.
     * Reading settings.keyHeight here means any height change the user saved
     * in SettingsActivity takes effect the next time they tap a text field —
     * no restart, no manual rebuild needed.
     */
    override fun onCreateInputView(): View {
        Log.d(TAG, "onCreateInputView — keyHeight=${settings.keyHeight.dp}dp")

        val root = themedInflater.inflate(R.layout.view_keyboard, null) as LinearLayout
        chipContainer    = root.findViewById(R.id.chip_container)
        keyRowsContainer = root.findViewById(R.id.key_rows_container)
        tvWalletDot      = root.findViewById(R.id.tv_wallet_dot)
        tvWalletLabel    = root.findViewById(R.id.tv_wallet_label)

        // KeyboardView reads settings.keyHeight inside populate() — always fresh
        KeyboardView.populate(themedContext, this, keyRowsContainer!!) { text ->
            commitTyped(text)
        }

        root.findViewById<LinearLayout>(R.id.btn_wallet)?.setOnClickListener {
            launchActivity(WalletActivity::class.java)
        }
        root.findViewById<LinearLayout>(R.id.btn_settings)?.setOnClickListener {
            launchActivity(SettingsActivity::class.java)
        }

        refreshWalletDot()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        stateManager.onInputStarted(currentInputConnection)
        refreshShiftVisuals()
        refreshWalletDot()
        clipboardMonitor.start()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        deleteRepeater.stop()
        clipboardMonitor.stop()
    }

    override fun onDestroy() {
        deleteRepeater.destroy()
        clipboardMonitor.stop()
        super.onDestroy()
    }

    // ── Typing ─────────────────────────────────────────────────

    private fun commitTyped(text: String) {
        val ic = currentInputConnection ?: return
        val toCommit = if (text.length == 1 && text[0].isLetter()) {
            if (stateManager.isUpperCase) text.uppercase() else text.lowercase()
        } else text
        ic.commitText(toCommit, 1)
        if (text.length == 1 && text[0].isLetter()) stateManager.onLetterTyped()
        stateManager.onCharTyped(ic)
        refreshShiftVisuals()
    }

    fun handleBackspace() {
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
        stateManager.onBackspace(ic)
        refreshShiftVisuals()
    }

    fun handleEnter() {
        currentInputConnection?.commitText("\n", 1)
    }

    // ── Shift ──────────────────────────────────────────────────

    fun handleShiftTap() {
        stateManager.onShiftKeyTapped()
        refreshShiftVisuals()
    }

    fun handleShiftLongPress() {
        stateManager.onShiftKeyLongPressed()
        refreshShiftVisuals()
    }

    private fun refreshShiftVisuals() {
        keyRowsContainer?.let { KeyboardView.refreshShiftKey(themedContext, stateManager) }
    }

    // ── Wallet dot ─────────────────────────────────────────────

    fun refreshWalletDot() {
        val (dot, label) = when (WalletManager.connectionState) {
            WalletManager.ConnectionState.CONNECTED ->
                "🟢" to (WalletManager.shortAddress ?: "Connected")
            WalletManager.ConnectionState.DEMO ->
                "🟡" to "Demo: ${WalletManager.shortAddress ?: ""}"
            WalletManager.ConnectionState.DISCONNECTED ->
                "⚪" to "Connect Wallet"
        }
        tvWalletDot?.text   = dot
        tvWalletLabel?.text = label
    }

    // ── Activity launch ────────────────────────────────────────

    private fun <T> launchActivity(cls: Class<T>) {
        applicationContext.startActivity(
            Intent(applicationContext, cls).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        )
    }

    // ── Address detection ──────────────────────────────────────

    private fun onAddressDetected(address: String) {
        if (address == detectedAddress && chipView != null) return
        detectedAddress = address
        showPaymentChip(address)
    }

    // ── Payment chip ───────────────────────────────────────────

    private fun showPaymentChip(address: String) {
        dismissChip()
        chipView = themedInflater.inflate(R.layout.view_payment_chip, chipContainer, false)

        val tvAddress   = chipView!!.findViewById<TextView>(R.id.tv_address)
        val etAmount    = chipView!!.findViewById<EditText>(R.id.et_amount)
        val btnSend     = chipView!!.findViewById<MaterialButton>(R.id.btn_send)
        val btnClose    = chipView!!.findViewById<ImageButton>(R.id.btn_close)
        val tvStatus    = chipView!!.findViewById<TextView>(R.id.tv_chip_status)
        val rowMain     = chipView!!.findViewById<LinearLayout>(R.id.chip_main_row)
        val rowWallet   = chipView!!.findViewById<LinearLayout>(R.id.chip_wallet_required_row)
        val btnConnect  = chipView!!.findViewById<MaterialButton>(R.id.btn_connect_from_chip)
        val btnCloseWR  = chipView!!.findViewById<ImageButton>(R.id.btn_close_wallet_required)

        tvAddress.text = address.take(6) + "…" + address.takeLast(4)
        etAmount.setText("5")

        // Determine initial state
        if (!WalletManager.isConnected) {
            applyChipState(ChipState.WALLET_REQUIRED, rowMain, rowWallet, btnSend, tvStatus)
        } else {
            applyChipState(ChipState.IDLE, rowMain, rowWallet, btnSend, tvStatus)
        }

        btnSend.setOnClickListener {
            if (isSending) return@setOnClickListener
            if (!WalletManager.isConnected) {
                // Wallet disconnected at send time — switch to wallet-required state
                applyChipState(ChipState.WALLET_REQUIRED, rowMain, rowWallet, btnSend, tvStatus)
                return@setOnClickListener
            }
            val amount = etAmount.text.toString().ifBlank { "5" }
            executeSend(address, amount, btnSend, tvStatus, rowMain, rowWallet)
        }

        btnClose.setOnClickListener { dismissChip() }
        btnCloseWR.setOnClickListener { dismissChip() }

        // "Connect" inside the chip — launches WalletActivity then returns
        btnConnect.setOnClickListener {
            launchActivity(WalletActivity::class.java)
            // After user returns, onStartInputView fires and refreshes wallet dot.
            // If they connected, next Send tap will proceed normally.
        }

        chipContainer?.removeAllViews()
        chipContainer?.addView(chipView)
        chipContainer?.visibility = View.VISIBLE
        chipView!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
    }

    private fun applyChipState(
        state: ChipState,
        rowMain: LinearLayout,
        rowWallet: LinearLayout,
        btnSend: MaterialButton,
        tvStatus: TextView
    ) {
        when (state) {
            ChipState.IDLE -> {
                rowMain.visibility   = View.VISIBLE
                rowWallet.visibility = View.GONE
                tvStatus.visibility  = View.GONE
                btnSend.text = getString(R.string.send_label)
                btnSend.isEnabled = true
                btnSend.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.blue_primary))
            }
            ChipState.WALLET_REQUIRED -> {
                rowMain.visibility   = View.GONE
                rowWallet.visibility = View.VISIBLE
                tvStatus.visibility  = View.GONE
            }
            ChipState.SENDING -> {
                rowMain.visibility   = View.VISIBLE
                rowWallet.visibility = View.GONE
                tvStatus.text = "⏳  Sending on Base…"
                tvStatus.setTextColor(getColor(R.color.text_secondary))
                tvStatus.visibility = View.VISIBLE
                btnSend.text = "…"
                btnSend.isEnabled = false
            }
            ChipState.SUCCESS -> {
                rowMain.visibility   = View.VISIBLE
                rowWallet.visibility = View.GONE
                tvStatus.visibility  = View.VISIBLE
                btnSend.text = "✓"
                btnSend.isEnabled = false
                btnSend.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.green_primary))
            }
            ChipState.FAILED -> {
                rowMain.visibility   = View.VISIBLE
                rowWallet.visibility = View.GONE
                tvStatus.visibility  = View.VISIBLE
                btnSend.text = getString(R.string.send_label)
                btnSend.isEnabled = true
                btnSend.backgroundTintList =
                    ColorStateList.valueOf(getColor(R.color.blue_primary))
            }
        }
    }

    private fun executeSend(
        address: String,
        amount: String,
        btnSend: MaterialButton,
        tvStatus: TextView,
        rowMain: LinearLayout,
        rowWallet: LinearLayout
    ) {
        isSending = true
        applyChipState(ChipState.SENDING, rowMain, rowWallet, btnSend, tvStatus)

        TransactionService.sendUSDC(address, amount) { success, txHash ->
            mainHandler.post {
                isSending = false
                if (success) {
                    currentInputConnection?.commitText(
                        getString(R.string.payment_success, amount), 1
                    )
                    tvStatus.text = "✅  Sent $amount USDC — ${txHash?.take(10)}…"
                    tvStatus.setTextColor(getColor(R.color.green_primary))
                    applyChipState(ChipState.SUCCESS, rowMain, rowWallet, btnSend, tvStatus)
                    mainHandler.postDelayed({ dismissChip() }, 2200)
                } else {
                    tvStatus.text = "❌  Transaction failed — check balance & try again"
                    tvStatus.setTextColor(getColor(R.color.red_primary))
                    applyChipState(ChipState.FAILED, rowMain, rowWallet, btnSend, tvStatus)
                }
            }
        }
    }

    private fun dismissChip() {
        mainHandler.removeCallbacksAndMessages(null) // cancel auto-dismiss timer
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
        isSending = false
    }
}
