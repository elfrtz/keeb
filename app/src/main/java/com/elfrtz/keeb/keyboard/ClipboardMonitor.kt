package com.elfrtz.keeb.keyboard

import android.content.ClipboardManager
import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * Polls the system clipboard every 500ms and notifies when
 * a new Ethereum address is detected.
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

    private val poller = object : Runnable {
        override fun run() {
            if (!running) return
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
            handler.postDelayed(this, POLL_MS)
        }
    }

    fun start() {
        running = true
        handler.post(poller)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(poller)
    }
}
