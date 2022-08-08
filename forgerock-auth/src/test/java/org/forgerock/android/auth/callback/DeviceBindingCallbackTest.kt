/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */
package org.forgerock.android.auth.callback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.forgerock.android.auth.FRListener
import org.forgerock.android.auth.devicebind.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class DeviceBindingCallbackTest {

    val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun testValuesAreSetProperly() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ALLOW_FALLBACK\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        assertEquals(rawContent, testObject.getContent())
    }

    @Test
    fun testSetDeviceNameAndJWSAndClientError() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ALLOW_FALLBACK\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)
        testObject.setDeviceName("jey")
        testObject.setJws("andy")
        testObject.setClientError("Abort")
        val output = testObject.getContent()
        val expectedOut = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"authenticationType\",\"value\":\"BIOMETRIC_ALLOW_FALLBACK\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"andy\"},{\"name\":\"IDToken1deviceName\",\"value\":\"jey\"},{\"name\":\"IDToken1clientError\",\"value\":\"Abort\"}]}"
        assertEquals(expectedOut, output)
    }

    @Test
    fun testSuccessPathForNoneType() {
        val rawContent = "{\"type\":\"DeviceBindingCallback\",\"output\":[{\"name\":\"userId\",\"value\":\"id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org\"},{\"name\":\"authenticationType\",\"value\":\"NONE\"},{\"name\":\"challenge\",\"value\":\"uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw=\"},{\"name\":\"title\",\"value\":\"jey\"},{\"name\":\"subtitle\",\"value\":\"Cryptography device binding\"},{\"name\":\"description\",\"value\":\"Please complete with biometric to proceed\"},{\"name\":\"timeout\",\"value\":20}],\"input\":[{\"name\":\"IDToken1jws\",\"value\":\"\"},{\"name\":\"IDToken1deviceName\",\"value\":\"\"},{\"name\":\"IDToken1clientError\",\"value\":\"\"}]}"
        val mockBiometric = mock<BiometricInterface>()
        val encryptedPref = mock<PreferenceInterface>()
        val keyAware = mock<KeyAwareInterface>()
        val publicKey = mock<RSAPublicKey>()
        val privateKey = mock<PrivateKey>()
        val keyPair = KeyPair(publicKey, privateKey, "keyAlias")
        val kid = "kid"
        val challenge = "uYksDJx878kl7B4u+wItpGXPozr8bzDTaJwHPJ06SIw="
        val authenticationLatch = CountDownLatch(1)
        whenever(keyAware.generateKeys(context, challenge, DeviceBindingAuthenticationType.NONE)).thenReturn(keyPair)
        whenever(keyAware.sign(keyPair, kid, challenge)).thenReturn("signedJWT")
        whenever(encryptedPref.persist("id=mockjey,ou=user,dc=openam,dc=forgerock,dc=org", "keyAlias", DeviceBindingAuthenticationType.NONE)).thenReturn(kid)

        val obj = JSONObject(rawContent)
        val testObject = DeviceBindingCallback(obj, 0)

        var success = false
        val listener = object: FRListener<Void> {
            override fun onSuccess(result: Void?) {
                success = true
                authenticationLatch.countDown()
            }

            override fun onException(e: Exception?) {
                success = false
                fail()
            }
        }
        testObject.execute(context, listener, keyAware = keyAware, mockBiometric, encryptedPreference = encryptedPref)
        authenticationLatch.await()
        assertTrue(success)
    }
}