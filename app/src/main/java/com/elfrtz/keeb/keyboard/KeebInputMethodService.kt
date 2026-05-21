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
import android.widget.Toast
import com.elfrtz.keeb.R
import com.elfrtz.keeb.SettingsActivity
import com.elfrtz.keeb.WalletActivity
import com.elfrtz.keeb.wallet.BaseConfig
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
    private var chipDismissGeneration = 0
    private val chipAutoDismissRunnable = Runnable { dismissChip(animated = true) }
    private var chipAmountEdit: EditText? = null
    private var amountFieldFocused = false

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

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        clipboardMonitor = ClipboardMonitor(
            context = applicationContext,
            clipboard = clipboard,
            onAddress = { address -> mainHandler.post { onAddressDetected(address) } },
            onNoAddress = {
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        R.string.keyboard_no_address_in_clipboard,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        )
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
        root.findViewById<LinearLayout>(R.id.btn_detect_address)?.setOnClickListener {
            clipboardMonitor.forceCheck(userInitiated = true)
        }

        refreshWalletDot()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        stateManager.onInputStarted(currentInputConnection)
        refreshShiftVisuals()
        Thread {
            try {
                WalletManager.reload(applicationContext)
            } catch (e: Exception) {
                Log.e(TAG, "Wallet reload failed", e)
            }
            mainHandler.post {
                refreshWalletDot()
                clipboardMonitor.start()
            }
        }.start()
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
        if (amountFieldFocused) {
            commitToAmountField(text)
            return
        }
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
        if (amountFieldFocused) {
            handleAmountBackspace()
            return
        }
        val ic = currentInputConnection ?: return
        ic.deleteSurroundingText(1, 0)
        stateManager.onBackspace(ic)
        refreshShiftVisuals()
    }

    fun handleEnter() {
        if (amountFieldFocused) {
            clearAmountFieldFocus()
            return
        }
        currentInputConnection?.commitText("\n", 1)
    }

    private fun commitToAmountField(text: String) {
        val et = chipAmountEdit ?: return
        if (!et.isEnabled) return
        when (text) {
            " " -> return
            "," -> appendAmountChar(et, ".")
            else -> {
                if (text.length == 1 && text[0].isLetter()) return
                for (ch in text) {
                    if (ch.isDigit()) appendAmountChar(et, ch.toString())
                    else if (ch == '.') appendAmountChar(et, ".")
                }
            }
        }
    }

    private fun appendAmountChar(et: EditText, ch: String) {
        val current = et.text?.toString() ?: ""
        if (ch == "." && current.contains('.')) return
        val start = et.selectionStart.coerceAtLeast(0)
        val end = et.selectionEnd.coerceAtLeast(0)
        val updated = StringBuilder(current).apply { replace(start, end, ch) }.toString()
        et.setText(updated)
        val newPos = (start + ch.length).coerceAtMost(updated.length)
        et.setSelection(newPos)
    }

    private fun handleAmountBackspace() {
        val et = chipAmountEdit ?: return
        val current = et.text?.toString() ?: return
        if (current.isEmpty()) return
        val start = et.selectionStart.coerceAtLeast(0)
        val end = et.selectionEnd.coerceAtLeast(0)
        if (start != end) {
            et.text?.delete(start, end)
        } else if (start > 0) {
            et.text?.delete(start - 1, start)
        }
        et.setSelection(et.text?.length?.coerceAtLeast(0) ?: 0)
    }

    private fun focusAmountField(et: EditText) {
        if (amountFieldFocused && chipAmountEdit === et) {
            et.post { et.selectAll() }
            return
        }
        chipAmountEdit = et
        amountFieldFocused = true
        et.requestFocus()
        et.post {
            et.selectAll()
            keyRowsContainer?.let { container ->
                KeyboardView.enterAmountMode(themedContext, this, container) { commitTyped(it) }
            }
        }
    }

    private fun clearAmountFieldFocus() {
        if (!amountFieldFocused) return
        amountFieldFocused = false
        chipAmountEdit?.clearFocus()
        keyRowsContainer?.let { container ->
            KeyboardView.exitAmountMode(themedContext, this, container) { commitTyped(it) }
        }
    }

    private fun pasteAmountFromClipboard() {
        val et = chipAmountEdit ?: return
        try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (!clipboard.hasPrimaryClip()) return
            val raw = clipboard.primaryClip?.getItemAt(0)?.coerceToText(applicationContext)?.toString()
                ?: return
            val cleaned = raw.filter { it.isDigit() || it == '.' }
            if (cleaned.isEmpty()) {
                Toast.makeText(applicationContext, R.string.keyboard_amount_paste_invalid, Toast.LENGTH_SHORT).show()
                return
            }
            val normalized = if (cleaned.count { it == '.' } > 1) {
                cleaned.substringBefore('.') + "." + cleaned.substringAfter('.').replace(".", "")
            } else cleaned
            et.setText(normalized)
            et.setSelection(normalized.length)
        } catch (e: Exception) {
            Log.w(TAG, "Amount paste failed", e)
        }
    }

    private fun setupAmountField(et: EditText) {
        et.showSoftInputOnFocus = false
        et.setOnClickListener { focusAmountField(et) }
        et.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (!amountFieldFocused) focusAmountField(et)
            } else if (amountFieldFocused) {
                clearAmountFieldFocus()
            }
        }
        et.setOnLongClickListener {
            focusAmountField(et)
            pasteAmountFromClipboard()
            true
        }
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
            WalletManager.ConnectionState.DEMO ->
                "🟢" to (WalletManager.shortAddress ?: "Ready")
            WalletManager.ConnectionState.DISCONNECTED ->
                "⚪" to "Wallet"
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
        if (address.equals(detectedAddress, ignoreCase = true) && chipView != null) return
        showPaymentChip(address)
    }

    // ── Payment chip ───────────────────────────────────────────

    private fun showPaymentChip(address: String) {
        if (address.equals(detectedAddress, ignoreCase = true) && chipView != null) return

        // Replace instantly — animated dismiss would remove the new chip when the old
        // fade-out animation finishes (~200ms), causing a brief flash then disappear.
        dismissChipImmediate()
        detectedAddress = address

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
        chipAmountEdit = etAmount
        setupAmountField(etAmount)
        configureSendButton(btnSend)

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

        btnClose.setOnClickListener {
            clearAmountFieldFocus()
            dismissChip(animated = true)
        }
        btnCloseWR.setOnClickListener {
            clearAmountFieldFocus()
            dismissChip(animated = true)
        }
        tvAddress.setOnClickListener { clearAmountFieldFocus() }

        // "Connect" inside the chip — launches WalletActivity then returns
        btnConnect.setOnClickListener {
            launchActivity(WalletActivity::class.java)
            // After user returns, onStartInputView fires and refreshes wallet dot.
            // If they connected, next Send tap will proceed normally.
        }

        chipContainer?.addView(chipView)
        chipContainer?.visibility = View.VISIBLE
        chipView!!.clearAnimation()
        chipView!!.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up))
    }

    /** Removes the chip immediately (no fade). Used when replacing or rebuilding. */
    private fun dismissChipImmediate() {
        mainHandler.removeCallbacks(chipAutoDismissRunnable)
        chipDismissGeneration++
        clearAmountFieldFocus()
        chipAmountEdit = null
        chipView?.clearAnimation()
        chipContainer?.removeAllViews()
        chipContainer?.visibility = View.GONE
        chipView = null
        isSending = false
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

    private fun configureSendButton(btnSend: MaterialButton) {
        val minPx = (76 * resources.displayMetrics.density).toInt()
        btnSend.minWidth = minPx
        btnSend.minHeight = (36 * resources.displayMetrics.density).toInt()
        btnSend.insetTop = 0
        btnSend.insetBottom = 0
        btnSend.isAllCaps = false
        btnSend.maxLines = 1
        btnSend.setPadding(
            (12 * resources.displayMetrics.density).toInt(),
            0,
            (12 * resources.displayMetrics.density).toInt(),
            0
        )
    }

    private fun buildPaymentSuccessMessage(amount: String, txHash: String?): String {
        if (txHash.isNullOrBlank()) {
            return getString(R.string.payment_success, amount)
        }
        return getString(
            R.string.payment_success_with_tx,
            amount,
            BaseConfig.explorerTxUrl(txHash)
        )
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
                        buildPaymentSuccessMessage(amount, txHash), 1
                    )
                    tvStatus.text = if (!txHash.isNullOrBlank()) {
                        "✅  Sent $amount USDC\n${BaseConfig.explorerTxUrl(txHash)}"
                    } else {
                        "✅  Sent $amount USDC"
                    }
                    tvStatus.setTextColor(getColor(R.color.green_primary))
                    applyChipState(ChipState.SUCCESS, rowMain, rowWallet, btnSend, tvStatus)
                    mainHandler.postDelayed(chipAutoDismissRunnable, 2200)
                } else {
                    tvStatus.text = "❌  Transaction failed — check balance & try again"
                    tvStatus.setTextColor(getColor(R.color.red_primary))
                    applyChipState(ChipState.FAILED, rowMain, rowWallet, btnSend, tvStatus)
                }
            }
        }
    }

    /** User-dismiss or auto-hide after success — animated fade out. */
    private fun dismissChip(animated: Boolean) {
        mainHandler.removeCallbacks(chipAutoDismissRunnable)
        val viewToRemove = chipView ?: run {
            dismissChipImmediate()
            detectedAddress = null
            return
        }
        chipView = null
        detectedAddress = null
        isSending = false

        if (!animated) {
            chipDismissGeneration++
            viewToRemove.clearAnimation()
            chipContainer?.removeAllViews()
            chipContainer?.visibility = View.GONE
            return
        }

        val generation = ++chipDismissGeneration
        val anim = AnimationUtils.loadAnimation(this, R.anim.fade_out)
        anim.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(a: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(a: android.view.animation.Animation?) {}
            override fun onAnimationEnd(a: android.view.animation.Animation?) {
                if (generation != chipDismissGeneration) return
                chipContainer?.removeAllViews()
                chipContainer?.visibility = View.GONE
            }
        })
        viewToRemove.startAnimation(anim)
    }
}
