package org.forgerock.android.auth.devicebind

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.*
import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.InitProvider
import org.forgerock.android.auth.biometric.AuthenticatorType
import org.forgerock.android.auth.biometric.BiometricAuth
import org.forgerock.android.auth.biometric.BiometricAuthCompletionHandler
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType

interface BiometricInterface {
    fun isSupported(): Boolean
    fun setListener(listener: BiometricAuthCompletionHandler)
    fun authenticate()
}

/**
 * Helper class for managing Biometric configuration.
 */
class BiometricUtil(titleValue: String,
                    subtitleValue: String,
                    descriptionValue: String,
                    fragmentActivity: FragmentActivity =  InitProvider.getCurrentActivityAsFragmentActivity(),
                    private var deviceBindAuthenticationType: DeviceBindingAuthenticationType,
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

    override fun isSupported(): Boolean {
        biometricAuth?.apply {
            if(deviceBindAuthenticationType == DeviceBindingAuthenticationType.BIOMETRIC_ONLY) {
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

            else if(deviceBindAuthenticationType == DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK) {
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
        }
        return false
    }

    override fun setListener(listener: BiometricAuthCompletionHandler) {
        biometricListener = listener
        biometricAuth?.biometricAuthListener = biometricListener
    }

    override fun authenticate() {
        biometricAuth?.authenticate()
    }
}