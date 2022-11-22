/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.AppPinAuthenticator
import org.forgerock.android.auth.EncryptedFileKeyStore
import org.forgerock.android.auth.KeyStoreRepository
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.interfaces.RSAPublicKey
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Device Authenticator which use Application PIN to secure device cryptography keys
 */
open class ApplicationPinDeviceAuthenticator(private val keyStore: KeyStoreRepository = EncryptedFileKeyStore()) :

    CryptoAware, DeviceAuthenticator, KeyStoreRepository {

    @VisibleForTesting
    internal lateinit var appPinAuthenticator: AppPinAuthenticator

    private val pinSuffix = "_PIN"

    @VisibleForTesting
    internal val pinRef = AtomicReference<CharArray>()

    @VisibleForTesting
    internal val worker = Executors.newSingleThreadScheduledExecutor()

    override fun generateKeys(context: Context, callback: (KeyPair) -> Unit) {
        requestForCredentials { pin ->
            pinRef.set(pin)
            //This allow a user to have an biometric key + application pin
            val alias = appPinAuthenticator.getKeyAlias() + pinSuffix
            val keyPair = appPinAuthenticator.generateKeys(context, pin)
            //Clean up the password from memory after 1 second
            worker.schedule({
                pinRef.set(null)
            }, 1, TimeUnit.SECONDS)
            callback(KeyPair(keyPair.public as RSAPublicKey, keyPair.private, alias))
        }
    }

    override fun authenticate(context: Context,
                              timeout: Int,
                              statusResult: (DeviceBindingStatus<PrivateKey>) -> Unit) {
        val pin = pinRef.getAndSet(null)
        pin?.let { getPrivateKey(context, it, statusResult) } ?: run {
            val startTime = Date().time
            requestForCredentials {
                val endTime = TimeUnit.MILLISECONDS.toSeconds(Date().time - startTime)
                if (endTime > (timeout.toLong())) {
                    statusResult(Timeout())
                } else {
                    getPrivateKey(context, it, statusResult)
                }
            }
        }
    }

    override fun isSupported(context: Context): Boolean {
        return true
    }

    /**
     * Request for user credential
     * @param fragmentActivity The current [FragmentActivity]
     * @param onCredentialsReceived Function to invoke after receiving the user pin
     */
    open fun requestForCredentials(fragmentActivity: FragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity(),
                                   onCredentialsReceived: (CharArray) -> Unit) {
        ApplicationPinFragment.newInstance().apply {
            this.onPinReceived = {
                onCredentialsReceived(it.toCharArray())
            }
            this.onCancelled = {
                onCredentialsReceived(CharArray(0))
            }
            this.show(fragmentActivity.supportFragmentManager, DeviceBindFragment.TAG)
        }
    }

    /**
     * Retrieve the Keystore Type, default to [KeyStore.getDefaultType]
     */
    private fun getPrivateKey(context: Context,
                              pin: CharArray,
                              statusResult: (DeviceBindingStatus<PrivateKey>) -> Unit) {
        if (pin.isNotEmpty()) {
            try {
                appPinAuthenticator.getPrivateKey(context, pin)?.let {
                    statusResult(Success(it))
                } ?: UnRegister()

            } catch (e: FileNotFoundException) {
                statusResult(UnRegister())
                return
            } catch (e: UnrecoverableKeyException) {
                statusResult(UnAuthorize())
            }
        } else {
            statusResult(Abort())
        }
    }

    final override fun setKey(cryptoKey: CryptoKey) {
        appPinAuthenticator = AppPinAuthenticator(cryptoKey, this)
    }

    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.APPLICATION_PIN

    override fun getInputStream(context: Context, identifier: String): InputStream {
        return keyStore.getInputStream(context, identifier)
    }

    override fun getOutputStream(context: Context, identifier: String): OutputStream {
        return keyStore.getOutputStream(context, identifier)
    }

    override fun getKeystoreType(): String {
        return keyStore.getKeystoreType()
    }

}