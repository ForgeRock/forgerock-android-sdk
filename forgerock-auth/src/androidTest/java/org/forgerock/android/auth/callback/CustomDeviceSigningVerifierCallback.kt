/*
 * Copyright (c) 2023 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.callback

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.forgerock.android.auth.Logger.Companion.debug
import org.json.JSONObject
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPrivateKey
import java.util.*

class CustomDeviceSigningVerifierCallback : DeviceSigningVerifierCallback {
  constructor() : super() {}
  constructor(json: JSONObject?, index: Int) : super(json!!, index) {}

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
    var kpg: KeyPairGenerator? = null
    try {
      kpg = KeyPairGenerator.getInstance("RSA")
    } catch (e: NoSuchAlgorithmException) {
      debug(TAG, e.message)
    }
    kpg!!.initialize(2048)
    val rsaKey = kpg.generateKeyPair()
    val header = JWSHeader.Builder(JWSAlgorithm.RS512).type(JOSEObjectType.JWT).keyID(kid).build()
    val payload = JWTClaimsSet.Builder().subject(sub).claim("challenge", challenge)
      .expirationTime(getExpiration(null)).build()
    val signedJWT = SignedJWT(header, payload)
    try {
      signedJWT.sign(RSASSASigner(rsaKey.private as RSAPrivateKey))
    } catch (e: JOSEException) {
      debug(TAG, e.message)
    }
    return signedJWT.serialize()
  }

  companion object {
    private val TAG = CustomDeviceSigningVerifierCallback::class.java.simpleName
  }
}