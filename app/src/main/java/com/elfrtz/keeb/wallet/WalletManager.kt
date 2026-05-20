package com.elfrtz.keeb.wallet

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.http.HttpService

/**
 * Central wallet state manager — singleton shared between the app and the IME.
 *
 * Signing uses a Base Sepolia private key stored on-device (demo / test only).
 * MetaMask cannot auto-connect from a custom keyboard without the MetaMask Android SDK;
 * users export their test-account key from MetaMask and paste it here.
 */
object WalletManager {
    private const val TAG = "WalletManager"
    private const val PREFS_NAME = "keeb_wallet"
    private const val KEY_DEMO_PRIVATE_KEY = "demo_private_key"
    private const val METAMASK_PACKAGE = "io.metamask"

    lateinit var web3: Web3j
        private set

    var credentials: Credentials? = null
        private set

    private var prefs: SharedPreferences? = null
    private var initialized = false

    enum class ConnectionState { DISCONNECTED, DEMO }

    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    val activeAddress: String?
        get() = credentials?.address

    val shortAddress: String?
        get() = activeAddress?.let { addr ->
            if (addr.length >= 10) addr.take(6) + "…" + addr.takeLast(4) else addr
        }

    /** True when a private key is loaded and USDC sends are possible. */
    val canSend: Boolean
        get() = credentials != null

    val isConnected: Boolean
        get() = canSend

    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        web3 = Web3j.build(HttpService(BaseConfig.RPC_URL))
        loadWalletFromStorage()
        initialized = true
        Log.d(TAG, "WalletManager initialized — state: $connectionState, address: $activeAddress")
    }

    /** Re-read saved key when returning to the keyboard after wallet setup. */
    fun reload(context: Context) {
        if (!initialized) {
            init(context.applicationContext)
            return
        }
        loadWalletFromStorage()
        Log.d(TAG, "Wallet reloaded — state: $connectionState, address: $activeAddress")
    }

    private fun loadWalletFromStorage() {
        val savedKey = prefs?.getString(KEY_DEMO_PRIVATE_KEY, null)?.trim()
        if (!savedKey.isNullOrEmpty()) {
            applyPrivateKey(savedKey, persist = false)
            return
        }
        if (BaseConfig.DEMO_PRIVATE_KEY != "YOUR_PRIVATE_KEY_HERE") {
            applyPrivateKey(BaseConfig.DEMO_PRIVATE_KEY.trim(), persist = false)
        }
    }

    /**
     * Save a test private key (from MetaMask export or Base Sepolia faucet wallet).
     * @return error message, or null on success
     */
    fun setDemoPrivateKey(context: Context, privateKey: String): String? {
        if (!initialized) init(context.applicationContext)
        val normalized = normalizePrivateKey(privateKey)
        return try {
            val creds = Credentials.create(normalized)
            prefs?.edit()?.putString(KEY_DEMO_PRIVATE_KEY, normalized)?.apply()
            credentials = creds
            connectionState = ConnectionState.DEMO
            Log.d(TAG, "Wallet configured: ${creds.address}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Invalid private key", e)
            "Invalid private key. Paste the 64-character hex key from MetaMask."
        }
    }

    /** Opens the MetaMask app so the user can export their test account key. */
    fun openMetaMaskApp(context: Context): Boolean {
        val launch = context.packageManager.getLaunchIntentForPackage(METAMASK_PACKAGE)
        if (launch != null) {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        }
        val playIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://play.google.com/store/apps/details?id=$METAMASK_PACKAGE")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(playIntent)
        return false
    }

    fun disconnect(context: Context) {
        if (!initialized) init(context.applicationContext)
        prefs?.edit()?.remove(KEY_DEMO_PRIVATE_KEY)?.apply()
        credentials = null
        connectionState = ConnectionState.DISCONNECTED
        Log.d(TAG, "Wallet disconnected")
    }

    fun getAddress(): String = activeAddress ?: "Not connected"

    private fun applyPrivateKey(privateKey: String, persist: Boolean) {
        try {
            val normalized = normalizePrivateKey(privateKey)
            val creds = Credentials.create(normalized)
            credentials = creds
            connectionState = ConnectionState.DEMO
            if (persist) {
                prefs?.edit()?.putString(KEY_DEMO_PRIVATE_KEY, normalized)?.apply()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load wallet credentials", e)
            credentials = null
            connectionState = ConnectionState.DISCONNECTED
        }
    }

    private fun normalizePrivateKey(key: String): String {
        var k = key.trim()
        if (k.startsWith("0x", ignoreCase = true)) k = k.substring(2)
        return k
    }
}
