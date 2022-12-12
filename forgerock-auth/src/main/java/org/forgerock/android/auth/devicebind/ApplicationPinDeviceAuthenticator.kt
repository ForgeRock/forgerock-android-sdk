/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.OperationCanceledException
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.forgerock.android.auth.AppPinAuthenticator
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.EncryptedFileKeyStore
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.KeyStoreRepository
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import org.forgerock.android.auth.devicebind.DeviceBindingErrorStatus.*
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
open class ApplicationPinDeviceAuthenticator : CryptoAware, DeviceAuthenticator,
    KeyStoreRepository {

    @VisibleForTesting
    internal lateinit var appPinAuthenticator: AppPinAuthenticator
    private lateinit var keyStore: KeyStoreRepository

    private val pinSuffix = "_PIN"

    protected lateinit var prompt: Prompt

    @VisibleForTesting
    internal val pinRef = AtomicReference<CharArray>()

    internal val worker = Executors.newSingleThreadScheduledExecutor()

    override suspend fun generateKeys(context: Context): KeyPair {
        val pin = requestForCredentials()
        pinRef.set(pin)
        //This allow a user to have an biometric key + application pin
        val alias = appPinAuthenticator.getKeyAlias() + pinSuffix
        val keyPair = appPinAuthenticator.generateKeys(context, pin)
        //Clean up the password from memory after 1 second
        worker.schedule({
            pinRef.set(null)
        }, 1, TimeUnit.SECONDS)
        return KeyPair(keyPair.public as RSAPublicKey, keyPair.private, alias)
    }

    override suspend fun authenticate(context: Context): DeviceBindingStatus {

        if (!appPinAuthenticator.exists(context)) {
            return UnRegister()
        }

        var pin = pinRef.getAndSet(null)
        pin?.let {
            return getPrivateKey(context, it)
        }
        return try {
            pin = requestForCredentials()
            getPrivateKey(context, pin)
        } catch (e: OperationCanceledException) {
            Abort()
        }
    }

    override fun prompt(prompt: Prompt) {
        this.prompt = prompt
    }

    override fun isSupported(context: Context): Boolean {
        return true
    }

    /**
     * Request for user credential
     * @param fragmentActivity The current [FragmentActivity]
     * @throws throw [OperationCanceledException] will return [Abort] status
     */
    open suspend fun requestForCredentials(fragmentActivity: FragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity()): CharArray =
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val existing =
                    fragmentActivity.supportFragmentManager.findFragmentByTag(ApplicationPinFragment.TAG) as? ApplicationPinFragment
                existing?.let {
                    existing.continuation = continuation
                } ?: run {
                    ApplicationPinFragment.newInstance(prompt, continuation)
                        .show(fragmentActivity.supportFragmentManager, ApplicationPinFragment.TAG)
                }
            }
        }

    /**
     * Retrieve the Keystore Type, default to [KeyStore.getDefaultType]
     */
    private fun getPrivateKey(context: Context, pin: CharArray): DeviceBindingStatus {
        return try {
            appPinAuthenticator.getPrivateKey(context, pin)?.let { Success(it) } ?: UnRegister()
        } catch (e: FileNotFoundException) {
            UnRegister()
        } catch (e: UnrecoverableKeyException) {
            UnAuthorize()
        }
    }

    final override fun setKey(cryptoKey: CryptoKey) {
        keyStore = EncryptedFileKeyStore(cryptoKey.keyAlias)
        appPinAuthenticator = AppPinAuthenticator(cryptoKey, this)
    }

    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.APPLICATION_PIN

    override fun getInputStream(context: Context): InputStream {
        return keyStore.getInputStream(context)
    }

    override fun getOutputStream(context: Context): OutputStream {
        return keyStore.getOutputStream(context)
    }

    override fun getKeystoreType(): String {
        return keyStore.getKeystoreType()
    }

    override fun delete(context: Context) {
        keyStore.delete(context)
    }

}