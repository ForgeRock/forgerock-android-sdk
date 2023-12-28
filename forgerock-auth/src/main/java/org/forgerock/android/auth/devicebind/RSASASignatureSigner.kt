/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.impl.RSASSA
import com.nimbusds.jose.crypto.impl.RSASSAProvider
import com.nimbusds.jose.util.Base64URL
import java.security.InvalidKeyException
import java.security.PrivateKey
import java.security.Signature
import java.security.SignatureException

/**
 * A [JWSSigner] which takes a signature
 */
internal class RSASASignatureSigner(val signature: Signature) : RSASSAProvider(), JWSSigner {

    @Throws(JOSEException::class)
    override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
        return sign(signingInput, signature)
    }

    private fun sign(signingInput: ByteArray, signer: Signature): Base64URL {
        return try {
            signer.update(signingInput)
            Base64URL.encode(signer.sign())
        } catch (e: SignatureException) {
            throw JOSEException("RSA signature exception: " + e.message, e)
        }
    }
}

/**
 * Create a [Signature] with the provided [JWSAlgorithm] and [PrivateKey]
 */
internal fun RSASSAProvider.signature(jwsAlgorithm: JWSAlgorithm, privateKey: PrivateKey): Signature {
    val signer = RSASSA.getSignerAndVerifier(jwsAlgorithm, jcaContext.provider)
    try {
        signer.initSign(privateKey)
    } catch (e: InvalidKeyException) {
        throw JOSEException("Invalid private RSA key: " + e.message, e)
    }
    return signer
}