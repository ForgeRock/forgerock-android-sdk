/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.biometric

import android.app.KeyguardManager
import android.content.Context
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import org.forgerock.android.auth.Logger.Companion.debug

/**
 * Helper class for managing Biometric Authentication Process.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
/**
 * Initializes Biometric authentication with the caller and callback handlers.
 * If device has no Biometric enrolled, it fallback to device credentials authentication.
 * @param title the title to be displayed on the prompt.
 * @param subtitle the subtitle to be displayed on the prompt.
 * @param allowDeviceCredentials if `true`, accepts device PIN, pattern, or password to process notification.
 * @param activity the activity of the client application that will host the prompt.
 * @param biometricAuthListener listener for receiving the biometric authentication result.
 * @param description the description to be displayed on the prompt.
 * @param authenticatorType The type of biometric authenticator to be displayed.
 */
class BiometricAuth @JvmOverloads constructor(
    /**
     * The title to be displayed on the prompt.
     * @return the Title as String.
     */
    val title: String?,
    /**
     * The subtitle to be displayed on the prompt.
     * @return the Subtitle as String.
     */
    val subtitle: String?,

    private val allowDeviceCredentials: Boolean,
    /**
     * Return the activity of the client application that will host the prompt.
     * @return the Activity that hosts the prompt.
     */
    val activity: FragmentActivity,

    /**
     * Return the Biometric Authentication listener used to return authentication result.
     * @return the Biometric Authentication listener.
     */
    var biometricAuthListener: AuthenticationCallback? = null,

    /**
     * The description to be displayed on the prompt.
     * @return the description as String.
     */
    private val description: String? = null,

    /**
     * The type of authenticator to be displayed.
     * @return the authenticatorType as Int.
     */
    var authenticatorType: AuthenticatorType = AuthenticatorType.WEAK
) {
    private var biometricManager: BiometricManager? = null

    private var fingerprintManager: FingerprintManager? = null

    /**
     * Return the mechanism used to lock and unlock the device.
     * @return the KeyguardManager instance.
     */
    var keyguardManager: KeyguardManager? = null
        private set
    private var promptInfo: BiometricPrompt.PromptInfo? = null


    private fun handleError(logMessage: String, biometricErrorMessage: String, errorType: Int) {
        debug(TAG, logMessage)
        biometricAuthListener?.onAuthenticationError(
            errorType, biometricErrorMessage)
    }

    private fun tryDisplayBiometricOnlyPrompt() {
        if (hasBiometricCapability()) {
            initBiometricAuthentication()
        } else {
            handleError("allowDeviceCredentials is set to false, but no biometric " +
                    "hardware found or enrolled." ,"It requires " +
                    "biometric authentication. No biometric hardware found or enrolled.", BiometricPrompt.ERROR_NO_BIOMETRICS)
        }
    }

    /*
     * Starts authentication process.
     */
    fun authenticate() {
        // if biometric only, try biometric prompt
        if (!allowDeviceCredentials) {
            tryDisplayBiometricOnlyPrompt()
            return
        }

        // API 29 and above, use BiometricPrompt
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            initBiometricAuthentication()
            return
        }

        // API 23 - 28, check enrollment with FingerprintManager once BiometricPrompt might not work
        if(hasEnrolledWithFingerPrint()) {
            initBiometricAuthentication()
            return
        }

        // API 23 or higher, no biometric, fallback to device credentials
        if (hasDeviceCredential()) {
            initDeviceCredentialAuthentication()
        } else {
            handleError("This device does not support required security features." +
                    " No Biometric, device PIN, pattern, or password registered." ,"This device does " +
                    "not support required security features. No Biometric, device PIN, pattern, " +
                    "or password registered.", BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL)
        }
    }

    // API 23 - 28, check enrollment with FingerprintManager once BiometricPrompt might not work
    fun hasEnrolledWithFingerPrint(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && fingerprintManager != null && fingerprintManager?.hasEnrolledFingerprints() == true
    }

    // API 23 or higher, no biometric, fallback to device credentials
    fun hasDeviceCredential(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && keyguardManager != null && keyguardManager?.isDeviceSecure == true
    }

    // validate the biometric capability for given type and return Boolean
    fun hasBiometricCapability(authenticators: Int): Boolean {
        if (biometricManager == null) return false
        val canAuthenticate =
            biometricManager?.canAuthenticate(authenticators)
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun setServicesFromActivity(activity: FragmentActivity) {
        val context = activity.baseContext
        biometricManager = BiometricManager.from(activity)
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            fingerprintManager =
                context.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
        }
    }

    private fun initBiometricPrompt(): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = biometricAuthListener?.let {
            BiometricPrompt(activity, executor, it)
        } ?: kotlin.run {
            throw IllegalStateException("AuthenticationCallback is not set.")
        }

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title ?: "Biometric Authentication for login")
            .setSubtitle(subtitle ?: "Log in using your biometric credential")
        val authenticators: Int
        if (allowDeviceCredentials) {
            authenticators = if(authenticatorType == AuthenticatorType.WEAK) {
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            } else {
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            }
        } else {
            authenticators = if(authenticatorType == AuthenticatorType.WEAK) {
                BiometricManager.Authenticators.BIOMETRIC_WEAK
            } else {
                BiometricManager.Authenticators.BIOMETRIC_STRONG
            }
            builder.setNegativeButtonText("Cancel")
        }
        description?.let {
            builder.setDescription(it)
        }
       builder.setAllowedAuthenticators(authenticators)
        promptInfo = builder.build()
        return biometricPrompt
    }

    private fun hasBiometricCapability(): Boolean {
        if (biometricManager == null) return false
        val canAuthenticate =
            biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun initBiometricAuthentication() {
        val biometricPrompt = initBiometricPrompt()
        promptInfo?.let {
            activity.runOnUiThread() {
                biometricPrompt.authenticate(it)
            }
        }
    }

    private fun initDeviceCredentialAuthentication() {
        DeviceCredentialFragment.launch(this)
    }

    companion object {
        private val TAG = BiometricAuth::class.java.simpleName
    }

    init {
        setServicesFromActivity(activity)
    }
}

enum class AuthenticatorType {
    STRONG,
    WEAK
}