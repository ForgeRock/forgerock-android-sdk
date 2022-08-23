/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.biometric.AuthenticatorType
import org.forgerock.android.auth.biometric.BiometricAuth
import org.forgerock.android.auth.biometric.BiometricAuthCompletionHandler
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.util.*
import java.util.concurrent.TimeUnit

interface BiometricInterface {
    /**
     * check support for Biometric only authentication type
     */
    fun isSupportedForBiometricOnly(): Boolean
    /**
     * check support for Biometric and device credential authentication type
     */
    fun isSupportedForBiometricAndDeviceCredential(): Boolean
    /**
     * set the biometric auth listener
     * @param listener Listener for receiving Biometric changes
     */
    fun setListener(listener: BiometricAuthCompletionHandler?)
    /**
     * Create biometric listener and produce the result
     * @param timeout Timeout for the biometric prompt.
     * @param statusResult Result of biometric action.
     */
    fun getBiometricListener(timeout: Int, statusResult: (DeviceBindingStatus) -> Unit): BiometricAuthCompletionHandler
    /**
     * To display biometric prompt
     */
    fun authenticate()
    /**
     * check the device running on R and above
     */
    fun isApi30AndAbove(): Boolean  {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    }
}

/**
 * Helper class for managing Biometric configuration.
 */
class BiometricUtil(titleValue: String,
                    subtitleValue: String,
                    descriptionValue: String,
                    fragmentActivity: FragmentActivity =  InitProvider.getCurrentActivityAsFragmentActivity(),
                    deviceBindAuthenticationType: DeviceBindingAuthenticationType,
                    private var biometricListener: BiometricAuthCompletionHandler? = null,
                    private var biometricAuth: BiometricAuth? = null): BiometricInterface {

    init {
        biometricAuth = biometricAuth ?: BiometricAuth(titleValue,
            subtitleValue,
            deviceBindAuthenticationType == DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK,
            fragmentActivity,
            biometricListener,
            descriptionValue)
    }


    override fun getBiometricListener(timeout: Int, statusResult: (DeviceBindingStatus) -> Unit): BiometricAuthCompletionHandler {
        val startTime = Date().time
        val biometricListener = object: BiometricAuthCompletionHandler {
            override fun onSuccess(result: BiometricPrompt.AuthenticationResult?) {
                val endTime =  TimeUnit.MILLISECONDS.toSeconds(Date().time - startTime)
                if(endTime > (timeout.toLong())) {
                    statusResult(Timeout())
                } else {
                    statusResult(Success)
                }
            }
            override fun onError(errorCode: Int, errorMessage: String?) {
                statusResult(Abort(errorMessage ?: "User Terminates the biometric Authentication", code = errorCode))
            }
        }
        return biometricListener
    }

    /**
     * check support for Biometric and device credential
     */
    override fun isSupportedForBiometricAndDeviceCredential(): Boolean {
        biometricAuth?.apply {
            when {
                // API 29 and above, check the support for STRONG type
                this.hasBiometricCapability(BIOMETRIC_STRONG or DEVICE_CREDENTIAL) -> {
                    this.authenticatorType = AuthenticatorType.STRONG
                    return true
                }
                // API 29 and above, use BiometricPrompt for WEAK type
                this.hasBiometricCapability(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) -> {
                    this.authenticatorType = AuthenticatorType.WEAK
                    return true
                }
                // API 23 - 28, check enrollment with FingerprintManager once BiometricPrompt might not work
                this.hasEnrolledWithFingerPrint() -> {
                    this.authenticatorType = AuthenticatorType.WEAK
                    return true
                }
                // API 23 - 28, using keyguard manager to verify and Display Device credential screen to enter pin
                this.hasDeviceCredential() -> {
                    return true
                }
            }
        }
        return false
    }

    /**
     * check support for Biometric only
     */
    override fun isSupportedForBiometricOnly(): Boolean {
        biometricAuth?.apply {
            when {
                // API 29 and above, First check BiometricPrompt for STRONG type
                this.hasBiometricCapability(BIOMETRIC_STRONG) -> {
                    this.authenticatorType = AuthenticatorType.STRONG
                    return true
                }

                // API 29 and above, use BiometricPrompt for WEAK type
                this.hasBiometricCapability(BIOMETRIC_WEAK) -> {
                    this.authenticatorType = AuthenticatorType.WEAK
                    return true
                }

                // API 23 - 28, check enrollment with FingerprintManager once BiometricPrompt might not work
                this.hasEnrolledWithFingerPrint() -> {
                    this.authenticatorType = AuthenticatorType.WEAK
                    return true
                }
                else -> return false
            }
        }
        return false
    }

    /**
     * set the biometric auth listener
     * @param listener Listener for receiving Biometric changes
     */
    override fun setListener(listener: BiometricAuthCompletionHandler?) {
        biometricListener = listener
        biometricAuth?.biometricAuthListener = biometricListener
    }


    /**
     * To display biometric prompt
     */
    override fun authenticate() {
        biometricAuth?.authenticate()
    }
}