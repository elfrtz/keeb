package com.elfrtz.keeb.keyboard

import java.util.regex.Pattern

/**
 * Detects Ethereum wallet addresses using a simple regex.
 */
object AddressDetector {
    private val ETH_PATTERN = Pattern.compile("0x[a-fA-F0-9]{40}")

    /** Returns the first Ethereum address found in [text], or null. */
    fun detect(text: CharSequence): String? {
        val m = ETH_PATTERN.matcher(text)
        return if (m.find()) m.group() else null
    }
}
