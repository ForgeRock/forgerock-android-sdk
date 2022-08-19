package org.forgerock.android.auth.devicebind

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
    fun isSupportedForBiometricOnly(): Boolean
    fun isSupportedForBiometricAndDeviceCredential(): Boolean
    fun setListener(listener: BiometricAuthCompletionHandler?)
    fun getBiometricListener(timeout: Int, statusResult: (BiometricStatus) -> Unit): BiometricAuthCompletionHandler
    fun authenticate()
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

    override fun getBiometricListener(timeout: Int, statusResult: (BiometricStatus) -> Unit): BiometricAuthCompletionHandler {
        val startTime = Date().time
        val biometricListener = object: BiometricAuthCompletionHandler {
            override fun onSuccess(result: BiometricPrompt.AuthenticationResult?) {
                val endTime =  TimeUnit.MILLISECONDS.toSeconds(Date().time - startTime)
                if(endTime > (timeout.toLong())) {
                    statusResult(BiometricStatus(false, "Timeout", BiometricTimeOutException("Biometric Timeout")))
                } else {
                    statusResult(BiometricStatus(true, "", null))
                }
            }
            override fun onError(errorCode: Int, errorMessage: String?) {
                statusResult(BiometricStatus(false, "Abort", BiometricErrorException("$errorCode: $errorMessage")))
            }
        }
        return biometricListener
    }

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

    override fun setListener(listener: BiometricAuthCompletionHandler?) {
        biometricListener = listener
        biometricAuth?.biometricAuthListener = biometricListener
    }

    override fun authenticate() {
        biometricAuth?.authenticate()
    }
}