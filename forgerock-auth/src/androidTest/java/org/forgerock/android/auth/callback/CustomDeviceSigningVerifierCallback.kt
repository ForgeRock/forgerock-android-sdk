/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.util.Calendar
import java.util.Date

class CustomDeviceSigningVerifierCallback : DeviceSigningVerifierCallback {
    constructor() : super()
    constructor(json: JSONObject, index: Int) : super(json, index)

    private var expSeconds = 0
    fun setExpSeconds(expSeconds: Int) {
        this.expSeconds = expSeconds
    }

    override fun getExpiration(timeout: Int?): Date {
        val date = Calendar.getInstance()
        date.add(Calendar.SECOND, expSeconds)
        return date.time
    }

    fun getSignedJwt(kid: String?, sub: String?, challenge: String?): String {
        //Generate RSA key
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val rsaKey = kpg.generateKeyPair()
        val header =
            JWSHeader.Builder(JWSAlgorithm.RS512).type(JOSEObjectType.JWT).keyID(kid).build()
        val payload = JWTClaimsSet.Builder().subject(sub).claim("challenge", challenge)
            .issuer(ApplicationProvider.getApplicationContext<Context>().packageName)
            .issueTime(Calendar.getInstance().time)
            .notBeforeTime(Calendar.getInstance().time)
            .expirationTime(getExpiration(null)).build()
        val signedJWT = SignedJWT(header, payload)
        signedJWT.sign(RSASSASigner(rsaKey.private as RSAPrivateKey))
        return signedJWT.serialize()
    }
}