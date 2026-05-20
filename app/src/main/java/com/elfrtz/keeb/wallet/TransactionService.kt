package com.elfrtz.keeb.wallet

import android.util.Log
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.RawTransactionManager
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Sends USDC via a raw ERC-20 transfer() call on Base Sepolia.
 */
object TransactionService {
    private const val TAG = "TransactionService"

    // Reasonable gas values for Base Sepolia
    private val GAS_PRICE = BigInteger.valueOf(1_500_000_000L)  // 1.5 gwei
    private val GAS_LIMIT = BigInteger.valueOf(100_000L)

    /**
     * Sends [amountStr] USDC to [toAddress].
     * Runs on a background thread; invokes [callback] on completion.
     */
    fun sendUSDC(
        toAddress: String,
        amountStr: String,
        callback: (success: Boolean, txHash: String?) -> Unit
    ) {
        Thread {
            try {
                val web3 = WalletManager.web3
                val creds = WalletManager.credentials

                // Convert human amount to USDC units (6 decimals)
                val amount = BigDecimal(amountStr)
                    .multiply(BigDecimal.TEN.pow(BaseConfig.USDC_DECIMALS))
                    .toBigInteger()

                // Encode ERC-20 transfer(address,uint256)
                val function = Function(
                    "transfer",
                    listOf(Address(toAddress), Uint256(amount)),
                    listOf(object : TypeReference<Bool>() {})
                )
                val encodedData = FunctionEncoder.encode(function)

                // Build and send raw transaction
                val txManager = RawTransactionManager(
                    web3, creds, BaseConfig.CHAIN_ID
                )

                val txReceipt = txManager.sendTransaction(
                    GAS_PRICE,
                    GAS_LIMIT,
                    BaseConfig.USDC_CONTRACT,
                    encodedData,
                    BigInteger.ZERO // no ETH value
                )

                val txHash = txReceipt.transactionHash
                Log.d(TAG, "USDC sent! tx=$txHash")
                callback(true, txHash)

            } catch (e: Exception) {
                Log.e(TAG, "USDC transfer failed", e)
                callback(false, null)
            }
        }.start()
    }
}
