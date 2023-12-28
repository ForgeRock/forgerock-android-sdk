/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.content.Context
import android.os.Build
import org.forgerock.android.auth.CryptoKey
import org.forgerock.android.auth.callback.Attestation
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.security.interfaces.RSAPublicKey

/**
 * Settings for all the none authentication is configured
 */
open class None : CryptoAware, DeviceAuthenticator {

    private lateinit var cryptoKey: CryptoKey

    /**
     * generate the public and private keypair
     */
    override suspend fun generateKeys(context: Context, attestation: Attestation): KeyPair {
        val builder = cryptoKey.keyBuilder()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setAttestationChallenge(attestation.challenge)
        }
        val key = cryptoKey.createKeyPair(builder.build())
        return KeyPair(key.public as RSAPublicKey, key.private, cryptoKey.keyAlias)
    }

    override fun deleteKeys(context: Context) {
        cryptoKey.deleteKeys()
    }

    final override fun type(): DeviceBindingAuthenticationType =
        DeviceBindingAuthenticationType.NONE

    /**
     * return success block for None type
     */
    override suspend fun authenticate(context: Context): DeviceBindingStatus {
        cryptoKey.getPrivateKey()?.let {
            return Success(it)
        } ?: return DeviceBindingErrorStatus.ClientNotRegistered()
    }

    final override fun setKey(cryptoKey: CryptoKey) {
        this.cryptoKey = cryptoKey
    }

}