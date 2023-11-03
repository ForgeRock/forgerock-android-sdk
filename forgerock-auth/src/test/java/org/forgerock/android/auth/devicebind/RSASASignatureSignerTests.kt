/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.crypto.impl.RSASSAProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey


class RSASASignatureSignerTests {

    private lateinit var keys: KeyPair
    private lateinit var jwsObject: JWSObject
    private lateinit var signature: Signature

    @Before
    fun setUp() {
        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        keys = kpg.generateKeyPair()
        val header: JWSHeader = JWSHeader.Builder(JWSAlgorithm.RS256).build()
        assertThat(header.algorithm).isEqualTo(JWSAlgorithm.RS256)
        val payload = Payload("test")
        jwsObject = JWSObject(header, payload)
        signature = object : RSASSAProvider() {
        }.signature(JWSAlgorithm.RS256, keys.private)
    }

    @Test
    fun `test sign and verify with signature`() {
        assertThat(jwsObject.state).isEqualTo(JWSObject.State.UNSIGNED)
        val signer = RSASASignatureSigner(signature)
        jwsObject.sign(signer)
        val verifier = RSASSAVerifier(keys.public as RSAPublicKey)
        assertThat(jwsObject.verify(verifier)).isTrue()
        assertThat(jwsObject.state).isEqualTo(JWSObject.State.VERIFIED)
    }

    @Test
    fun `test sign and verify with invalid public key`() {
        assertThat(jwsObject.state).isEqualTo(JWSObject.State.UNSIGNED)
        val signer = RSASASignatureSigner(signature)
        jwsObject.sign(signer)

        val kpg: KeyPairGenerator = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val anotherKey = kpg.generateKeyPair()

        val verifier = RSASSAVerifier(anotherKey.public as RSAPublicKey)
        assertThat(jwsObject.verify(verifier)).isFalse()
        assertThat(jwsObject.state).isEqualTo(JWSObject.State.SIGNED)
    }
}