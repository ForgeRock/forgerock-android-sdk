/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import org.forgerock.android.auth.callback.Attestation
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * Settings for all the biometric authentication and device credential is configured
 */
open class BiometricAndDeviceCredential : BiometricAuthenticator() {
    override fun authenticate(authenticationCallback: BiometricPrompt.AuthenticationCallback,
                              privateKey: PrivateKey) {
        biometricInterface.authenticate(authenticationCallback)
    }

    /**
     * generate the public and private keypair
     */
    @SuppressLint("NewApi")
    override suspend fun generateKeys(context: Context, attestation: Attestation): KeyPair {
        val builder = cryptoKey.keyBuilder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setAttestationChallenge(attestation.challenge)
        }

        //We use time-base key because we allow device credential as fallback
        //Device credential is not consider biometric strong
        if (isApi30OrAbove) {
            builder.setUserAuthenticationParameters(cryptoKey.timeout,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(cryptoKey.timeout)
        }

        builder.setUserAuthenticationRequired(true)
        val key = cryptoKey.createKeyPair(builder.build())
        return KeyPair(key.public as RSAPublicKey, key.private, cryptoKey.keyAlias)
    }

    /**
     * check biometric is supported
     */
    override fun isSupported(context: Context, attestation: Attestation): Boolean {
        return super.isSupported(context, attestation) &&
                biometricInterface.isSupported(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL,
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    }


    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK


}