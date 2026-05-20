package com.elfrtz.keeb.keyboard

import android.os.Handler
import android.os.Looper

/**
 * Handles long-press repeat deletion, matching standard Android keyboard behavior.
 *
 * Behavior:
 * - First delete fires immediately on long-press trigger
 * - Subsequent deletes fire every [REPEAT_INTERVAL_MS] ms
 * - Stops immediately when [stop] is called (key released)
 *
 * Usage:
 *   In your long-press listener: repeater.start { deleteOneChar() }
 *   In your touch-up / cancel:   repeater.stop()
 */
class DeleteKeyRepeater {

    companion object {
        private const val INITIAL_DELAY_MS = 400L   // delay before repeat starts
        private const val REPEAT_INTERVAL_MS = 50L  // interval between repeats (~20/sec)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var deleteAction: (() -> Unit)? = null

    private val repeatRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            deleteAction?.invoke()
            handler.postDelayed(this, REPEAT_INTERVAL_MS)
        }
    }

    /**
     * Start repeating deletion.
     * [action] is called immediately, then repeatedly after [INITIAL_DELAY_MS].
     */
    fun start(action: () -> Unit) {
        stop()  // cancel any previous run
        isRunning = true
        deleteAction = action
        action()  // fire immediately
        handler.postDelayed(repeatRunnable, INITIAL_DELAY_MS)
    }

    /**
     * Stop repeating. Call this on touch-up or touch-cancel.
     */
    fun stop() {
        isRunning = false
        handler.removeCallbacks(repeatRunnable)
        deleteAction = null
    }

    /**
     * Clean up — call from onDestroy to prevent handler leaks.
     */
    fun destroy() {
        stop()
    }
}
