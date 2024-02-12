/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.devicebind

import androidx.biometric.BiometricManager.Authenticators.*
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.CryptoObject
import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.biometric.BiometricAuth
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType


/**
 * Interface to display biometric and verify the device supported for biometric
 */

interface BiometricHandler {

    /**
     * check support for Biometric and device credential
     * @param strongAuthenticators accept different strong authenticators like BIOMETRIC_STRONG
     * @param weakAuthenticators accept different strong authenticators like BIOMETRIC_WEAK
     */
    fun isSupported(strongAuthenticators: Int = BIOMETRIC_STRONG,
                    weakAuthenticators: Int = BIOMETRIC_WEAK): Boolean

    fun isSupportedBiometricStrong(): Boolean

    /**
     * display biometric prompt  for Biometric and device credential
     * @param authenticationCallback Result of biometric action in callback
     */
    fun authenticate(authenticationCallback: AuthenticationCallback, cryptoObject: CryptoObject? = null)

}

/**
 * Helper class for managing Biometric configuration.
 * @param titleValue Title displayed in biometric prompt
 * @param subtitleValue Subtitle displayed in biometric prompt
 * @param descriptionValue  Description displayed in biometric prompt
 * @param fragmentActivity Activity that uses biometric to be displayed
 * @param biometricListener callback for biometric success or failure
 * @param biometricAuth Return the BiometricAuth instance
 */
internal class BiometricBindingHandler(titleValue: String,
                                       subtitleValue: String,
                                       descriptionValue: String,
                                       fragmentActivity: FragmentActivity = InitProvider.getCurrentActivityAsFragmentActivity(),
                                       deviceBindAuthenticationType: DeviceBindingAuthenticationType,
                                       var biometricListener: AuthenticationCallback? = null,
                                       private var biometricAuth: BiometricAuth? = null) :
    BiometricHandler {

    init {
        biometricAuth = biometricAuth ?: BiometricAuth(titleValue,
            subtitleValue,
            deviceBindAuthenticationType == DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK,
            fragmentActivity,
            biometricListener,
            descriptionValue)
    }

    /**
     * display biometric prompt  for Biometric and device credential
     * @param authenticationCallback Result of biometric action in callback
     */
    override fun authenticate(authenticationCallback: AuthenticationCallback, cryptoObject: CryptoObject?) {

        biometricListener = authenticationCallback
        biometricAuth?.biometricAuthListener = authenticationCallback
        biometricAuth?.authenticate(cryptoObject)
    }

    /**
     * check support for Biometric and device credential
     * @param strongAuthenticators accept different strong authenticators like BIOMETRIC_STRONG
     * @param weakAuthenticators accept different strong authenticators like BIOMETRIC_WEAK
     */
    override fun isSupported(strongAuthenticators: Int, weakAuthenticators: Int): Boolean {
        biometricAuth?.apply {
            when {
                // API 29 and above, check the support for STRONG type
                this.hasBiometricCapability(strongAuthenticators) -> {
                    return true
                }
                // API 29 and above, use BiometricPrompt for WEAK type
                this.hasBiometricCapability(weakAuthenticators) -> {
                    return true
                }
                // API 23 - 28, check enrollment with FingerprintManager once BiometricPrompt might not work
                this.hasEnrolledWithFingerPrint() -> {
                    return true
                }
                // API 23 - 28, using keyguard manager to verify and Display Device credential screen to enter pin
                weakAuthenticators == BIOMETRIC_WEAK or DEVICE_CREDENTIAL && this.hasDeviceCredential() -> {
                    return true
                }
            }
        }
        return false
    }

    override fun isSupportedBiometricStrong() =
        biometricAuth?.hasBiometricCapability(BIOMETRIC_STRONG) ?: false
}