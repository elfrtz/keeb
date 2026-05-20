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
 * Supports two modes:
 *  1. DEMO mode  — uses a hard-coded private key from BaseConfig (for hackathon demo)
 *  2. CONNECTED  — user connected MetaMask via deep-link; address stored in prefs
 *
 * The keyboard and the main app both read from this singleton, so wallet state
 * is always consistent across both surfaces.
 */
object WalletManager {
    private const val TAG = "WalletManager"
    private const val PREFS_NAME = "keeb_wallet"
    private const val KEY_ADDRESS = "connected_address"
    private const val KEY_MODE    = "wallet_mode"
    private const val MODE_DEMO   = "demo"
    private const val MODE_METAMASK = "metamask"

    // MetaMask deep-link scheme for connection
    // Opens MetaMask and requests the user to share their address
    private const val METAMASK_DEEP_LINK = "metamask://connect"
    private const val METAMASK_PACKAGE   = "io.metamask"

    lateinit var web3: Web3j
        private set
    lateinit var credentials: Credentials
        private set

    private var prefs: SharedPreferences? = null
    private var initialized = false

    // ── Connection state ───────────────────────────────────────

    enum class ConnectionState { DISCONNECTED, DEMO, CONNECTED }

    var connectionState: ConnectionState = ConnectionState.DISCONNECTED
        private set

    /** The active wallet address — either demo or MetaMask-connected. */
    val activeAddress: String?
        get() = when (connectionState) {
            ConnectionState.DEMO      -> if (::credentials.isInitialized) credentials.address else null
            ConnectionState.CONNECTED -> prefs?.getString(KEY_ADDRESS, null)
            ConnectionState.DISCONNECTED -> null
        }

    /** Short display form: 0x1234…abcd */
    val shortAddress: String?
        get() = activeAddress?.let { addr ->
            if (addr.length >= 10) addr.take(6) + "…" + addr.takeLast(4) else addr
        }

    val isConnected: Boolean
        get() = connectionState != ConnectionState.DISCONNECTED

    // ── Initialization ─────────────────────────────────────────

    /**
     * Initialize web3j and restore persisted wallet state.
     * Safe to call multiple times — idempotent after first call.
     * MUST be called off the main thread (web3j creates thread pools).
     */
    fun init(context: Context) {
        if (initialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        web3 = Web3j.build(HttpService(BaseConfig.RPC_URL))

        // Restore persisted state
        val savedMode = prefs?.getString(KEY_MODE, null)
        when (savedMode) {
            MODE_METAMASK -> {
                val savedAddress = prefs?.getString(KEY_ADDRESS, null)
                if (savedAddress != null) {
                    connectionState = ConnectionState.CONNECTED
                    Log.d(TAG, "Restored MetaMask wallet: $savedAddress")
                } else {
                    initDemoWallet()
                }
            }
            else -> initDemoWallet()
        }

        initialized = true
        Log.d(TAG, "WalletManager initialized — state: $connectionState, address: $activeAddress")
    }

    private fun initDemoWallet() {
        if (BaseConfig.DEMO_PRIVATE_KEY == "YOUR_PRIVATE_KEY_HERE") {
            Log.w(TAG, "Demo private key not configured — wallet in DISCONNECTED state")
            connectionState = ConnectionState.DISCONNECTED
            return
        }
        try {
            credentials = Credentials.create(BaseConfig.DEMO_PRIVATE_KEY)
            connectionState = ConnectionState.DEMO
            Log.d(TAG, "Demo wallet active: ${credentials.address}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create demo credentials", e)
            connectionState = ConnectionState.DISCONNECTED
        }
    }

    // ── MetaMask connection ────────────────────────────────────

    /**
     * Launch MetaMask deep-link to request wallet connection.
     * After the user approves in MetaMask, they return to the app.
     * The app must then call [onMetaMaskAddressReceived] with the address.
     *
     * If MetaMask is not installed, opens Play Store.
     */
    fun launchMetaMaskConnect(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(METAMASK_DEEP_LINK)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val canOpen = context.packageManager.queryIntentActivities(intent, 0).isNotEmpty()
        if (canOpen) {
            context.startActivity(intent)
        } else {
            // MetaMask not installed — open Play Store
            val playIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$METAMASK_PACKAGE")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(playIntent)
        }
    }

    /**
     * Called after the user returns from MetaMask with their address.
     * Persists the connection and updates state.
     */
    fun onMetaMaskAddressReceived(address: String) {
        prefs?.edit()
            ?.putString(KEY_ADDRESS, address)
            ?.putString(KEY_MODE, MODE_METAMASK)
            ?.apply()
        connectionState = ConnectionState.CONNECTED
        Log.d(TAG, "MetaMask connected: $address")
    }

    /**
     * Disconnect the current wallet.
     * Reverts to demo mode if a demo key is configured, otherwise DISCONNECTED.
     */
    fun disconnect(context: Context) {
        prefs?.edit()?.remove(KEY_ADDRESS)?.remove(KEY_MODE)?.apply()
        connectionState = ConnectionState.DISCONNECTED
        // Re-init demo wallet if available
        initDemoWallet()
        Log.d(TAG, "Wallet disconnected — state: $connectionState")
    }

    // ── Compatibility shim ─────────────────────────────────────

    /** Returns the active address — used by existing code. */
    fun getAddress(): String = activeAddress ?: "Not connected"
}
