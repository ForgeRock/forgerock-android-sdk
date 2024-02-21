/*
 * Copyright (c) 2022 - 2024 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.biometric

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.AuthenticationCallback
import androidx.biometric.BiometricPrompt.CryptoObject
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
            errorType, biometricErrorMessage
        )
    }

    /*
     * Starts authentication process.
     */
    fun authenticate(cryptoObject: CryptoObject? = null) {
        getAuthenticators()?.let {
            initBiometricAuthentication(cryptoObject, it)
        } ?: run {
            if (allowDeviceCredentials) {
                handleError(
                    "This device does not support required security features." +
                            " No Biometric, device PIN, pattern, or password registered.",
                    "This device does " +
                            "not support required security features. No Biometric, device PIN, pattern, " +
                            "or password registered.",
                    BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
                )
            } else {
                handleError(
                    "allowDeviceCredentials is set to false, but no biometric " +
                            "hardware found or enrolled.",
                    "It requires " +
                            "biometric authentication. No biometric hardware found or enrolled.",
                    BiometricPrompt.ERROR_NO_BIOMETRICS
                )
            }
        }
    }

    // API 23 - 28, check enrollment with FingerprintManager once BiometricPrompt might not work
    fun hasEnrolledWithFingerPrint(): Boolean {
        return fingerprintManager != null && fingerprintManager?.hasEnrolledFingerprints() == true
    }

    // API 23 or higher, no biometric, fallback to device credentials
    fun hasDeviceCredential(): Boolean {
        return keyguardManager != null && keyguardManager?.isDeviceSecure == true
    }

    // validate the biometric capability for given type and return Boolean
    fun hasBiometricCapability(authenticators: Int): Boolean {
        return biometricManager?.let {
            val canAuthenticate =
                biometricManager?.canAuthenticate(authenticators)
            return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        } ?: false
    }

    private fun getAuthenticators(): Int? {
        biometricManager?.let {
            if (allowDeviceCredentials) {
                if (hasBiometricCapability(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)) {
                    return BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                }
                if (hasBiometricCapability(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                    || hasEnrolledWithFingerPrint()
                    || hasDeviceCredential()) {
                    return BIOMETRIC_WEAK or DEVICE_CREDENTIAL
                }
                return null
            } else {
                if (hasBiometricCapability(BIOMETRIC_STRONG)) {
                    return BIOMETRIC_STRONG
                }
                if (hasBiometricCapability(BIOMETRIC_WEAK)) {
                    return BIOMETRIC_WEAK
                }
            }
        }
        return null
    }

    private fun initBiometricAuthentication(cryptoObject: CryptoObject?, authenticators: Int) {
        val biometricPrompt = initBiometricPrompt(authenticators)
        promptInfo?.let { promptInfo ->
            activity.runOnUiThread {
                cryptoObject?.let {
                    biometricPrompt.authenticate(promptInfo, it)
                } ?: biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    private fun setServicesFromActivity(activity: FragmentActivity) {
        val context = activity.baseContext
        biometricManager = BiometricManager.from(activity)
        keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        fingerprintManager =
            context.getSystemService(Context.FINGERPRINT_SERVICE) as FingerprintManager
    }

    private fun initBiometricPrompt(authenticators: Int): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = biometricAuthListener?.let {
            BiometricPrompt(activity, executor, it)
        } ?: kotlin.run {
            throw IllegalStateException("AuthenticationCallback is not set.")
        }

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title ?: "Biometric Authentication for login")
            .setSubtitle(subtitle ?: "Log in using your biometric credential")
        if (!allowDeviceCredentials) {
            builder.setNegativeButtonText("Cancel")
        }
        description?.let {
            builder.setDescription(it)
        }
        builder.setAllowedAuthenticators(authenticators)
        promptInfo = builder.build()
        return biometricPrompt
    }
    companion object {
        private val TAG = BiometricAuth::class.java.simpleName

        @JvmStatic
        fun isBiometricAvailable(applicationContext: Context): Boolean {
            var canAuthenticate = true
            if (Build.VERSION.SDK_INT < 28) {
                val keyguardManager: KeyguardManager =
                    applicationContext.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
                val packageManager: PackageManager = applicationContext.packageManager
                // Check if Fingerprint Sensor is supported
                if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT).not()) {
                    canAuthenticate = false
                }
                // Check if lock screen security is enabled in Settings
                if (keyguardManager.isKeyguardSecure.not()) {
                    canAuthenticate = false
                }
                // Check if Fingerprint Authentication Permission was granted
                if (ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.USE_FINGERPRINT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    canAuthenticate = false
                }
            } else {
                // Check if biometric is supported
                val biometricManager: BiometricManager = BiometricManager.from(applicationContext)
                if (biometricManager.canAuthenticate(BIOMETRIC_WEAK) != BiometricManager.BIOMETRIC_SUCCESS) {
                    canAuthenticate = false
                }
                // Check if Fingerprint Authentication Permission was granted
                if (ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.USE_BIOMETRIC
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    canAuthenticate = false
                }
            }
            return canAuthenticate
        }
    }

    init {
        setServicesFromActivity(activity)
    }
}