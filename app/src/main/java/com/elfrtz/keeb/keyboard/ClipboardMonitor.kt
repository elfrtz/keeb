package com.elfrtz.keeb.keyboard

import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Monitors the system clipboard for Ethereum addresses.
 *
 * Strategy:
 * - OnPrimaryClipChangedListener when the OS allows it
 * - Polling while the keyboard is visible
 * - [forceCheck] on keyboard open and when the user taps "Detect address"
 *
 * Many OEMs (e.g. Realme/Oppo) block silent clipboard reads for IMEs — the manual
 * detect button uses a direct user gesture and is more reliable.
 */
class ClipboardMonitor(
    private val context: Context,
    private val clipboard: ClipboardManager,
    private val onAddress: (String) -> Unit,
    private val onNoAddress: (() -> Unit)? = null
) {
    companion object {
        private const val TAG = "ClipboardMonitor"
        private const val POLL_MS = 400L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastSeen: String? = null
    private var running = false

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard(userInitiated = false)
    }

    private val poller = object : Runnable {
        override fun run() {
            if (!running) return
            checkClipboard(userInitiated = false)
            handler.postDelayed(this, POLL_MS)
        }
    }

    /** Read clipboard now; [userInitiated] helps on Android 12+ OEM restrictions. */
    fun forceCheck(userInitiated: Boolean = true): Boolean {
        // Do not clear lastSeen — avoids re-firing when the same address is already showing
        return checkClipboard(userInitiated)
    }

    private fun checkClipboard(userInitiated: Boolean): Boolean {
        try {
            if (!clipboard.hasPrimaryClip()) {
                if (userInitiated) onNoAddress?.invoke()
                return false
            }

            val clip = clipboard.primaryClip ?: run {
                if (userInitiated) onNoAddress?.invoke()
                return false
            }

            if (clip.itemCount == 0) {
                if (userInitiated) onNoAddress?.invoke()
                return false
            }

            val text = buildString {
                for (i in 0 until clip.itemCount) {
                    val item = clip.getItemAt(i) ?: continue
                    val part = item.coerceToText(context)?.toString() ?: item.text?.toString()
                    if (!part.isNullOrBlank()) {
                        if (isNotEmpty()) append('\n')
                        append(part.trim())
                    }
                }
            }

            if (text.isEmpty()) {
                if (userInitiated) onNoAddress?.invoke()
                return false
            }

            // Re-detect when user explicitly asks, even if clipboard text unchanged
            if (!userInitiated && text == lastSeen) return false
            lastSeen = text

            val address = AddressDetector.detect(text)
            if (address != null) {
                Log.d(TAG, "Address detected: $address")
                onAddress(address)
                return true
            }

            if (userInitiated) {
                Log.d(TAG, "Clipboard has text but no address: ${text.take(40)}…")
                onNoAddress?.invoke()
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Clipboard access denied (tap Detect address)", e)
            if (userInitiated) onNoAddress?.invoke()
        } catch (e: Exception) {
            Log.w(TAG, "Clipboard read failed", e)
            if (userInitiated) onNoAddress?.invoke()
        }
        return false
    }

    fun start() {
        if (!running) {
            running = true
            try {
                clipboard.addPrimaryClipChangedListener(clipListener)
            } catch (e: Exception) {
                Log.w(TAG, "Could not register clipboard listener", e)
            }
            handler.post(poller)
            Log.d(TAG, "started (sdk=${Build.VERSION.SDK_INT})")
        }
        handler.post { forceCheck(userInitiated = false) }
    }

    fun stop() {
        if (!running) return
        running = false
        try {
            clipboard.removePrimaryClipChangedListener(clipListener)
        } catch (_: Exception) {
        }
        handler.removeCallbacks(poller)
        Log.d(TAG, "stopped")
    }
}
