package com.elfrtz.keeb.keyboard

import java.util.regex.Pattern

/**
 * Detects Ethereum wallet addresses using a simple regex.
 */
object AddressDetector {
    private val ETH_PATTERN = Pattern.compile("0x[a-fA-F0-9]{40}")
    private val HEX_ONLY_PATTERN = Pattern.compile("(?i)(?<![a-f0-9])[a-f0-9]{40}(?![a-f0-9])")

    /** Returns the first Ethereum address found in [text], or null. */
    fun detect(text: CharSequence): String? {
        val raw = text.toString().trim()
        if (raw.isEmpty()) return null

        ETH_PATTERN.matcher(raw).let { m ->
            if (m.find()) return m.group()
        }

        // Some apps copy without 0x prefix
        HEX_ONLY_PATTERN.matcher(raw).let { m ->
            if (m.find()) return "0x" + m.group().lowercase()
        }

        return null
    }
}
