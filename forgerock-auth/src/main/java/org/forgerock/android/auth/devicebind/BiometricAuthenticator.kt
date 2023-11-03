/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.forgerock.android.auth.CryptoKey
import java.security.PrivateKey
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

abstract class BiometricAuthenticator : CryptoAware, DeviceAuthenticator {

    @VisibleForTesting
    internal var isApi30OrAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    internal lateinit var cryptoKey: CryptoKey
    internal lateinit var biometricInterface: BiometricHandler

    final override fun setKey(cryptoKey: CryptoKey) {
        this.cryptoKey = cryptoKey
    }

    fun setBiometricHandler(biometricHandler: BiometricHandler) {
        this.biometricInterface = biometricHandler
    }

    override fun deleteKeys(context: Context) {
        cryptoKey.deleteKeys()
    }

    /**
     * Display biometric prompt for authentication type
     * @param context Application Context
     * @return statusResult Listener for receiving Biometric changes
     */
    override suspend fun authenticate(context: Context): DeviceBindingStatus =
        withContext(Dispatchers.Main) {
            suspendCoroutine { continuation ->
                //The keys may be removed due to pin change
                val privateKey = cryptoKey.getPrivateKey()
                if (privateKey == null) {
                    continuation.resume(DeviceBindingErrorStatus.ClientNotRegistered())
                } else {
                    val listener = object : BiometricPrompt.AuthenticationCallback() {

                        override fun onAuthenticationError(errorCode: Int,
                                                           errString: CharSequence) {
                            when (errorCode) {
                                BiometricPrompt.ERROR_CANCELED, BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_NEGATIVE_BUTTON -> continuation.resume(
                                    DeviceBindingErrorStatus.Abort(errString.toString(),
                                        code = errorCode))

                                BiometricPrompt.ERROR_TIMEOUT -> continuation.resume(
                                    DeviceBindingErrorStatus.Timeout(
                                        errString.toString(),
                                        code = errorCode))

                                BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL, BiometricPrompt.ERROR_HW_NOT_PRESENT -> continuation.resume(
                                    DeviceBindingErrorStatus.Unsupported(errString.toString(),
                                        code = errorCode))

                                BiometricPrompt.ERROR_VENDOR -> continuation.resume(
                                    DeviceBindingErrorStatus.Unsupported(
                                        errString.toString(),
                                        code = errorCode))

                                BiometricPrompt.ERROR_LOCKOUT_PERMANENT, BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_NO_SPACE, BiometricPrompt.ERROR_HW_UNAVAILABLE, BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> continuation.resume(
                                    DeviceBindingErrorStatus.UnAuthorize(errString.toString(),
                                        code = errorCode))

                                else -> {
                                    continuation.resume(DeviceBindingErrorStatus.Unknown(errString.toString(),
                                        code = errorCode))
                                }
                            }
                        }

                        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                            result.cryptoObject?.signature?.let {
                                continuation.resume(Success(privateKey, it))
                                return
                            }
                            continuation.resume(Success(privateKey))
                        }

                        override fun onAuthenticationFailed() {
                            //Ignore with wrong fingerprint
                        }
                    }

                    authenticate(listener, privateKey)
                }
            }
        }

    /**
     * Launch the Biometric Prompt.
     * @param authenticationCallback [BiometricPrompt.AuthenticationCallback] to handle the result.
     * @param privateKey The private key to unlock
     */
    abstract fun authenticate(authenticationCallback: BiometricPrompt.AuthenticationCallback,
                              privateKey: PrivateKey)
}