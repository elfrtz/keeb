package com.elfrtz.keeb.wallet

import android.content.Context
import android.util.Log
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

/**
 * Manages the demo wallet and web3j connection.
 * Uses a hard-coded private key for hackathon demo purposes.
 */
object WalletManager {
    private const val TAG = "WalletManager"

    lateinit var web3: Web3j
        private set
    lateinit var credentials: Credentials
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        web3 = Web3j.build(HttpService(BaseConfig.RPC_URL))
        credentials = Credentials.create(BaseConfig.DEMO_PRIVATE_KEY)
        initialized = true
        Log.d(TAG, "Wallet initialized: ${credentials.address}")
    }

    fun getAddress(): String = credentials.address
}
