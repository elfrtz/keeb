package com.elfrtz.keeb.keyboard

import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Monitors the system clipboard for Ethereum addresses.
 *
 * Strategy:
 * - Primary: OnPrimaryClipChangedListener (event-driven, works on all API levels)
 * - Fallback: 500ms polling (catches addresses copied before the keyboard was active)
 *
 * Note: On Android 13+ (API 33), clipboard reads are restricted to the focused
 * IME or foreground app. Since Keeb IS the active IME, reads succeed while the
 * keyboard is visible.
 */
class ClipboardMonitor(
    private val clipboard: ClipboardManager,
    private val onAddress: (String) -> Unit
) {
    companion object {
        private const val TAG = "ClipboardMonitor"
        private const val POLL_MS = 500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastSeen: String? = null
    private var running = false

    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
        checkClipboard()
    }

    private val poller = object : Runnable {
        override fun run() {
            if (!running) return
            checkClipboard()
            handler.postDelayed(this, POLL_MS)
        }
    }

    private fun checkClipboard() {
        try {
            val clip = clipboard.primaryClip
            val text = clip?.getItemAt(0)?.text?.toString()
            if (text != null && text != lastSeen) {
                lastSeen = text
                AddressDetector.detect(text)?.let(onAddress)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Clipboard read failed", e)
        }
    }

    fun start() {
        if (running) return  // guard against double-start on keyboard show/hide cycles
        running = true
        clipboard.addPrimaryClipChangedListener(clipListener)
        handler.post(poller)
        Log.d(TAG, "started")
    }

    fun stop() {
        if (!running) return
        running = false
        clipboard.removePrimaryClipChangedListener(clipListener)
        handler.removeCallbacks(poller)
        Log.d(TAG, "stopped")
    }
}
