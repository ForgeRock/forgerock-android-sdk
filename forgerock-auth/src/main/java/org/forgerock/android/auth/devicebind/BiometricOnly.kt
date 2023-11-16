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
import androidx.annotation.VisibleForTesting
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.impl.RSASSAProvider
import org.forgerock.android.auth.Logger
import org.forgerock.android.auth.callback.Attestation
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.security.PrivateKey
import java.security.Signature
import java.security.interfaces.RSAPublicKey

private val TAG = BiometricOnly::class.java.simpleName

/**
 * Settings  for all the biometric authentication is configured
 */
open class BiometricOnly : BiometricAuthenticator() {

    @VisibleForTesting
    fun getSignature(privateKey: PrivateKey): Signature {
        return object : RSASSAProvider() {
        }.signature(JWSAlgorithm.parse(getAlgorithm()), privateKey)
    }

    override fun authenticate(authenticationCallback: BiometricPrompt.AuthenticationCallback,
                              privateKey: PrivateKey) {
        try {
            biometricInterface.authenticate(authenticationCallback,
                BiometricPrompt.CryptoObject(getSignature(privateKey)))
        } catch (e: Exception) {
            //Failed because the key was generated with
            //KeyGenParameterSpec.Builder.setUserAuthenticationParameters
            Logger.warn(TAG, "Fallback to time-based key", e)
            biometricInterface.authenticate(authenticationCallback)
        }
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

        if (biometricInterface.isSupportedBiometricStrong().not()) {
            if (isApi30OrAbove) {
                builder.setUserAuthenticationParameters(cryptoKey.timeout,
                    KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                builder.setUserAuthenticationValidityDurationSeconds(cryptoKey.timeout)
            }
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
                biometricInterface.isSupported(BiometricManager.Authenticators.BIOMETRIC_STRONG,
                    BiometricManager.Authenticators.BIOMETRIC_WEAK)
    }

    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.BIOMETRIC_ONLY

}