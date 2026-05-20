package com.elfrtz.keeb.wallet

/**
 * Network and contract constants for Base Sepolia.
 */
object BaseConfig {
    const val RPC_URL = "https://sepolia.base.org"
    const val CHAIN_ID = 84532L

    // USDC on Base Sepolia (official Circle test token)
    const val USDC_CONTRACT = "0x036CbD53842c5426634e7929541eC2318f3dCF7e"
    const val USDC_DECIMALS = 6

    // ⚠️ DEMO ONLY — replace with a funded test wallet private key
    const val DEMO_PRIVATE_KEY = "YOUR_PRIVATE_KEY_HERE"
}
